package com.HammersTech.RoutineChart.features.familyinvite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.data.remote.firebase.CompositeFamilyRepository
import com.HammersTech.RoutineChart.core.domain.models.Role
import com.HammersTech.RoutineChart.core.domain.models.User
import com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyInviteRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.HammersTech.RoutineChart.core.utils.InviteCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for joining a family with a manual invite code
 * Phase 2.2: QR Family Joining (Manual Code Entry)
 */
@HiltViewModel
class JoinWithCodeViewModel
    @Inject
    constructor(
        private val inviteRepository: FamilyInviteRepository,
        private val familyRepository: FamilyRepository,
        private val userRepository: UserRepository,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        data class UiState(
            val inviteCode: String = "",
            val errorMessage: String? = null,
            val isJoining: Boolean = false,
            val joinSuccess: Boolean = false,
        )

        private val _state = MutableStateFlow(UiState())
        val state: StateFlow<UiState> = _state.asStateFlow()

        val isCodeValid: Boolean
            get() = InviteCodeGenerator.normalizeInviteCode(_state.value.inviteCode) != null

        fun onInviteCodeChange(code: String) {
            _state.value =
                _state.value.copy(
                    inviteCode = code,
                    errorMessage = null,
                )
        }

        fun joinWithCode() {
            viewModelScope.launch {
                val normalizedCode = InviteCodeGenerator.normalizeInviteCode(_state.value.inviteCode)

                if (normalizedCode == null) {
                    _state.value =
                        _state.value.copy(
                            errorMessage = "Invalid code format. Use XXX-YYYY (e.g., ABC-1234)",
                        )
                    return@launch
                }

                _state.value = _state.value.copy(isJoining = true, errorMessage = null)

                try {
                    // CRITICAL: Authenticate user FIRST before querying Firestore
                    // Firestore security rules require authentication to read invites
                    var authUser = authRepository.currentUser
                    if (authUser == null) {
                        // User is not authenticated - sign in anonymously first
                        val signInResult = authRepository.signInAnonymously()
                        signInResult.fold(
                            onSuccess = { user ->
                                authUser = user
                                AppLogger.UI.info("Signed in anonymously for join family flow")
                            },
                            onFailure = { error ->
                                _state.value =
                                    _state.value.copy(
                                        isJoining = false,
                                        errorMessage = "Failed to sign in: ${error.message}",
                                    )
                                return@launch
                            },
                        )
                    }

                    val finalAuthUser = authUser
                    if (finalAuthUser == null) {
                        _state.value =
                            _state.value.copy(
                                isJoining = false,
                                errorMessage = "Please sign in to join a family",
                            )
                        return@launch
                    }

                    // Now that user is authenticated, query Firestore for invite
                    val invite =
                        try {
                            inviteRepository.getByInviteCode(normalizedCode)
                        } catch (e: Exception) {
                            AppLogger.UI.error("Failed to query invite from Firestore", e)
                            _state.value =
                                _state.value.copy(
                                    isJoining = false,
                                    errorMessage =
                                        when {
                                            e.message?.contains("network", ignoreCase = true) == true ->
                                                "Network error. Please check your internet connection and try again."
                                            e.message?.contains("permission", ignoreCase = true) == true ->
                                                "Permission denied. Please try again."
                                            else ->
                                                "Failed to validate invite code. Please try again."
                                        },
                                )
                            return@launch
                        }

                    if (invite == null) {
                        _state.value =
                            _state.value.copy(
                                isJoining = false,
                                errorMessage = "Invite code not found. Please check the code and try again.",
                            )
                        return@launch
                    }

                    // Validate invite
                    if (!invite.isValid) {
                        val message =
                            when {
                                invite.isExpired -> "This invite has expired"
                                invite.isMaxUsesReached -> "This invite has reached its maximum uses"
                                else -> "This invite is no longer active"
                            }
                        _state.value =
                            _state.value.copy(
                                isJoining = false,
                                errorMessage = message,
                            )
                        return@launch
                    }

                    // Get the family - always sync from Firestore first since we need fresh data
                    val family =
                        try {
                            if (familyRepository is CompositeFamilyRepository) {
                                AppLogger.UI.info("Syncing family ${invite.familyId} from Firestore...")
                                familyRepository.syncFromFirestore(invite.familyId)
                                familyRepository.getById(invite.familyId)
                            } else {
                                familyRepository.getById(invite.familyId)
                            }
                        } catch (e: Exception) {
                            AppLogger.UI.error("Failed to sync family from Firestore", e)
                            _state.value =
                                _state.value.copy(
                                    isJoining = false,
                                    errorMessage =
                                        when {
                                            e.message?.contains("network", ignoreCase = true) == true ->
                                                "Network error. Please check your internet connection and try again."
                                            else ->
                                                "Family not found. The invite may be invalid."
                                        },
                                )
                            return@launch
                        }

                    if (family == null) {
                        _state.value =
                            _state.value.copy(
                                isJoining = false,
                                errorMessage = "Family not found. The invite may be invalid.",
                            )
                        return@launch
                    }

                    // User is already authenticated (done earlier), now link to family
                    // Link user to family
                    // Check if user exists in database
                    val existingUser = userRepository.getById(finalAuthUser.id)
                    if (existingUser != null) {
                        // User exists - update familyId
                        if (existingUser.familyId != invite.familyId) {
                            // User is switching families
                            userRepository.updateFamilyId(finalAuthUser.id, invite.familyId)
                            AppLogger.UI.info("User ${finalAuthUser.id} switched to family ${invite.familyId}")
                        } else {
                            // User already in this family
                            AppLogger.UI.info("User ${finalAuthUser.id} already in family ${invite.familyId}")
                        }
                    } else {
                        // User doesn't exist - create new User record
                        // Default to child role for new users joining via invite
                        val newUser =
                            User(
                                id = finalAuthUser.id,
                                familyId = invite.familyId,
                                role = Role.CHILD,
                                displayName = finalAuthUser.email?.substringBefore("@") ?: "User",
                                email = finalAuthUser.email,
                                createdAt = Instant.now(),
                            )
                        userRepository.create(newUser)
                        AppLogger.UI.info("Created user ${finalAuthUser.id} and linked to family ${invite.familyId}")
                    }

                    // Increment invite used count
                    val updatedInvite = invite.copy(usedCount = invite.usedCount + 1)
                    AppLogger.UI.info(
                        "Incrementing usedCount for invite ${invite.id} from ${invite.usedCount} to ${updatedInvite.usedCount}",
                    )
                    try {
                        inviteRepository.update(updatedInvite)
                        AppLogger.UI.info("Successfully updated invite usedCount to ${updatedInvite.usedCount}")
                    } catch (e: Exception) {
                        AppLogger.UI.error("Failed to update invite usedCount: ${e.message}", e)
                        // Don't fail the entire join flow if usedCount update fails
                        // The user is already linked to the family, so log and continue
                    }

                    _state.value =
                        _state.value.copy(
                            isJoining = false,
                            joinSuccess = true,
                        )
                } catch (e: Exception) {
                    AppLogger.UI.error("Failed to join family with code", e)
                    _state.value =
                        _state.value.copy(
                            isJoining = false,
                            errorMessage = "Failed to join family: ${e.message}",
                        )
                }
            }
        }

        fun resetState() {
            _state.value = UiState()
        }
    }

package com.HammersTech.RoutineChart.features.familyinvite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.domain.models.FamilyInvite
import com.HammersTech.RoutineChart.core.domain.models.Role
import com.HammersTech.RoutineChart.core.domain.models.User
import com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyInviteRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
import com.HammersTech.RoutineChart.core.data.remote.firebase.CompositeFamilyRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import java.time.Instant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for scanning and joining via family invites
 * Phase 2.2: QR Family Joining
 */
@HiltViewModel
class ScanInviteViewModel @Inject constructor(
    private val inviteRepository: FamilyInviteRepository,
    private val familyRepository: FamilyRepository,
    private val userRepository: UserRepository,
    private val authRepository: com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository
) : ViewModel() {
    
    data class UiState(
        val isScanning: Boolean = false,
        val scannedInvite: ScannedInvite? = null,
        val showConfirmation: Boolean = false,
        val errorMessage: String? = null,
        val isJoining: Boolean = false,
        val joinSuccess: Boolean = false
    )
    
    data class ScannedInvite(
        val familyId: String,
        val token: String,
        val expires: Instant
    )
    
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    
    fun startScanning() {
        _state.value = _state.value.copy(
            isScanning = true,
            errorMessage = null
        )
    }
    
    fun stopScanning() {
        _state.value = _state.value.copy(isScanning = false)
    }
    
    fun handleScannedCode(code: String) {
        val parsed = FamilyInvite.fromURL(code)
        
        if (parsed == null) {
            _state.value = _state.value.copy(
                isScanning = false,
                errorMessage = "Invalid QR code. Please scan a valid family invite."
            )
            return
        }
        
        // Check if expired
        if (Instant.now().isAfter(parsed.third)) {
            _state.value = _state.value.copy(
                isScanning = false,
                errorMessage = "This invite has expired. Please ask for a new one."
            )
            return
        }
        
        _state.value = _state.value.copy(
            isScanning = false,
            scannedInvite = ScannedInvite(
                familyId = parsed.first,
                token = parsed.second,
                expires = parsed.third
            ),
            showConfirmation = true
        )
    }
    
    fun cancelJoin() {
        _state.value = _state.value.copy(
            showConfirmation = false,
            scannedInvite = null
        )
    }
    
    fun joinFamily() {
        val scannedInvite = _state.value.scannedInvite ?: return
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isJoining = true, errorMessage = null)
            
            try {
                // Validate invite exists and is valid
                val invite = inviteRepository.getByToken(scannedInvite.token)
                
                if (invite == null) {
                    _state.value = _state.value.copy(
                        isJoining = false,
                        errorMessage = "Invalid invite token"
                    )
                    return@launch
                }
                
                if (!invite.isValid) {
                    val message = when {
                        invite.isExpired -> "This invite has expired"
                        invite.isMaxUsesReached -> "This invite has reached its maximum uses"
                        else -> "This invite is no longer active"
                    }
                    _state.value = _state.value.copy(
                        isJoining = false,
                        errorMessage = message
                    )
                    return@launch
                }
                
                // Get the family - check locally first, then sync from Firestore if needed
                var family = familyRepository.getById(invite.familyId)
                if (family == null) {
                    // Family doesn't exist locally - try to sync from Firestore
                    AppLogger.UI.info("Family ${invite.familyId} not found locally, syncing from Firestore...")
                    if (familyRepository is CompositeFamilyRepository) {
                        try {
                            familyRepository.syncFromFirestore(invite.familyId)
                            family = familyRepository.getById(invite.familyId)
                            if (family == null) {
                                _state.value = _state.value.copy(
                                    isJoining = false,
                                    errorMessage = "Family not found. The invite may be invalid."
                                )
                                return@launch
                            }
                            AppLogger.UI.info("Successfully synced family ${invite.familyId} from Firestore")
                        } catch (e: Exception) {
                            AppLogger.UI.error("Failed to sync family from Firestore", e)
                            _state.value = _state.value.copy(
                                isJoining = false,
                                errorMessage = "Family not found. The invite may be invalid."
                            )
                            return@launch
                        }
                    } else {
                        _state.value = _state.value.copy(
                            isJoining = false,
                            errorMessage = "Family not found"
                        )
                        return@launch
                    }
                }
                
                // Check if user is authenticated, if not, sign in anonymously
                var authUser = authRepository.currentUser
                if (authUser == null) {
                    // User is not authenticated - sign in anonymously first
                    authRepository.signInAnonymously().fold(
                        onSuccess = { user ->
                            authUser = user
                            AppLogger.UI.info("Signed in anonymously for join family flow (QR)")
                        },
                        onFailure = { error ->
                            _state.value = _state.value.copy(
                                isJoining = false,
                                errorMessage = "Failed to sign in: ${error.message}"
                            )
                            return@launch
                        }
                    )
                }
                
                val finalAuthUser = authUser
                if (finalAuthUser == null) {
                    _state.value = _state.value.copy(
                        isJoining = false,
                        errorMessage = "Please sign in to join a family"
                    )
                    return@launch
                }
                
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
                    val newUser = User(
                        id = finalAuthUser.id,
                        familyId = invite.familyId,
                        role = Role.CHILD,
                        displayName = finalAuthUser.email?.substringBefore("@") ?: "User",
                        email = finalAuthUser.email,
                        createdAt = Instant.now()
                    )
                    userRepository.create(newUser)
                    AppLogger.UI.info("Created user ${finalAuthUser.id} and linked to family ${invite.familyId}")
                }
                
                // Increment invite used count
                val updatedInvite = invite.copy(usedCount = invite.usedCount + 1)
                inviteRepository.update(updatedInvite)
                
                _state.value = _state.value.copy(
                    isJoining = false,
                    joinSuccess = true
                )
            } catch (e: Exception) {
                AppLogger.UI.error("Failed to join family", e)
                _state.value = _state.value.copy(
                    isJoining = false,
                    errorMessage = "Failed to join family: ${e.message}"
                )
            }
        }
    }
    
    fun resetState() {
        _state.value = UiState()
    }
}

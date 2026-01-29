package com.HammersTech.RoutineChart.features.familyinvite

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.domain.models.FamilyInvite
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyInviteRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.HammersTech.RoutineChart.core.utils.InviteCodeGenerator
import com.HammersTech.RoutineChart.core.utils.QRCodeGenerator
import com.HammersTech.RoutineChart.core.utils.TokenGenerator
import com.HammersTech.RoutineChart.core.utils.ULIDGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for generating and displaying family invites
 * Phase 2.2: QR Family Joining
 */
@HiltViewModel
class GenerateInviteViewModel
    @Inject
    constructor(
        private val inviteRepository: FamilyInviteRepository,
        private val familyRepository: FamilyRepository,
        private val userRepository: com.HammersTech.RoutineChart.core.domain.repositories.UserRepository,
        private val authRepository: com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository,
    ) : ViewModel() {
        data class UiState(
            val isLoading: Boolean = false,
            val loadingMessage: String = "Loading...",
            val qrCodeBitmap: Bitmap? = null,
            val invite: FamilyInvite? = null,
            val errorMessage: String? = null,
            val timeRemaining: String = "",
        )

        private val _state = MutableStateFlow(UiState())
        val state: StateFlow<UiState> = _state.asStateFlow()

        private var timerJob: kotlinx.coroutines.Job? = null

        fun loadActiveInvite() {
            viewModelScope.launch {
                _state.value =
                    _state.value.copy(
                        isLoading = true,
                        loadingMessage = "Loading...",
                        errorMessage = null,
                    )

                try {
                    // Get current authenticated user
                    val authUser =
                        authRepository.currentUser
                            ?: throw IllegalStateException("Not authenticated")

                    // Get current user's familyId (source of truth)
                    val currentUser = userRepository.getById(authUser.id)
                    if (currentUser == null) {
                        _state.value = _state.value.copy(isLoading = false)
                        return@launch
                    }

                    val currentFamilyId = currentUser.familyId

                    // Get the current family
                    val family = familyRepository.getById(currentFamilyId)
                    if (family == null) {
                        _state.value = _state.value.copy(isLoading = false)
                        return@launch
                    }

                    // Sync invites from Firestore first (to get invites created on other devices)
                    if (inviteRepository is com.HammersTech.RoutineChart.core.data.remote.firebase.CompositeFamilyInviteRepository) {
                        try {
                            (inviteRepository as com.HammersTech.RoutineChart.core.data.remote.firebase.CompositeFamilyInviteRepository)
                                .syncFromFirestore(currentFamilyId)
                            AppLogger.UI.info("Synced invites from Firestore for family: $currentFamilyId")
                        } catch (e: Exception) {
                            // Log but don't fail - we can still use local data
                            AppLogger.UI.error("Failed to sync invites from Firestore: ${e.message}", e)
                        }
                    }

                    // Get all active invites for this family (now includes synced invites)
                    val activeInvites = inviteRepository.getActiveInvites(currentFamilyId)

                    // Find the first valid (not expired, not max uses) invite
                    val validInvite = activeInvites.firstOrNull { it.isValid }

                    if (validInvite != null) {
                        // Generate QR code
                        val qrCode = QRCodeGenerator.generate(validInvite)

                        _state.value =
                            _state.value.copy(
                                isLoading = false,
                                invite = validInvite,
                                qrCodeBitmap = qrCode,
                            )

                        // Start timer to update time remaining
                        startTimer()
                        AppLogger.UI.info("Loaded existing active invite: ${validInvite.id}")
                    }
                } catch (e: Exception) {
                    AppLogger.UI.error("Failed to load active invite", e)
                    // Don't show error to user - just proceed to generate new one if needed
                }

                _state.value = _state.value.copy(isLoading = false)
            }
        }

        fun generateInvite() {
            viewModelScope.launch {
                _state.value =
                    _state.value.copy(
                        isLoading = true,
                        loadingMessage = "Generating invite...",
                        errorMessage = null,
                    )

                try {
                    // Get current authenticated user
                    val authUser =
                        authRepository.currentUser
                            ?: throw IllegalStateException("Not authenticated")

                    // Get current user's familyId (source of truth)
                    val currentUser = userRepository.getById(authUser.id)
                    if (currentUser == null) {
                        _state.value =
                            _state.value.copy(
                                isLoading = false,
                                errorMessage = "User not found. Please sign in again.",
                            )
                        return@launch
                    }

                    val currentFamilyId = currentUser.familyId

                    // Get the current family
                    val family = familyRepository.getById(currentFamilyId)
                    if (family == null) {
                        _state.value =
                            _state.value.copy(
                                isLoading = false,
                                errorMessage = "No family found. Please create a family first.",
                            )
                        return@launch
                    }

                    // Use the authUser we already got above
                    val createdBy = authUser.id

                    // Create invite
                    val token = TokenGenerator.generateSecureToken()
                    val inviteCode = InviteCodeGenerator.generateInviteCode()
                    val expiresAt = Instant.now().plusSeconds(86400) // 24 hours

                    val newInvite =
                        FamilyInvite(
                            id = ULIDGenerator.generate(),
                            familyId = family.id,
                            token = token,
                            inviteCode = inviteCode,
                            createdBy = createdBy,
                            expiresAt = expiresAt,
                        )

                    inviteRepository.create(newInvite)

                    // Generate QR code
                    val qrCode = QRCodeGenerator.generate(newInvite)

                    _state.value =
                        _state.value.copy(
                            isLoading = false,
                            invite = newInvite,
                            qrCodeBitmap = qrCode,
                        )

                    // Start timer to update time remaining
                    startTimer()

                    AppLogger.UI.info("Generated family invite: ${newInvite.id}")
                } catch (e: Exception) {
                    AppLogger.UI.error("Failed to generate invite", e)
                    _state.value =
                        _state.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to generate invite: ${e.message}",
                        )
                }
            }
        }

        fun deactivateInvite() {
            viewModelScope.launch {
                val invite = _state.value.invite ?: return@launch

                try {
                    inviteRepository.deactivate(invite.id)
                    _state.value = UiState() // Reset state
                    timerJob?.cancel()
                    AppLogger.UI.info("Deactivated invite: ${invite.id}")
                } catch (e: Exception) {
                    AppLogger.UI.error("Failed to deactivate invite", e)
                    _state.value =
                        _state.value.copy(
                            errorMessage = "Failed to deactivate invite: ${e.message}",
                        )
                }
            }
        }

        fun shareableImage(
            invite: FamilyInvite,
            qrBitmap: Bitmap,
        ): Bitmap? {
            return try {
                // Create a composite image with QR code and invite code text
                val width = 600
                val height = 800
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)

                // White background
                canvas.drawColor(android.graphics.Color.WHITE)

                val paint =
                    android.graphics.Paint().apply {
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                var yPos = 60f

                // Title text
                paint.textSize = 28f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.color = android.graphics.Color.BLACK
                canvas.drawText("Join my family on Routine Chart!", width / 2f, yPos, paint)
                yPos += 50f

                // Invite code text
                paint.textSize = 32f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                paint.color = android.graphics.Color.parseColor("#2196F3") // Material Blue
                canvas.drawText("Invite Code: ${invite.inviteCode}", width / 2f, yPos, paint)
                yPos += 60f

                // QR Code (centered, larger)
                val qrSize = 400
                val qrX = (width - qrSize) / 2f
                val qrY = yPos
                canvas.drawBitmap(
                    Bitmap.createScaledBitmap(qrBitmap, qrSize, qrSize, true),
                    qrX,
                    qrY,
                    null,
                )
                yPos += qrSize + 40f

                // Instructions text
                paint.textSize = 18f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                paint.color = android.graphics.Color.GRAY
                canvas.drawText("Scan the QR code or use the invite code above", width / 2f, yPos, paint)

                bitmap
            } catch (e: Exception) {
                AppLogger.UI.error("Failed to create shareable image", e)
                null
            }
        }

        private fun startTimer() {
            timerJob?.cancel()
            timerJob =
                viewModelScope.launch {
                    while (true) {
                        updateTimeRemaining()
                        delay(1000) // Update every second
                    }
                }
        }

        private fun updateTimeRemaining() {
            val invite = _state.value.invite ?: return

            val remaining = invite.timeRemaining
            if (remaining <= 0) {
                _state.value = _state.value.copy(timeRemaining = "Expired")
                timerJob?.cancel()
                return
            }

            val hours = remaining / 3600
            val minutes = (remaining % 3600) / 60
            val seconds = remaining % 60

            val timeString =
                when {
                    hours > 0 -> "Expires in ${hours}h ${minutes}m"
                    minutes > 0 -> "Expires in ${minutes}m ${seconds}s"
                    else -> "Expires in ${seconds}s"
                }

            _state.value = _state.value.copy(timeRemaining = timeString)
        }

        override fun onCleared() {
            super.onCleared()
            timerJob?.cancel()
        }
    }

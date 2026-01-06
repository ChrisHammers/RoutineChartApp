package com.HammersTech.RoutineChart.features.familyinvite

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.domain.models.FamilyInvite
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyInviteRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.HammersTech.RoutineChart.core.utils.QRCodeGenerator
import com.HammersTech.RoutineChart.core.utils.TokenGenerator
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
class GenerateInviteViewModel @Inject constructor(
    private val inviteRepository: FamilyInviteRepository,
    private val familyRepository: FamilyRepository
) : ViewModel() {
    
    data class UiState(
        val isLoading: Boolean = false,
        val qrCodeBitmap: Bitmap? = null,
        val invite: FamilyInvite? = null,
        val errorMessage: String? = null,
        val timeRemaining: String = ""
    )
    
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    
    private var timerJob: kotlinx.coroutines.Job? = null
    
    fun generateInvite() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // Get the current family
                val family = familyRepository.getAll().firstOrNull()
                if (family == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "No family found. Please create a family first."
                    )
                    return@launch
                }
                
                // For now, use a placeholder user ID (in Phase 2.3, use actual auth user)
                val createdBy = "currentUserId" // TODO: Replace with actual auth user ID
                
                // Create invite
                val token = TokenGenerator.generateSecureToken()
                val expiresAt = Instant.now().plusSeconds(86400) // 24 hours
                
                val newInvite = FamilyInvite(
                    id = com.github.guepardoapps.kulid.ULID.random(),
                    familyId = family.id,
                    token = token,
                    createdBy = createdBy,
                    expiresAt = expiresAt
                )
                
                inviteRepository.create(newInvite)
                
                // Generate QR code
                val qrCode = QRCodeGenerator.generate(newInvite)
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    invite = newInvite,
                    qrCodeBitmap = qrCode
                )
                
                // Start timer to update time remaining
                startTimer()
                
                AppLogger.UI.info("Generated family invite: ${newInvite.id}")
            } catch (e: Exception) {
                AppLogger.UI.error("Failed to generate invite", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate invite: ${e.message}"
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
                _state.value = _state.value.copy(
                    errorMessage = "Failed to deactivate invite: ${e.message}"
                )
            }
        }
    }
    
    fun shareInvite() {
        val url = _state.value.invite?.qrCodeURL() ?: return
        // TODO: Implement sharing functionality
        AppLogger.UI.info("Share invite URL: $url")
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
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
        
        val timeString = when {
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


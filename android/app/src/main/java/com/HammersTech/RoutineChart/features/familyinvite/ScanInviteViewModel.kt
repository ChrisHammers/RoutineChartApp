package com.HammersTech.RoutineChart.features.familyinvite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.domain.models.FamilyInvite
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyInviteRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for scanning and joining via family invites
 * Phase 2.2: QR Family Joining
 */
@HiltViewModel
class ScanInviteViewModel @Inject constructor(
    private val inviteRepository: FamilyInviteRepository,
    private val familyRepository: FamilyRepository,
    private val userRepository: UserRepository
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
                
                // Get the family
                val family = familyRepository.getById(invite.familyId)
                if (family == null) {
                    _state.value = _state.value.copy(
                        isJoining = false,
                        errorMessage = "Family not found"
                    )
                    return@launch
                }
                
                // TODO Phase 2.3: Link current auth user to this family
                // For now, just log success
                AppLogger.UI.info("Successfully validated invite for family: ${family.name ?: family.id}")
                
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


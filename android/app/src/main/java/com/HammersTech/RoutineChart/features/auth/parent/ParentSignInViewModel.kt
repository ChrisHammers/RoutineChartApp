package com.HammersTech.RoutineChart.features.auth.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for parent sign-in
 * Phase 2.1: Firebase Auth
 */
@HiltViewModel
class ParentSignInViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ParentSignInState())
        val state: StateFlow<ParentSignInState> = _state.asStateFlow()

        fun onEmailChange(email: String) {
            _state.update { it.copy(email = email, errorMessage = null) }
        }

        fun onPasswordChange(password: String) {
            _state.update { it.copy(password = password, errorMessage = null) }
        }

        fun toggleSignUpMode() {
            _state.update { it.copy(isSignUpMode = !it.isSignUpMode, errorMessage = null) }
        }

        fun signIn() {
            val currentState = _state.value
            if (!currentState.canSubmit) return

            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, errorMessage = null) }

                val result =
                    if (currentState.isSignUpMode) {
                        authRepository.signUpWithEmail(currentState.email, currentState.password)
                    } else {
                        authRepository.signInWithEmail(currentState.email, currentState.password)
                    }

                result.fold(
                    onSuccess = {
                        AppLogger.UI.info("Parent sign in successful")
                        _state.update { it.copy(isLoading = false) }
                    },
                    onFailure = { error ->
                        AppLogger.UI.error("Parent sign in failed", error)
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Sign in failed",
                            )
                        }
                    },
                )
            }
        }

        fun sendPasswordReset() {
            val currentState = _state.value
            if (currentState.email.isBlank()) {
                _state.update { it.copy(errorMessage = "Please enter your email address") }
                return
            }

            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, errorMessage = null) }

                val result = authRepository.sendPasswordReset(currentState.email)

                result.fold(
                    onSuccess = {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Password reset email sent! Check your inbox.",
                            )
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to send reset email",
                            )
                        }
                    },
                )
            }
        }
    }

data class ParentSignInState(
    val email: String = "",
    val password: String = "",
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.length >= 6 && !isLoading
}

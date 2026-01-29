package com.HammersTech.RoutineChart.features.auth.child

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
 * ViewModel for child sign-in
 * Phase 2.1: Firebase Auth
 */
@HiltViewModel
class ChildSignInViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ChildSignInState())
        val state: StateFlow<ChildSignInState> = _state.asStateFlow()

        fun signInAsChild() {
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, errorMessage = null) }

                val result = authRepository.signInAnonymously()

                result.fold(
                    onSuccess = {
                        AppLogger.UI.info("Child signed in anonymously")
                        _state.update { it.copy(isLoading = false) }
                    },
                    onFailure = { error ->
                        AppLogger.UI.error("Child sign in failed", error)
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
    }

data class ChildSignInState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

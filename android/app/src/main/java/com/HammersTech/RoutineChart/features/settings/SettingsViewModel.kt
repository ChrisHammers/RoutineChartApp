package com.HammersTech.RoutineChart.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.domain.models.AgeBand
import com.HammersTech.RoutineChart.core.domain.models.ChildProfile
import com.HammersTech.RoutineChart.core.domain.models.Role
import com.HammersTech.RoutineChart.core.domain.models.User
import com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository
import com.HammersTech.RoutineChart.core.domain.repositories.ChildProfileRepository
import com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.HammersTech.RoutineChart.core.utils.ULIDGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 * Phase 2.3: User Linking
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val childProfileRepository: ChildProfileRepository,
    ) : ViewModel() {
        data class UiState(
            val currentUser: User? = null,
            val isCreatingTestData: Boolean = false,
            val testDataMessage: String? = null,
        )

        private val _state = MutableStateFlow(UiState())
        val state: StateFlow<UiState> = _state.asStateFlow()

        init {
            loadCurrentUser()
        }

        private fun loadCurrentUser() {
            viewModelScope.launch {
                val authUser = authRepository.currentUser
                if (authUser != null) {
                    val user = userRepository.getById(authUser.id)
                    _state.update { it.copy(currentUser = user) }
                }
            }
        }

        fun createTestChildren() {
            viewModelScope.launch {
                val user = _state.value.currentUser
                if (user == null || user.role != Role.PARENT) {
                    return@launch
                }

                _state.update { it.copy(isCreatingTestData = true, testDataMessage = null) }

                try {
                    val child1 =
                        ChildProfile(
                            id = ULIDGenerator.generate(),
                            familyId = user.familyId,
                            displayName = "Emma",
                            avatarIcon = "🌟",
                            ageBand = AgeBand.AGE_5_7,
                            readingMode = com.HammersTech.RoutineChart.core.domain.models.ReadingMode.LIGHT_TEXT,
                            audioEnabled = true,
                            createdAt = Instant.now(),
                        )
                    childProfileRepository.create(child1)

                    val child2 =
                        ChildProfile(
                            id = ULIDGenerator.generate(),
                            familyId = user.familyId,
                            displayName = "Noah",
                            avatarIcon = "🚀",
                            ageBand = AgeBand.AGE_8_10,
                            readingMode = com.HammersTech.RoutineChart.core.domain.models.ReadingMode.FULL_TEXT,
                            audioEnabled = true,
                            createdAt = Instant.now(),
                        )
                    childProfileRepository.create(child2)

                    _state.update {
                        it.copy(
                            isCreatingTestData = false,
                            testDataMessage = "Created Emma and Noah",
                        )
                    }
                    AppLogger.Database.info("Created test children for family: ${user.familyId}")
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            isCreatingTestData = false,
                            testDataMessage = "Error: ${e.message}",
                        )
                    }
                    AppLogger.UI.error("Failed to create test children", e)
                }
            }
        }
    }

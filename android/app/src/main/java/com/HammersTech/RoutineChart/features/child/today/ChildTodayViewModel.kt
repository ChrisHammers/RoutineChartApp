package com.HammersTech.RoutineChart.features.child.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.data.local.SeedDataManager
import com.HammersTech.RoutineChart.core.domain.models.ChildProfile
import com.HammersTech.RoutineChart.core.domain.models.Routine
import com.HammersTech.RoutineChart.core.domain.models.RoutineStep
import com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository
import com.HammersTech.RoutineChart.core.domain.repositories.ChildProfileRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineAssignmentRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineStepRepository
import com.HammersTech.RoutineChart.core.domain.usecases.CompleteStepUseCase
import com.HammersTech.RoutineChart.core.domain.usecases.DeriveRoutineCompletionUseCase
import com.HammersTech.RoutineChart.core.domain.usecases.DeriveStepCompletionUseCase
import com.HammersTech.RoutineChart.core.domain.usecases.UndoStepUseCase
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.HammersTech.RoutineChart.core.utils.DateHelpers
import com.HammersTech.RoutineChart.core.utils.DeviceIdentifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for child's today routine view
 */
@HiltViewModel
class ChildTodayViewModel @Inject constructor(
    private val seedDataManager: SeedDataManager,
    private val familyRepository: FamilyRepository,
    private val childProfileRepository: ChildProfileRepository,
    private val routineRepository: RoutineRepository,
    private val routineStepRepository: RoutineStepRepository,
    private val routineAssignmentRepository: RoutineAssignmentRepository,
    private val completeStepUseCase: CompleteStepUseCase,
    private val undoStepUseCase: UndoStepUseCase,
    private val deriveStepCompletionUseCase: DeriveStepCompletionUseCase,
    private val deriveRoutineCompletionUseCase: DeriveRoutineCompletionUseCase,
    private val deviceIdentifier: DeviceIdentifier,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChildTodayState())
    val state: StateFlow<ChildTodayState> = _state.asStateFlow()

    private var deviceId: String = ""

    init {
        AppLogger.UI.info("ChildTodayViewModel init started")
        initializeData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            try {
                AppLogger.UI.info("Initializing data...")
                _state.update { it.copy(isLoading = true) }

                // Seed data if needed
                // Get current authenticated user ID for seeding
                val authUser = authRepository.currentUser
                if (authUser != null) {
                    AppLogger.UI.info("Seeding data if needed...")
                    seedDataManager.seedDataIfNeeded(authUser.id)
                } else {
                    AppLogger.UI.error("No authenticated user, skipping seed data")
                }

                // Get device ID
                AppLogger.UI.info("Getting device ID...")
                deviceId = deviceIdentifier.getDeviceId()
                AppLogger.UI.info("Device ID: $deviceId")

                // Load family
                AppLogger.UI.info("Loading family...")
                val family = familyRepository.getFirst()
                if (family == null) {
                    AppLogger.UI.error("No family found")
                    _state.update { it.copy(isLoading = false, error = "No family found") }
                    return@launch
                }
                AppLogger.UI.info("Family loaded: ${family.name}")

                // Load children
                AppLogger.UI.info("Loading children...")
                val children = childProfileRepository.getByFamilyId(family.id)
                if (children.isEmpty()) {
                    AppLogger.UI.error("No children found")
                    _state.update { it.copy(isLoading = false, error = "No children found") }
                    return@launch
                }
                AppLogger.UI.info("Children loaded: ${children.size}")

                _state.update {
                    it.copy(
                        family = family,
                        children = children,
                        selectedChild = children.first(),
                        isLoading = false
                    )
                }

                // Load routines for selected child
                AppLogger.UI.info("Loading routines for child...")
                loadRoutinesForChild(children.first().id, family.id, family.timeZone)
                AppLogger.UI.info("Initialization complete")
            } catch (e: Exception) {
                AppLogger.UI.error("Error initializing data: ${e.message}", e)
                android.util.Log.e("ChildTodayViewModel", "Error initializing data", e)
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectChild(child: ChildProfile) {
        val family = _state.value.family ?: return
        _state.update { it.copy(selectedChild = child) }
        loadRoutinesForChild(child.id, family.id, family.timeZone)
    }

    private fun loadRoutinesForChild(childId: String, familyId: String, timeZone: String) {
        viewModelScope.launch {
            try {
                val dayKey = DateHelpers.todayDayKey(timeZone)

                // Get assigned routines
                val assignments = routineAssignmentRepository.getActiveByChildId(childId)
                AppLogger.UI.info("Loading routines for childId=$childId: found ${assignments.size} assignments")
                val routineIds = assignments.map { it.routineId }
                AppLogger.UI.info("Routine IDs: ${routineIds.joinToString()}")

                val routinesWithSteps = routineIds.mapNotNull { routineId ->
                    val routine = routineRepository.getById(routineId) ?: return@mapNotNull null
                    AppLogger.UI.info("Loaded routine: ${routine.title}")
                    val steps = routineStepRepository.getByRoutineId(routineId)
                    AppLogger.UI.info("  -> ${steps.size} steps")

                    // Get completion states for all steps
                    val stepStates = deriveStepCompletionUseCase.getStepStates(
                        childId = childId,
                        routineId = routineId,
                        stepIds = steps.map { it.id },
                        dayKey = dayKey
                    )

                    // Check routine completion
                    val isComplete = deriveRoutineCompletionUseCase(
                        routine = routine,
                        childId = childId,
                        stepIds = steps.map { it.id },
                        dayKey = dayKey
                    )

                    RoutineWithSteps(
                        routine = routine,
                        steps = steps,
                        stepCompletionStates = stepStates,
                        isRoutineComplete = isComplete
                    )
                }

                _state.update { it.copy(routines = routinesWithSteps) }
            } catch (e: Exception) {
                AppLogger.UI.error("Error loading routines", e)
            }
        }
    }

    fun toggleStep(routineId: String, stepId: String) {
        val family = _state.value.family ?: return
        val child = _state.value.selectedChild ?: return

        viewModelScope.launch {
            try {
                val dayKey = DateHelpers.todayDayKey(family.timeZone)

                // Check current state
                val isCurrentlyComplete = deriveStepCompletionUseCase(
                    childId = child.id,
                    routineId = routineId,
                    stepId = stepId,
                    dayKey = dayKey
                )

                // Toggle: if complete, undo; if incomplete, complete
                if (isCurrentlyComplete) {
                    undoStepUseCase(
                        familyId = family.id,
                        familyTimeZone = family.timeZone,
                        childId = child.id,
                        routineId = routineId,
                        stepId = stepId,
                        deviceId = deviceId
                    )
                } else {
                    completeStepUseCase(
                        familyId = family.id,
                        familyTimeZone = family.timeZone,
                        childId = child.id,
                        routineId = routineId,
                        stepId = stepId,
                        deviceId = deviceId
                    )
                }

                // Reload routines to update UI
                loadRoutinesForChild(child.id, family.id, family.timeZone)
            } catch (e: Exception) {
                AppLogger.UI.error("Error toggling step", e)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                AppLogger.UI.info("User signed out")
            } catch (e: Exception) {
                AppLogger.UI.error("Error signing out: ${e.message}", e)
                _state.update { it.copy(error = "Failed to sign out") }
            }
        }
    }
}

data class ChildTodayState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val family: com.HammersTech.RoutineChart.core.domain.models.Family? = null,
    val children: List<ChildProfile> = emptyList(),
    val selectedChild: ChildProfile? = null,
    val routines: List<RoutineWithSteps> = emptyList()
)

data class RoutineWithSteps(
    val routine: Routine,
    val steps: List<RoutineStep>,
    val stepCompletionStates: Map<String, Boolean>,
    val isRoutineComplete: Boolean
)


package com.HammersTech.RoutineChart.features.parent.routinebuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.domain.models.ChildProfile
import com.HammersTech.RoutineChart.core.domain.models.Routine
import com.HammersTech.RoutineChart.core.domain.models.RoutineAssignment
import com.HammersTech.RoutineChart.core.domain.models.RoutineStep
import com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository
import com.HammersTech.RoutineChart.core.domain.repositories.ChildProfileRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineAssignmentRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineStepRepository
import com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
import com.HammersTech.RoutineChart.core.domain.usecases.CreateRoutineUseCase
import com.HammersTech.RoutineChart.core.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RoutineBuilderViewModel @Inject constructor(
    private val createRoutineUseCase: CreateRoutineUseCase,
    private val routineRepository: RoutineRepository,
    private val routineStepRepository: RoutineStepRepository,
    private val routineAssignmentRepository: RoutineAssignmentRepository,
    private val childProfileRepository: ChildProfileRepository,
    private val familyRepository: FamilyRepository,
    private val authRepository: com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository,
    private val userRepository: com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RoutineBuilderState())
    val state: StateFlow<RoutineBuilderState> = _state.asStateFlow()

    fun initialize(routine: Routine?) {
        viewModelScope.launch {
            try {
                // Get current authenticated user to find their familyId
                val authUser = authRepository.currentUser
                if (authUser == null) {
                    _state.update { it.copy(error = "Not signed in") }
                    return@launch
                }
                
                // Get user record to find their familyId
                val user = userRepository.getById(authUser.id)
                if (user == null) {
                    _state.update { it.copy(error = "User not found. Please join a family first.") }
                    return@launch
                }
                
                val familyId = user.familyId

                // Load children
                val children = childProfileRepository.getByFamilyId(familyId)

                // If editing, load existing data
                if (routine != null) {
                    val steps = routineStepRepository.getByRoutineId(routine.id)
                        .sortedBy { it.orderIndex }
                        .map { StepInput(it.label ?: "", it.iconName ?: "⚪️") }

                    val assignments = routineAssignmentRepository.getByRoutineId(routine.id)
                    val selectedChildIds = assignments.filter { it.isActive }.map { it.childId }.toSet()

                    val updatedState = RoutineBuilderState(
                        familyId = familyId,
                        existingRoutine = routine,
                        title = routine.title,
                        iconName = routine.iconName ?: "📋",
                        steps = steps,
                        children = children,
                        selectedChildIds = selectedChildIds,
                        canSave = false // Will be computed
                    )
                    _state.update { updatedState.copy(canSave = computeCanSave(updatedState)) }
                } else {
                    // Creating new routine - reset all fields
                    val newState = RoutineBuilderState(
                        familyId = familyId,
                        existingRoutine = null,
                        title = "",
                        iconName = "📋",
                        steps = emptyList(),
                        children = children,
                        selectedChildIds = emptySet(),
                        canSave = false
                    )
                    _state.update { newState }
                }

                AppLogger.UI.info("Initialized routine builder with ${children.size} children")
            } catch (e: Exception) {
                AppLogger.UI.error("Error initializing routine builder: ${e.message}", e)
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateTitle(title: String) {
        _state.update { state ->
            val updated = state.copy(title = title)
            updated.copy(canSave = computeCanSave(updated))
        }
    }

    fun updateIconName(iconName: String) {
        _state.update { it.copy(iconName = iconName) }
    }

    fun updateStep(index: Int, label: String, iconName: String) {
        val updatedSteps = _state.value.steps.toMutableList()
        updatedSteps[index] = StepInput(label, iconName)
        _state.update { state ->
            val updated = state.copy(steps = updatedSteps)
            updated.copy(canSave = computeCanSave(updated))
        }
    }

    fun addStep() {
        val updatedSteps = _state.value.steps + StepInput("", "⚪️")
        _state.update { state ->
            val updated = state.copy(steps = updatedSteps)
            updated.copy(canSave = computeCanSave(updated))
        }
    }

    fun removeStep(index: Int) {
        val updatedSteps = _state.value.steps.toMutableList()
        updatedSteps.removeAt(index)
        _state.update { state ->
            val updated = state.copy(steps = updatedSteps)
            updated.copy(canSave = computeCanSave(updated))
        }
    }

    fun moveStep(from: Int, to: Int) {
        val updatedSteps = _state.value.steps.toMutableList()
        val item = updatedSteps.removeAt(from)
        updatedSteps.add(to, item)
        _state.update { it.copy(steps = updatedSteps) }
    }

    fun toggleChildSelection(childId: String) {
        val updatedSelection = _state.value.selectedChildIds.toMutableSet()
        if (updatedSelection.contains(childId)) {
            updatedSelection.remove(childId)
        } else {
            updatedSelection.add(childId)
        }
        _state.update { it.copy(selectedChildIds = updatedSelection) }
    }

    fun canSave(): Boolean {
        return _state.value.canSave
    }
    
    private fun computeCanSave(state: RoutineBuilderState): Boolean {
        val hasTitle = state.title.isNotBlank()
        val hasSteps = state.steps.isNotEmpty()
        // Require ALL steps to have non-blank text
        val allStepsHaveText = state.steps.all { it.label.isNotBlank() }
        
        val canSaveResult = hasTitle && hasSteps && allStepsHaveText
        
        // Log validation for debugging (only when it changes or fails)
        if (!canSaveResult) {
            AppLogger.UI.info("computeCanSave() failed: hasTitle=$hasTitle, hasSteps=$hasSteps, allStepsHaveText=$allStepsHaveText, totalSteps=${state.steps.size}")
            state.steps.forEachIndexed { index, step ->
                AppLogger.UI.info("  Step $index: label='${step.label}', iconName='${step.iconName}'")
            }
        }
        
        return canSaveResult
    }

    fun save(onSuccess: () -> Unit) {
        val state = _state.value
        val familyId = state.familyId ?: return

        if (!canSave()) {
            _state.update { it.copy(error = "Please fill in all fields") }
            return
        }

        viewModelScope.launch {
            try {
                _state.update { it.copy(isSaving = true, error = null) }

                val routine: Routine

                if (state.existingRoutine != null) {
                    // Update existing routine
                    val updated = state.existingRoutine.copy(
                        title = state.title,
                        iconName = state.iconName,
                        updatedAt = Instant.now()
                    )

                    routineRepository.update(updated)

                    // Delete old steps (soft delete)
                    val oldSteps = routineStepRepository.getByRoutineId(state.existingRoutine.id)
                    oldSteps.forEach { step ->
                        val deleted = step.copy(deletedAt = Instant.now())
                        routineStepRepository.update(deleted)
                    }

                    // Create new steps (no familyId needed)
                    state.steps.forEachIndexed { index, stepInput ->
                        val step = RoutineStep(
                            id = UUID.randomUUID().toString(),
                            routineId = state.existingRoutine.id,
                            orderIndex = index,
                            label = stepInput.label,
                            iconName = stepInput.iconName,
                            audioCueUrl = null,
                            createdAt = Instant.now(),
                            deletedAt = null
                        )
                        routineStepRepository.create(step)
                    }

                    routine = updated
                } else {
                    // Create new routine - get current user ID
                    val authUser = authRepository.currentUser
                    if (authUser == null) {
                        _state.update { it.copy(isSaving = false, error = "Not signed in") }
                        return@launch
                    }
                    
                    // canSave() already ensures all steps have text, so we can use all steps
                    routine = createRoutineUseCase(
                        userId = authUser.id,
                        familyId = familyId, // Optional - if nil, routine is personal
                        title = state.title,
                        iconName = state.iconName,
                        steps = state.steps.map { CreateRoutineUseCase.StepInput(it.label, it.iconName) }
                    )
                    
                    // Phase 3.2: Upload unsynced routines after creation
                    if (routineRepository is com.HammersTech.RoutineChart.core.data.remote.firebase.CompositeRoutineRepository) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                val uploaded = routineRepository.uploadUnsynced(authUser.id, familyId)
                                if (uploaded > 0) {
                                    AppLogger.UI.info("✅ Uploaded $uploaded unsynced routine(s) after creation")
                                }
                            } catch (e: Exception) {
                                AppLogger.UI.error("⚠️ Failed to upload unsynced routines after creation: ${e.message}", e)
                            }
                        }
                    }
                }

                // Update assignments
                val existingAssignments = routineAssignmentRepository.getByRoutineId(routine.id)

                // Deactivate all existing
                existingAssignments.forEach { assignment ->
                    routineAssignmentRepository.update(assignment.copy(isActive = false))
                }

                // Create/activate assignments for selected children
                state.selectedChildIds.forEach { childId ->
                    val existing = existingAssignments.firstOrNull { it.childId == childId }
                    if (existing != null) {
                        routineAssignmentRepository.update(existing.copy(isActive = true))
                    } else {
                        val assignment = RoutineAssignment(
                            id = UUID.randomUUID().toString(),
                            familyId = familyId,
                            routineId = routine.id,
                            childId = childId,
                            isActive = true,
                            assignedAt = Instant.now(),
                            deletedAt = null
                        )
                        routineAssignmentRepository.create(assignment)
                    }
                }

                AppLogger.UI.info("Saved routine: ${routine.title} with ${state.steps.size} steps")
                _state.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (e: Exception) {
                AppLogger.UI.error("Error saving routine: ${e.message}", e)
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = "Failed to save routine: ${e.message}"
                    )
                }
            }
        }
    }
}

data class RoutineBuilderState(
    val familyId: String? = null,
    val existingRoutine: Routine? = null,
    val title: String = "",
    val iconName: String = "📋",
    val steps: List<StepInput> = emptyList(),
    val children: List<ChildProfile> = emptyList(),
    val selectedChildIds: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val canSave: Boolean = false // Computed from title and steps
)

data class StepInput(
    val label: String,
    val iconName: String
)


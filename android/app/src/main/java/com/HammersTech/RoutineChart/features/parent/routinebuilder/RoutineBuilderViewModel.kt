package com.HammersTech.RoutineChart.features.parent.routinebuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.domain.models.ChildProfile
import com.HammersTech.RoutineChart.core.domain.models.Routine
import com.HammersTech.RoutineChart.core.domain.models.RoutineAssignment
import com.HammersTech.RoutineChart.core.domain.models.RoutineStep
import com.HammersTech.RoutineChart.core.domain.repositories.ChildProfileRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineAssignmentRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineStepRepository
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
    private val familyRepository: FamilyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RoutineBuilderState())
    val state: StateFlow<RoutineBuilderState> = _state.asStateFlow()

    fun initialize(routine: Routine?) {
        viewModelScope.launch {
            try {
                // Get family
                val family = familyRepository.getFirst()
                if (family == null) {
                    _state.update { it.copy(error = "No family found") }
                    return@launch
                }

                val familyId = family.id

                // Load children
                val children = childProfileRepository.getByFamilyId(familyId)

                // If editing, load existing data
                if (routine != null) {
                    val steps = routineStepRepository.getByRoutineId(routine.id)
                        .sortedBy { it.orderIndex }
                        .map { StepInput(it.label ?: "", it.iconName ?: "⚪️") }

                    val assignments = routineAssignmentRepository.getByRoutineId(routine.id)
                    val selectedChildIds = assignments.filter { it.isActive }.map { it.childId }.toSet()

                    _state.update {
                        it.copy(
                            familyId = familyId,
                            existingRoutine = routine,
                            title = routine.title,
                            iconName = routine.iconName ?: "📋",
                            steps = steps,
                            children = children,
                            selectedChildIds = selectedChildIds
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            familyId = familyId,
                            children = children
                        )
                    }
                }

                AppLogger.UI.info("Initialized routine builder with ${children.size} children")
            } catch (e: Exception) {
                AppLogger.UI.error("Error initializing routine builder: ${e.message}", e)
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateTitle(title: String) {
        _state.update { it.copy(title = title) }
    }

    fun updateIconName(iconName: String) {
        _state.update { it.copy(iconName = iconName) }
    }

    fun updateStep(index: Int, label: String, iconName: String) {
        val updatedSteps = _state.value.steps.toMutableList()
        updatedSteps[index] = StepInput(label, iconName)
        _state.update { it.copy(steps = updatedSteps) }
    }

    fun addStep() {
        val updatedSteps = _state.value.steps + StepInput("", "⚪️")
        _state.update { it.copy(steps = updatedSteps) }
    }

    fun removeStep(index: Int) {
        val updatedSteps = _state.value.steps.toMutableList()
        updatedSteps.removeAt(index)
        _state.update { it.copy(steps = updatedSteps) }
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
        return _state.value.title.isNotBlank() &&
                _state.value.steps.isNotEmpty() &&
                _state.value.steps.all { it.label.isNotBlank() }
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

                    // Create new steps
                    state.steps.forEachIndexed { index, stepInput ->
                        val step = RoutineStep(
                            id = UUID.randomUUID().toString(),
                            routineId = state.existingRoutine.id,
                            familyId = familyId,
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
                    // Create new routine
                    routine = createRoutineUseCase(
                        familyId = familyId,
                        title = state.title,
                        iconName = state.iconName,
                        steps = state.steps.map { CreateRoutineUseCase.StepInput(it.label, it.iconName) }
                    )
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
    val error: String? = null
)

data class StepInput(
    val label: String,
    val iconName: String
)


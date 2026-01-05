package com.HammersTech.RoutineChart.features.parent.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.domain.models.ChildProfile
import com.HammersTech.RoutineChart.core.domain.models.Routine
import com.HammersTech.RoutineChart.core.domain.repositories.ChildProfileRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for parent dashboard
 */
@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val routineRepository: RoutineRepository,
    private val childProfileRepository: ChildProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ParentDashboardState())
    val state: StateFlow<ParentDashboardState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                // Get family
                val family = familyRepository.getFirst()
                if (family == null) {
                    _state.update { it.copy(isLoading = false, error = "No family found") }
                    return@launch
                }

                // Load routines (exclude deleted)
                val allRoutines = routineRepository.getByFamilyId(family.id)
                val activeRoutines = allRoutines.filter { it.deletedAt == null }
                    .sortedBy { it.createdAt }

                // Load children
                val children = childProfileRepository.getByFamilyId(family.id)

                _state.update {
                    it.copy(
                        familyId = family.id,
                        routines = activeRoutines,
                        children = children,
                        isLoading = false
                    )
                }

                AppLogger.UI.info("Loaded ${activeRoutines.size} routines and ${children.size} children")
            } catch (e: Exception) {
                AppLogger.UI.error("Error loading dashboard data: ${e.message}", e)
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch {
            try {
                routineRepository.softDelete(routineId)
                loadData() // Reload
                AppLogger.UI.info("Deleted routine: $routineId")
            } catch (e: Exception) {
                AppLogger.UI.error("Error deleting routine: ${e.message}", e)
                _state.update { it.copy(error = "Failed to delete routine") }
            }
        }
    }
}

data class ParentDashboardState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val familyId: String? = null,
    val routines: List<Routine> = emptyList(),
    val children: List<ChildProfile> = emptyList()
)


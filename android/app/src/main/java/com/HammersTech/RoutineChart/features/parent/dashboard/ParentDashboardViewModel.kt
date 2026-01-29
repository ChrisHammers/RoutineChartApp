package com.HammersTech.RoutineChart.features.parent.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.core.domain.models.ChildProfile
import com.HammersTech.RoutineChart.core.domain.models.Routine
import com.HammersTech.RoutineChart.core.data.remote.firebase.CompositeRoutineRepository
import com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository
import com.HammersTech.RoutineChart.core.domain.repositories.ChildProfileRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
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
    private val childProfileRepository: ChildProfileRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ParentDashboardState())
    val state: StateFlow<ParentDashboardState> = _state.asStateFlow()

    /** Tracks which user's data is currently loaded so we reload when the user switches */
    private var lastLoadedUserId: String? = null

    init {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { authUser ->
                val newId = authUser?.id
                if (newId != lastLoadedUserId) {
                    lastLoadedUserId = newId
                    if (newId == null) {
                        _state.update {
                            it.copy(
                                routines = emptyList(),
                                children = emptyList(),
                                familyId = null,
                                isLoading = false,
                                error = "Not signed in"
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                routines = emptyList(),
                                children = emptyList(),
                                familyId = null,
                                isLoading = true,
                                error = null
                            )
                        }
                        loadData()
                    }
                }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                // Get current authenticated user to find their familyId
                val authUser = authRepository.currentUser
                if (authUser == null) {
                    _state.update { it.copy(isLoading = false, error = "Not signed in") }
                    return@launch
                }
                
                // Get user record to find their familyId
                val user = userRepository.getById(authUser.id)
                if (user == null) {
                    _state.update { it.copy(isLoading = false, error = "User not found. Please join a family first.") }
                    return@launch
                }

                // CRITICAL: Pull routines from Firestore BEFORE loading from local
                // This ensures we have the latest data when the UI loads
                if (routineRepository is CompositeRoutineRepository) {
                    try {
                        // First, upload any unsynced local changes
                        val uploaded = routineRepository.uploadUnsynced(user.id, user.familyId)
                        if (uploaded > 0) {
                            AppLogger.UI.info("✅ Uploaded $uploaded unsynced routine(s) before loading dashboard")
                        }
                        
                        // Then, pull any remote changes (this will also pull steps)
                        val pulled = routineRepository.pullRoutines(user.id, user.familyId)
                        if (pulled > 0) {
                            AppLogger.UI.info("✅ Pulled $pulled routine(s) from Firestore before loading dashboard")
                        }
                    } catch (e: Exception) {
                        AppLogger.UI.error("⚠️ Failed to sync routines before loading dashboard: ${e.message}", e)
                        // Continue loading local data even if sync fails
                    }
                }

                // Load routines (exclude deleted) for the user (by userId, optionally filtered by familyId)
                val allRoutines = routineRepository.getAll(user.id, user.familyId, includeDeleted = false)
                val activeRoutines = allRoutines.filter { it.deletedAt == null }
                    .sortedBy { it.createdAt }

                // Load children for the user's family
                val children = childProfileRepository.getByFamilyId(user.familyId)

                _state.update {
                    it.copy(
                        familyId = user.familyId,
                        routines = activeRoutines,
                        children = children,
                        isLoading = false
                    )
                }

                AppLogger.UI.info("Loaded ${activeRoutines.size} routines and ${children.size} children for family: ${user.familyId}")
            } catch (e: Exception) {
                AppLogger.UI.error("Error loading dashboard data: ${e.message}", e)
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch {
            try {
                // Soft delete by setting deletedAt
                val routine = routineRepository.getById(routineId)
                if (routine != null) {
                    val deleted = routine.copy(deletedAt = java.time.Instant.now())
                    routineRepository.update(deleted)
                    loadData() // Reload
                    AppLogger.UI.info("Deleted routine: $routineId")
                }
            } catch (e: Exception) {
                AppLogger.UI.error("Error deleting routine: ${e.message}", e)
                _state.update { it.copy(error = "Failed to delete routine") }
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

data class ParentDashboardState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val familyId: String? = null,
    val routines: List<Routine> = emptyList(),
    val children: List<ChildProfile> = emptyList()
)


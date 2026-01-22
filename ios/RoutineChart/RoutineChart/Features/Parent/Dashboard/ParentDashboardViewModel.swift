import Foundation
import Combine
import OSLog

@MainActor
final class ParentDashboardViewModel: ObservableObject {
    @Published var routines: [Routine] = []
    @Published var children: [ChildProfile] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let routineRepository: RoutineRepository
    private let childProfileRepository: ChildProfileRepository
    private let familyRepository: FamilyRepository
    private let authRepository: AuthRepository
    private let userRepository: UserRepository
    
    private var familyId: String?
    
    init(
        routineRepository: RoutineRepository,
        childProfileRepository: ChildProfileRepository,
        familyRepository: FamilyRepository,
        authRepository: AuthRepository,
        userRepository: UserRepository
    ) {
        self.routineRepository = routineRepository
        self.childProfileRepository = childProfileRepository
        self.familyRepository = familyRepository
        self.authRepository = authRepository
        self.userRepository = userRepository
    }
    
    func loadData() async {
        isLoading = true
        errorMessage = nil
        
        do {
            // Get current authenticated user to find their familyId
            guard let authUser = authRepository.currentUser else {
                errorMessage = "Not signed in"
                isLoading = false
                return
            }
            
            // Get user record to find their familyId
            guard let user = try await userRepository.get(id: authUser.id) else {
                errorMessage = "User not found. Please join a family first."
                isLoading = false
                return
            }
            
            familyId = user.familyId
            
            // CRITICAL: Pull routines from Firestore BEFORE loading from local
            // This ensures we have the latest data when the UI loads
            if let compositeRepo = routineRepository as? CompositeRoutineRepository {
                do {
                    // First, upload any unsynced local changes
                    let uploaded = try await compositeRepo.uploadUnsynced(familyId: user.familyId)
                    if uploaded > 0 {
                        AppLogger.ui.info("✅ Uploaded \(uploaded) unsynced routine(s) before loading dashboard")
                    }
                    
                    // Then, pull any remote changes (this will also pull steps)
                    let pulled = try await compositeRepo.pullRoutines(userId: user.id, familyId: user.familyId)
                    if pulled > 0 {
                        AppLogger.ui.info("✅ Pulled \(pulled) routine(s) from Firestore before loading dashboard")
                    }
                } catch {
                    AppLogger.ui.warning("⚠️ Failed to sync routines before loading dashboard: \(error.localizedDescription)")
                    // Continue loading local data even if sync fails
                }
            }
            
            // Load routines (exclude deleted) for the user's family
            let allRoutines = try await routineRepository.getAll(familyId: user.familyId, includeDeleted: false)
            routines = allRoutines.sorted { $0.createdAt < $1.createdAt }
            
            // Load children for the user's family
            children = try await childProfileRepository.getAll(familyId: user.familyId)
            
            AppLogger.ui.info("Loaded \(self.routines.count) routines and \(self.children.count) children for family: \(user.familyId)")
        } catch {
            AppLogger.ui.error("Error loading dashboard data: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func deleteRoutine(_ routine: Routine) async {
        do {
            try await routineRepository.softDelete(id: routine.id)
            await loadData() // Reload
            AppLogger.ui.info("Deleted routine: \(routine.title)")
        } catch {
            AppLogger.ui.error("Error deleting routine: \(error.localizedDescription)")
            errorMessage = "Failed to delete routine"
        }
    }
    
    func signOut() {
        do {
            try authRepository.signOut()
            AppLogger.ui.info("User signed out")
        } catch {
            AppLogger.ui.error("Error signing out: \(error.localizedDescription)")
            errorMessage = "Failed to sign out"
        }
    }
}


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
    
    private var familyId: String?
    
    init(
        routineRepository: RoutineRepository,
        childProfileRepository: ChildProfileRepository,
        familyRepository: FamilyRepository
    ) {
        self.routineRepository = routineRepository
        self.childProfileRepository = childProfileRepository
        self.familyRepository = familyRepository
    }
    
    func loadData() async {
        isLoading = true
        errorMessage = nil
        
        do {
            // Get family
            let families = try await familyRepository.getAll()
            guard let family = families.first else {
                errorMessage = "No family found"
                isLoading = false
                return
            }
            
            familyId = family.id
            
            // Load routines (exclude deleted)
            let allRoutines = try await routineRepository.getAll(familyId: family.id, includeDeleted: false)
            routines = allRoutines.sorted { $0.createdAt < $1.createdAt }
            
            // Load children
            children = try await childProfileRepository.getAll(familyId: family.id)
            
            AppLogger.ui.info("Loaded \(self.routines.count) routines and \(self.children.count) children")
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
}


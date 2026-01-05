import SwiftUI

struct ParentDashboardView: View {
    let dependencies: AppDependencies
    @StateObject private var viewModel: ParentDashboardViewModel
    @State private var showingRoutineBuilder = false
    @State private var selectedRoutine: Routine?
    
    init(dependencies: AppDependencies) {
        self.dependencies = dependencies
        _viewModel = StateObject(wrappedValue: ParentDashboardViewModel(
            routineRepository: dependencies.routineRepository,
            childProfileRepository: dependencies.childProfileRepository,
            familyRepository: dependencies.familyRepository,
            authRepository: dependencies.authRepository
        ))
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.isLoading {
                    ProgressView("Loading...")
                } else if let error = viewModel.errorMessage {
                    VStack(spacing: 16) {
                        Text("Error")
                            .font(.headline)
                        Text(error)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                        Button("Retry") {
                            Task { await viewModel.loadData() }
                        }
                    }
                    .padding()
                } else {
                    routinesList
                }
            }
            .navigationTitle("Routines")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { viewModel.signOut() }) {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingRoutineBuilder = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showingRoutineBuilder) {
                RoutineBuilderView(
                    routine: selectedRoutine,
                    dependencies: dependencies,
                    onDismiss: {
                        showingRoutineBuilder = false
                        selectedRoutine = nil
                        Task { await viewModel.loadData() }
                    }
                )
            }
            .task {
                await viewModel.loadData()
            }
        }
    }
    
    private var routinesList: some View {
        Group {
            if viewModel.routines.isEmpty {
                emptyState
            } else {
                List {
                    ForEach(viewModel.routines) { routine in
                        RoutineRow(routine: routine)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                selectedRoutine = routine
                                showingRoutineBuilder = true
                            }
                    }
                    .onDelete(perform: deleteRoutines)
                }
            }
        }
    }
    
    private var emptyState: some View {
        VStack(spacing: 20) {
            Image(systemName: "list.bullet.clipboard")
                .font(.system(size: 64))
                .foregroundColor(.secondary)
            
            Text("No Routines Yet")
                .font(.title2)
                .fontWeight(.semibold)
            
            Text("Create your first routine to get started!")
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            Button(action: { showingRoutineBuilder = true }) {
                Label("Create Routine", systemImage: "plus.circle.fill")
                    .font(.headline)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
    
    private func deleteRoutines(at offsets: IndexSet) {
        for index in offsets {
            let routine = viewModel.routines[index]
            Task {
                await viewModel.deleteRoutine(routine)
            }
        }
    }
}

struct RoutineRow: View {
    let routine: Routine
    
    var body: some View {
        HStack(spacing: 12) {
            // Icon
            Text(routine.iconName ?? "📋")
                .font(.title2)
            
            // Title
            VStack(alignment: .leading, spacing: 4) {
                Text(routine.title)
                    .font(.headline)
                
                Text("Version \(routine.version)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .foregroundColor(.secondary)
                .font(.caption)
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    ParentDashboardView(dependencies: AppDependencies())
}


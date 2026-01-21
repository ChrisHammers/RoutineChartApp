import SwiftUI

struct RoutineBuilderView: View {
    @StateObject private var viewModel: RoutineBuilderViewModel
    @Environment(\.dismiss) private var dismiss
    let onDismiss: () -> Void
    let routine: Routine?
    
    init(routine: Routine?, dependencies: AppDependencies, onDismiss: @escaping () -> Void) {
        self.routine = routine
        _viewModel = StateObject(wrappedValue: RoutineBuilderViewModel(
            routine: routine,
            createRoutineUseCase: dependencies.createRoutineUseCase,
            routineRepository: dependencies.routineRepository,
            routineStepRepository: dependencies.routineStepRepository,
            routineAssignmentRepository: dependencies.routineAssignmentRepository,
            childProfileRepository: dependencies.childProfileRepository,
            familyRepository: dependencies.familyRepository,
            authRepository: dependencies.authRepository,
            userRepository: dependencies.userRepository
        ))
        self.onDismiss = onDismiss
    }
    
    var body: some View {
        NavigationView {
            Form {
                // Routine Details Section
                Section("Routine Details") {
                    // Title
                    TextField("Routine Name", text: $viewModel.title)
                    
                    // Icon Picker (simple emoji input for now)
                    HStack {
                        Text("Icon")
                        Spacer()
                        TextField("📋", text: $viewModel.iconName)
                            .multilineTextAlignment(.trailing)
                            .font(.title2)
                    }
                }
                
                // Steps Section
                Section {
                    ForEach(Array(viewModel.steps.enumerated()), id: \.element.id) { index, step in
                        HStack(spacing: 12) {
                            // Step number
                            Text("\(index + 1).")
                                .foregroundColor(.secondary)
                                .frame(width: 30, alignment: .trailing)
                            
                            // Icon
                            TextField("⚪️", text: $viewModel.steps[index].iconName)
                                .font(.title3)
                                .frame(width: 40)
                            
                            // Label
                            TextField("Step name", text: $viewModel.steps[index].label)
                        }
                    }
                    .onDelete { indexSet in
                        indexSet.forEach { viewModel.removeStep(at: $0) }
                    }
                    .onMove(perform: viewModel.moveStep)
                    
                    Button(action: viewModel.addStep) {
                        Label("Add Step", systemImage: "plus.circle.fill")
                    }
                } header: {
                    Text("Steps")
                } footer: {
                    Text("Tap and hold to reorder steps")
                        .font(.caption)
                }
                
                // Child Assignment Section
                if !viewModel.children.isEmpty {
                    Section("Assign to Children") {
                        ForEach(viewModel.children) { child in
                            Toggle(isOn: Binding(
                                get: { viewModel.selectedChildIds.contains(child.id) },
                                set: { isSelected in
                                    if isSelected {
                                        viewModel.selectedChildIds.insert(child.id)
                                    } else {
                                        viewModel.selectedChildIds.remove(child.id)
                                    }
                                }
                            )) {
                                HStack {
                                    Text(child.avatarIcon ?? "👤")
                                        .font(.title3)
                                    Text(child.displayName)
                                }
                            }
                        }
                    }
                }
                
                // Error Section
                if let error = viewModel.errorMessage {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                }
            }
            .navigationTitle(routine == nil ? "New Routine" : "Edit Routine")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        onDismiss()
                    }
                    .disabled(viewModel.isSaving)
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Save") {
                        Task {
                            let success = await viewModel.save()
                            if success {
                                onDismiss()
                            }
                        }
                    }
                    .disabled(!viewModel.canSave() || viewModel.isSaving)
                }
            }
            .task {
                await viewModel.loadData()
            }
        }
    }
}

#Preview {
    RoutineBuilderView(
        routine: nil,
        dependencies: AppDependencies(),
        onDismiss: {}
    )
}


//
//  ChildTodayView.swift
//  RoutineChart
//
//  Child's today view showing assigned routines
//

import SwiftUI

struct ChildTodayView: View {
    @StateObject private var viewModel: ChildTodayViewModel
    
    init(dependencies: AppDependencies) {
        _viewModel = StateObject(wrappedValue: ChildTodayViewModel(dependencies: dependencies))
    }
    
    var body: some View {
        NavigationView {
            VStack {
                if viewModel.isLoading {
                    ProgressView()
                } else if let error = viewModel.error {
                    Text("Error: \(error)")
                        .foregroundColor(.red)
                } else {
                    // Child selector
                    if !viewModel.children.isEmpty {
                        Picker("Child", selection: Binding(
                            get: { viewModel.selectedChild },
                            set: { if let child = $0 {
                                Task { await viewModel.selectChild(child) }
                            }}
                        )) {
                            ForEach(viewModel.children) { child in
                                Text("\(child.avatarIcon ?? "👤") \(child.displayName)")
                                    .tag(child as ChildProfile?)
                            }
                        }
                        .pickerStyle(.segmented)
                        .padding()
                    }
                    
                    // Routines list
                    if viewModel.routines.isEmpty {
                        Text("No routines assigned")
                            .foregroundColor(.secondary)
                            .padding()
                    } else {
                        ScrollView {
                            VStack(spacing: 16) {
                                ForEach(viewModel.routines) { routineWithSteps in
                                    RoutineCard(
                                        routineWithSteps: routineWithSteps,
                                        onToggleStep: { step in
                                            Task {
                                                await viewModel.toggleStep(
                                                    routine: routineWithSteps,
                                                    step: step
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            .padding()
                        }
                    }
                }
            }
            .navigationTitle("Today's Routines")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { viewModel.showSettings = true }) {
                        Image(systemName: "gearshape")
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { viewModel.signOut() }) {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                    }
                }
            }
            .sheet(isPresented: $viewModel.showSettings) {
                SettingsView(dependencies: viewModel.dependencies)
            }
            .task {
                await viewModel.loadData()
            }
        }
    }
}

// MARK: - Routine Card

struct RoutineCard: View {
    let routineWithSteps: RoutineWithSteps
    let onToggleStep: (StepWithCompletion) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Routine header
            HStack {
                Text(routineWithSteps.routine.iconName ?? "📋")
                    .font(.title)
                Text(routineWithSteps.routine.title)
                    .font(.title2)
                    .fontWeight(.bold)
                Spacer()
                Text(completionText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Divider()
            
            // Steps list
            ForEach(routineWithSteps.steps) { stepWithCompletion in
                StepRow(
                    stepWithCompletion: stepWithCompletion,
                    onToggle: { onToggleStep(stepWithCompletion) }
                )
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 2)
    }
    
    private var completionText: String {
        let completed = routineWithSteps.steps.filter { $0.isComplete }.count
        let total = routineWithSteps.steps.count
        return "\(completed)/\(total) steps"
    }
}

// MARK: - Step Row

struct StepRow: View {
    let stepWithCompletion: StepWithCompletion
    let onToggle: () -> Void
    
    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: 12) {
                // Checkbox
                Image(systemName: stepWithCompletion.isComplete ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundColor(stepWithCompletion.isComplete ? .green : .gray)
                
                // Icon
                if let icon = stepWithCompletion.step.iconName {
                    Text(icon)
                        .font(.title3)
                }
                
                // Label
                Text(stepWithCompletion.step.label ?? "Step \(stepWithCompletion.step.orderIndex + 1)")
                    .font(.body)
                    .foregroundColor(stepWithCompletion.isComplete ? .secondary : .primary)
                    .strikethrough(stepWithCompletion.isComplete)
                
                Spacer()
            }
            .padding(.vertical, 8)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Preview

#Preview {
    ChildTodayView(dependencies: AppDependencies())
}


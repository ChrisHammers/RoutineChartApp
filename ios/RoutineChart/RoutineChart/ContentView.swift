//
//  ContentView.swift
//  RoutineChart
//
//  Created by Christopher Hammers on 1/2/26.
//

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var dependencies: AppDependencies
    
    var body: some View {
        ChildTodayView(dependencies: dependencies)
    }
}

#Preview {
    ContentView()
        .environmentObject(AppDependencies())
}

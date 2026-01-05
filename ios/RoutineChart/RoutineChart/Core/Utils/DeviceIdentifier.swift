//
//  DeviceIdentifier.swift
//  RoutineChart
//
//  Device ID generation and storage
//

import Foundation

struct DeviceIdentifier {
    private static let key = "com.routinechart.deviceId"
    
    static func get() -> String {
        if let existing = UserDefaults.standard.string(forKey: key) {
            return existing
        }
        
        let newId = UUID().uuidString
        UserDefaults.standard.set(newId, forKey: key)
        return newId
    }
}


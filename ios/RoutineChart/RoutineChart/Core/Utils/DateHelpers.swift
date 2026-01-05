//
//  DateHelpers.swift
//  RoutineChart
//
//  Date utility extensions
//

import Foundation

extension Date {
    /// Converts date to local day key (YYYY-MM-DD) in specified timezone
    func localDayKey(timeZone: TimeZone) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.timeZone = timeZone
        return formatter.string(from: self)
    }
    
    /// Converts date to local day key using current timezone
    func localDayKey() -> String {
        return localDayKey(timeZone: .current)
    }
}

extension TimeZone {
    /// Create timezone from IANA identifier string
    static func from(identifier: String) -> TimeZone {
        return TimeZone(identifier: identifier) ?? .current
    }
}


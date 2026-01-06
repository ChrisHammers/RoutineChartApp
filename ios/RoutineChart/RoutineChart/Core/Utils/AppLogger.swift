//
//  AppLogger.swift
//  RoutineChart
//
//  Centralized logging utility
//

import Foundation
import OSLog

struct AppLogger {
    private static let subsystem = "com.routinechart.app"
    
    static let database = Logger(subsystem: subsystem, category: "database")
    static let sync = Logger(subsystem: subsystem, category: "sync")
    static let ui = Logger(subsystem: subsystem, category: "ui")
    static let domain = Logger(subsystem: subsystem, category: "domain")
    
    // General purpose logger
    static func log(_ message: String, category: String = "general") {
        let logger = Logger(subsystem: subsystem, category: category)
        logger.info("\(message)")
    }
    
    static func error(_ message: String, category: String = "general") {
        let logger = Logger(subsystem: subsystem, category: category)
        logger.error("\(message)")
    }
}


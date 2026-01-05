//
//  ULIDGenerator.swift
//  RoutineChart
//
//  ULID generator for event IDs
//

import Foundation
import ULID
import Combine

struct ULIDGenerator {
    static func generate() -> String {
        return ULID().ulidString
    }
}


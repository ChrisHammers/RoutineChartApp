//
//  DatabaseExtensions.swift
//  RoutineChart
//
//  GRDB extensions for domain models
//

import Foundation
import GRDB

// MARK: - Family

extension Family: FetchableRecord, MutablePersistableRecord {
    static let databaseTableName = "families"
    
    enum Columns: String, ColumnExpression {
        case id, name, timeZone, weekStartsOn, planTier, createdAt, updatedAt
    }
    
    func encode(to container: inout PersistenceContainer) {
        container[Columns.id] = id
        container[Columns.name] = name
        container[Columns.timeZone] = timeZone
        container[Columns.weekStartsOn] = weekStartsOn
        container[Columns.planTier] = planTier.rawValue
        container[Columns.createdAt] = createdAt
        container[Columns.updatedAt] = updatedAt
    }
}

// MARK: - User

extension User: FetchableRecord, MutablePersistableRecord {
    static let databaseTableName = "users"
    
    enum Columns: String, ColumnExpression {
        case id, familyId, role, displayName, email, createdAt
    }
    
    func encode(to container: inout PersistenceContainer) {
        container[Columns.id] = id
        container[Columns.familyId] = familyId
        container[Columns.role] = role.rawValue
        container[Columns.displayName] = displayName
        container[Columns.email] = email
        container[Columns.createdAt] = createdAt
    }
}

// MARK: - ChildProfile

extension ChildProfile: FetchableRecord, MutablePersistableRecord {
    static let databaseTableName = "child_profiles"
    
    enum Columns: String, ColumnExpression {
        case id, familyId, displayName, avatarIcon, ageBand, readingMode, audioEnabled, createdAt
    }
    
    func encode(to container: inout PersistenceContainer) {
        container[Columns.id] = id
        container[Columns.familyId] = familyId
        container[Columns.displayName] = displayName
        container[Columns.avatarIcon] = avatarIcon
        container[Columns.ageBand] = ageBand.rawValue
        container[Columns.readingMode] = readingMode.rawValue
        container[Columns.audioEnabled] = audioEnabled
        container[Columns.createdAt] = createdAt
    }
}

// MARK: - Routine

extension Routine: FetchableRecord, MutablePersistableRecord {
    static let databaseTableName = "routines"
    
    enum Columns: String, ColumnExpression {
        case id, familyId, title, iconName, version, completionRule, createdAt, updatedAt, deletedAt
    }
    
    func encode(to container: inout PersistenceContainer) {
        container[Columns.id] = id
        container[Columns.familyId] = familyId
        container[Columns.title] = title
        container[Columns.iconName] = iconName
        container[Columns.version] = version
        container[Columns.completionRule] = completionRule.rawValue
        container[Columns.createdAt] = createdAt
        container[Columns.updatedAt] = updatedAt
        container[Columns.deletedAt] = deletedAt
    }
}

// MARK: - RoutineStep

extension RoutineStep: FetchableRecord, MutablePersistableRecord {
    static let databaseTableName = "routine_steps"
    
    enum Columns: String, ColumnExpression {
        case id, routineId, familyId, orderIndex, label, iconName, audioCueUrl, createdAt, deletedAt
    }
    
    func encode(to container: inout PersistenceContainer) {
        container[Columns.id] = id
        container[Columns.routineId] = routineId
        container[Columns.familyId] = familyId
        container[Columns.orderIndex] = orderIndex
        container[Columns.label] = label
        container[Columns.iconName] = iconName
        container[Columns.audioCueUrl] = audioCueUrl
        container[Columns.createdAt] = createdAt
        container[Columns.deletedAt] = deletedAt
    }
}

// MARK: - RoutineAssignment

extension RoutineAssignment: FetchableRecord, MutablePersistableRecord {
    static let databaseTableName = "routine_assignments"
    
    enum Columns: String, ColumnExpression {
        case id, familyId, routineId, childId, isActive, assignedAt, deletedAt
    }
    
    func encode(to container: inout PersistenceContainer) {
        container[Columns.id] = id
        container[Columns.familyId] = familyId
        container[Columns.routineId] = routineId
        container[Columns.childId] = childId
        container[Columns.isActive] = isActive
        container[Columns.assignedAt] = assignedAt
        container[Columns.deletedAt] = deletedAt
    }
}

// MARK: - CompletionEvent

extension CompletionEvent: FetchableRecord, MutablePersistableRecord {
    static let databaseTableName = "completion_events"
    
    enum Columns: String, ColumnExpression {
        case id, familyId, childId, routineId, stepId, eventType, eventAt, localDayKey, deviceId, synced
    }
    
    func encode(to container: inout PersistenceContainer) {
        container[Columns.id] = id
        container[Columns.familyId] = familyId
        container[Columns.childId] = childId
        container[Columns.routineId] = routineId
        container[Columns.stepId] = stepId
        container[Columns.eventType] = eventType.rawValue
        container[Columns.eventAt] = eventAt
        container[Columns.localDayKey] = localDayKey
        container[Columns.deviceId] = deviceId
        container[Columns.synced] = synced
    }
}

// MARK: - FamilyInvite

extension FamilyInvite: FetchableRecord, MutablePersistableRecord {
    static let databaseTableName = "family_invites"
    
    enum Columns: String, ColumnExpression {
        case id, familyId, token, inviteCode, createdBy, createdAt, expiresAt, maxUses, usedCount, isActive
    }
    
    func encode(to container: inout PersistenceContainer) {
        container[Columns.id] = id
        container[Columns.familyId] = familyId
        container[Columns.token] = token
        container[Columns.inviteCode] = inviteCode
        container[Columns.createdBy] = createdBy
        container[Columns.createdAt] = createdAt
        container[Columns.expiresAt] = expiresAt
        container[Columns.maxUses] = maxUses
        container[Columns.usedCount] = usedCount
        container[Columns.isActive] = isActive
    }
}

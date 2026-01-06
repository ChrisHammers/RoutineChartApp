//
//  FamilyInviteRepository.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining
//

import Foundation

protocol FamilyInviteRepository {
    /// Create a new family invite
    func create(_ invite: FamilyInvite) async throws
    
    /// Get invite by ID
    func get(id: String) async throws -> FamilyInvite?
    
    /// Get invite by token
    func getByToken(_ token: String) async throws -> FamilyInvite?
    
    /// Get invite by invite code (e.g., "ABC-1234")
    func getByInviteCode(_ inviteCode: String) async throws -> FamilyInvite?
    
    /// Get all active invites for a family
    func getActiveInvites(familyId: String) async throws -> [FamilyInvite]
    
    /// Update an invite (for incrementing usedCount or deactivating)
    func update(_ invite: FamilyInvite) async throws
    
    /// Deactivate an invite
    func deactivate(id: String) async throws
    
    /// Delete expired invites
    func deleteExpired() async throws
}


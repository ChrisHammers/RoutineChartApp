package com.HammersTech.RoutineChart.core.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.HammersTech.RoutineChart.core.domain.models.FamilyInvite
import java.time.Instant

/**
 * Room entity for family invites
 * Phase 2.2: QR Family Joining
 */
@Entity(
    tableName = "family_invites",
    foreignKeys = [
        ForeignKey(
            entity = FamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["familyId"]),
        Index(value = ["token"], unique = true)
    ]
)
data class FamilyInviteEntity(
    @PrimaryKey
    val id: String,
    val familyId: String,
    val token: String,
    val createdBy: String,
    val createdAt: Long,
    val expiresAt: Long,
    val maxUses: Int?,
    val usedCount: Int,
    val isActive: Boolean
) {
    fun toDomain(): FamilyInvite {
        return FamilyInvite(
            id = id,
            familyId = familyId,
            token = token,
            createdBy = createdBy,
            createdAt = Instant.ofEpochMilli(createdAt),
            expiresAt = Instant.ofEpochMilli(expiresAt),
            maxUses = maxUses,
            usedCount = usedCount,
            isActive = isActive
        )
    }
    
    companion object {
        fun fromDomain(invite: FamilyInvite): FamilyInviteEntity {
            return FamilyInviteEntity(
                id = invite.id,
                familyId = invite.familyId,
                token = invite.token,
                createdBy = invite.createdBy,
                createdAt = invite.createdAt.toEpochMilli(),
                expiresAt = invite.expiresAt.toEpochMilli(),
                maxUses = invite.maxUses,
                usedCount = invite.usedCount,
                isActive = invite.isActive
            )
        }
    }
}


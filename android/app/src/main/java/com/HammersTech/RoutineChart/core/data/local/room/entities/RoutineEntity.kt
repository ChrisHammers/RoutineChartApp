package com.HammersTech.RoutineChart.core.data.local.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.HammersTech.RoutineChart.core.domain.models.CompletionRule
import com.HammersTech.RoutineChart.core.domain.models.Routine
import java.time.Instant

@Entity(
    tableName = "routines",
    // Note: No foreign key constraint on familyId to allow routines to reference families
    // that don't exist locally yet (e.g., when pulling from Firestore)
    indices = [Index("userId"), Index("familyId"), Index("synced")],
)
data class RoutineEntity(
    @PrimaryKey
    val id: String,
    val userId: String, // Owner of the routine (required)
    val familyId: String?, // Optional - if null, routine is personal
    val title: String,
    val iconName: String?,
    val version: Int,
    val completionRule: CompletionRule,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val synced: Boolean = false, // Phase 3.2: Upload queue - tracks if synced to Firestore
) {
    fun toDomain(): Routine =
        Routine(
            id = id,
            userId = userId,
            familyId = familyId,
            title = title,
            iconName = iconName,
            version = version,
            completionRule = completionRule,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )

    companion object {
        fun fromDomain(
            routine: Routine,
            synced: Boolean = false,
        ): RoutineEntity =
            RoutineEntity(
                id = routine.id,
                userId = routine.userId,
                familyId = routine.familyId,
                title = routine.title,
                iconName = routine.iconName,
                version = routine.version,
                completionRule = routine.completionRule,
                createdAt = routine.createdAt,
                updatedAt = routine.updatedAt,
                deletedAt = routine.deletedAt,
                synced = synced,
            )
    }
}

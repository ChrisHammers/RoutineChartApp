package com.HammersTech.RoutineChart.core.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.HammersTech.RoutineChart.core.domain.models.CompletionRule
import com.HammersTech.RoutineChart.core.domain.models.Routine
import java.time.Instant

@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = FamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("familyId")]
)
data class RoutineEntity(
    @PrimaryKey
    val id: String,
    val familyId: String,
    val title: String,
    val iconName: String?,
    val version: Int,
    val completionRule: CompletionRule,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?
) {
    fun toDomain(): Routine = Routine(
        id = id,
        familyId = familyId,
        title = title,
        iconName = iconName,
        version = version,
        completionRule = completionRule,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    companion object {
        fun fromDomain(routine: Routine): RoutineEntity = RoutineEntity(
            id = routine.id,
            familyId = routine.familyId,
            title = routine.title,
            iconName = routine.iconName,
            version = routine.version,
            completionRule = routine.completionRule,
            createdAt = routine.createdAt,
            updatedAt = routine.updatedAt,
            deletedAt = routine.deletedAt
        )
    }
}


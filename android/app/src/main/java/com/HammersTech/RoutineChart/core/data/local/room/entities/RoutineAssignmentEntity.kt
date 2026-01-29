package com.HammersTech.RoutineChart.core.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.HammersTech.RoutineChart.core.domain.models.RoutineAssignment
import java.time.Instant

@Entity(
    tableName = "routine_assignments",
    foreignKeys = [
        ForeignKey(
            entity = FamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChildProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["childId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("familyId"), Index("routineId"), Index("childId"), Index("synced")]
)
data class RoutineAssignmentEntity(
    @PrimaryKey
    val id: String,
    val familyId: String,
    val routineId: String,
    val childId: String,
    val isActive: Boolean,
    val assignedAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val synced: Int = 0
) {
    fun toDomain(): RoutineAssignment = RoutineAssignment(
        id = id,
        familyId = familyId,
        routineId = routineId,
        childId = childId,
        isActive = isActive,
        assignedAt = assignedAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    companion object {
        fun fromDomain(assignment: RoutineAssignment, synced: Int = 0): RoutineAssignmentEntity = RoutineAssignmentEntity(
            id = assignment.id,
            familyId = assignment.familyId,
            routineId = assignment.routineId,
            childId = assignment.childId,
            isActive = assignment.isActive,
            assignedAt = assignment.assignedAt,
            updatedAt = assignment.updatedAt,
            deletedAt = assignment.deletedAt,
            synced = synced
        )
    }
}


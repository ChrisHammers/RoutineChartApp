package com.HammersTech.RoutineChart.core.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.HammersTech.RoutineChart.core.domain.models.RoutineStep
import java.time.Instant

@Entity(
    tableName = "routine_steps",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineId")],
)
data class RoutineStepEntity(
    @PrimaryKey
    val id: String,
    val routineId: String,
    val orderIndex: Int,
    val label: String?,
    val iconName: String?,
    val audioCueUrl: String?,
    val createdAt: Instant,
    val deletedAt: Instant?,
    val synced: Boolean = false, // Phase 3.4: Upload queue - tracks if synced to Firestore
) {
    fun toDomain(): RoutineStep =
        RoutineStep(
            id = id,
            routineId = routineId,
            orderIndex = orderIndex,
            label = label,
            iconName = iconName,
            audioCueUrl = audioCueUrl,
            createdAt = createdAt,
            deletedAt = deletedAt,
        )

    companion object {
        fun fromDomain(
            step: RoutineStep,
            synced: Boolean = false,
        ): RoutineStepEntity =
            RoutineStepEntity(
                id = step.id,
                routineId = step.routineId,
                orderIndex = step.orderIndex,
                label = step.label,
                iconName = step.iconName,
                audioCueUrl = step.audioCueUrl,
                createdAt = step.createdAt,
                deletedAt = step.deletedAt,
                synced = synced,
            )
    }
}

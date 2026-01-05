package com.HammersTech.RoutineChart.core.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.HammersTech.RoutineChart.core.domain.models.CompletionEvent
import com.HammersTech.RoutineChart.core.domain.models.EventType
import java.time.Instant

@Entity(
    tableName = "completion_events",
    foreignKeys = [
        ForeignKey(
            entity = FamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChildProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["childId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoutineStepEntity::class,
            parentColumns = ["id"],
            childColumns = ["stepId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("familyId"),
        Index("childId"),
        Index("routineId"),
        Index("stepId"),
        Index("localDayKey"),
        Index("eventAt")
    ]
)
data class CompletionEventEntity(
    @PrimaryKey
    val id: String, // ULID
    val familyId: String,
    val childId: String,
    val routineId: String,
    val stepId: String,
    val eventType: EventType,
    val eventAt: Instant,
    val localDayKey: String,
    val deviceId: String,
    val synced: Boolean = false
) {
    fun toDomain(): CompletionEvent = CompletionEvent(
        id = id,
        familyId = familyId,
        childId = childId,
        routineId = routineId,
        stepId = stepId,
        eventType = eventType,
        eventAt = eventAt,
        localDayKey = localDayKey,
        deviceId = deviceId,
        synced = synced
    )

    companion object {
        fun fromDomain(event: CompletionEvent): CompletionEventEntity = CompletionEventEntity(
            id = event.id,
            familyId = event.familyId,
            childId = event.childId,
            routineId = event.routineId,
            stepId = event.stepId,
            eventType = event.eventType,
            eventAt = event.eventAt,
            localDayKey = event.localDayKey,
            deviceId = event.deviceId,
            synced = event.synced
        )
    }
}


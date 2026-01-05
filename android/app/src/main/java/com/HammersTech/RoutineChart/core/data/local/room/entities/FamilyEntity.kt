package com.HammersTech.RoutineChart.core.data.local.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.HammersTech.RoutineChart.core.domain.models.Family
import com.HammersTech.RoutineChart.core.domain.models.PlanTier
import java.time.Instant

@Entity(tableName = "families")
data class FamilyEntity(
    @PrimaryKey
    val id: String,
    val name: String?,
    val timeZone: String,
    val weekStartsOn: Int,
    val planTier: PlanTier,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun toDomain(): Family = Family(
        id = id,
        name = name,
        timeZone = timeZone,
        weekStartsOn = weekStartsOn,
        planTier = planTier,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(family: Family): FamilyEntity = FamilyEntity(
            id = family.id,
            name = family.name,
            timeZone = family.timeZone,
            weekStartsOn = family.weekStartsOn,
            planTier = family.planTier,
            createdAt = family.createdAt,
            updatedAt = family.updatedAt
        )
    }
}


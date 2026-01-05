package com.HammersTech.RoutineChart.core.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.HammersTech.RoutineChart.core.domain.models.AgeBand
import com.HammersTech.RoutineChart.core.domain.models.ChildProfile
import com.HammersTech.RoutineChart.core.domain.models.ReadingMode
import java.time.Instant

@Entity(
    tableName = "child_profiles",
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
data class ChildProfileEntity(
    @PrimaryKey
    val id: String,
    val familyId: String,
    val displayName: String,
    val avatarIcon: String?,
    val ageBand: AgeBand,
    val readingMode: ReadingMode,
    val audioEnabled: Boolean,
    val createdAt: Instant
) {
    fun toDomain(): ChildProfile = ChildProfile(
        id = id,
        familyId = familyId,
        displayName = displayName,
        avatarIcon = avatarIcon,
        ageBand = ageBand,
        readingMode = readingMode,
        audioEnabled = audioEnabled,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(profile: ChildProfile): ChildProfileEntity = ChildProfileEntity(
            id = profile.id,
            familyId = profile.familyId,
            displayName = profile.displayName,
            avatarIcon = profile.avatarIcon,
            ageBand = profile.ageBand,
            readingMode = profile.readingMode,
            audioEnabled = profile.audioEnabled,
            createdAt = profile.createdAt
        )
    }
}


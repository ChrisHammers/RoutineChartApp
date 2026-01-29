package com.HammersTech.RoutineChart.core.data.local.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.HammersTech.RoutineChart.core.domain.models.Role
import com.HammersTech.RoutineChart.core.domain.models.User
import java.time.Instant

@Entity(
    tableName = "users",
    foreignKeys = [
        ForeignKey(
            entity = FamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("familyId")],
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val familyId: String,
    val role: Role,
    val displayName: String,
    val email: String?,
    val createdAt: Instant,
) {
    fun toDomain(): User =
        User(
            id = id,
            familyId = familyId,
            role = role,
            displayName = displayName,
            email = email,
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(user: User): UserEntity =
            UserEntity(
                id = user.id,
                familyId = user.familyId,
                role = user.role,
                displayName = user.displayName,
                email = user.email,
                createdAt = user.createdAt,
            )
    }
}

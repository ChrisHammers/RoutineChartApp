package com.HammersTech.RoutineChart.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.HammersTech.RoutineChart.core.data.local.room.entities.ChildProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ChildProfileEntity)

    @Update
    suspend fun update(profile: ChildProfileEntity)

    @Query("SELECT * FROM child_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChildProfileEntity?

    @Query("SELECT * FROM child_profiles WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ChildProfileEntity?>

    @Query("SELECT * FROM child_profiles WHERE familyId = :familyId")
    suspend fun getByFamilyId(familyId: String): List<ChildProfileEntity>

    @Query("SELECT * FROM child_profiles WHERE familyId = :familyId")
    fun observeByFamilyId(familyId: String): Flow<List<ChildProfileEntity>>
}


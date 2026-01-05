package com.HammersTech.RoutineChart.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.HammersTech.RoutineChart.core.data.local.room.entities.FamilyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(family: FamilyEntity)

    @Update
    suspend fun update(family: FamilyEntity)

    @Query("SELECT * FROM families WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FamilyEntity?

    @Query("SELECT * FROM families WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<FamilyEntity?>

    @Query("SELECT * FROM families LIMIT 1")
    suspend fun getFirst(): FamilyEntity?

    @Query("SELECT * FROM families")
    suspend fun getAll(): List<FamilyEntity>
}


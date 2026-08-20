package com.example.data.dao

import androidx.room.*
import com.example.data.entity.PaymentRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentRequestDao {
    @Query("SELECT * FROM payment_requests ORDER BY requestDate DESC, requestTime DESC")
    fun getAllRequests(): Flow<List<PaymentRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: PaymentRequestEntity)

    @Update
    suspend fun updateRequest(request: PaymentRequestEntity)

    @Query("DELETE FROM payment_requests WHERE id = :id")
    suspend fun deleteRequestById(id: String)
}

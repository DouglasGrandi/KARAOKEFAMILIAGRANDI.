package com.familiagrandi.karaoke

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Headers

data class DriveListResponse(
    val files: List<DriveVideo> = emptyList()
)

interface DriveApi {
    @GET("drive/v3/files")
    suspend fun listFiles(
        @Query("q") q: String,
        @Query("fields") fields: String = "files(id,name,mimeType)",
        @Query("key") key: String,
        @Query("pageSize") pageSize: Int = 1000
    ): DriveListResponse
}
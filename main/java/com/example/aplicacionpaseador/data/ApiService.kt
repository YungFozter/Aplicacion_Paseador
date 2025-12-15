package com.example.aplicacionpaseador.data

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {
    @POST("auth/walkerlogin")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("auth/walkerregister")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<Unit>

    @POST("walkers/availability")
    suspend fun setAvailability(@Header("Authorization") token: String, @Body availability: AvailabilityRequest): Response<Unit>

    @POST("walkers/photo")
    suspend fun uploadWalkerPhoto(@Header("Authorization") token: String, @Body photo: Any): Response<Unit> // TODO multipart

    @GET("walks/pending")
    suspend fun getPendingWalks(@Header("Authorization") token: String): Response<List<Walk>>

    @GET("walks/accepted")
    suspend fun getAcceptedWalks(@Header("Authorization") token: String): Response<List<Walk>>

    @POST("walks/{id}/accept")
    suspend fun acceptWalk(@Header("Authorization") token: String, @Path("id") id: Int): Response<Unit>

    @POST("walks/{id}/reject")
    suspend fun rejectWalk(@Header("Authorization") token: String, @Path("id") id: Int): Response<Unit>

    @POST("walks/{id}/start")
    suspend fun startWalk(@Header("Authorization") token: String, @Path("id") id: Int): Response<Unit>

    @POST("walks/{id}/end")
    suspend fun endWalk(@Header("Authorization") token: String, @Path("id") id: Int): Response<Unit>

    @GET("walks/{id}/photos")
    suspend fun getWalkPhotos(@Header("Authorization") token: String, @Path("id") id: Int): Response<List<String>>

    @Multipart
    @POST("walks/{id}/photo")
    suspend fun uploadWalkPhoto(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Part photo: MultipartBody.Part
    ): Response<Unit>

    @GET("me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserInfo>

    @GET("walks")
    suspend fun getAllWalks(@Header("Authorization") token: String): Response<List<Walk>>

    @GET("reviews")
    suspend fun getReviews(@Header("Authorization") token: String): Response<List<Review>>

    @GET("reviews/{id}")
    suspend fun getReview(@Header("Authorization") token: String, @Path("id") id: Int): Response<Review>
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("token")
    val accessToken: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val price_hour: String
)

data class AvailabilityRequest(
    @SerializedName("is_available") val isAvailable: Boolean
)

data class UserInfo(
    @SerializedName("name") val name: String?,
    @SerializedName("role") val role: String?
)

// Modelo para paseos
data class Walk(
    @SerializedName("id") val id: Int,
    @SerializedName("pet_id") val petId: Int?,
    @SerializedName("walker_id") val walkerId: Int?,
    @SerializedName("scheduled_at") val scheduledAt: String?,
    @SerializedName("duration_minutes") val durationMinutes: Int?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("status") val status: String?
)

data class Review(
    @SerializedName("id") val id: Int,
    @SerializedName("rating") val rating: Int?,
    @SerializedName("text") val text: String?,
    @SerializedName("walk_id") val walkId: Int?,
    @SerializedName("created_at") val createdAt: String?
)

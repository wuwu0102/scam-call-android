package com.alertanumero.mx.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface ScamDatabaseService {
    @GET
    suspend fun fetchDatabase(@Url url: String): Response<ResponseBody>
}

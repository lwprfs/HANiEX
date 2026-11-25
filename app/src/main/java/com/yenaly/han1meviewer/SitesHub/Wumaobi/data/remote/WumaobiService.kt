package com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface WumaobiService {

    @GET
    suspend fun get(
        @Url url: String,
        @Header(WumaobiNetwork.HEADER_SORT) sort: String?,
        @Header(WumaobiNetwork.HEADER_CATEGORY_SORT) categorySort: String?,
    ): Response<ResponseBody>

    @FormUrlEncoded
    @Headers("X-Requested-With: XMLHttpRequest", "Accept: application/json, text/javascript, */*; q=0.01")
    @POST
    suspend fun post(
        @Url url: String,
        @Header("Referer") referer: String,
        @Header("Origin") origin: String,
        @FieldMap fields: Map<String, String>,
    ): Response<ResponseBody>
}

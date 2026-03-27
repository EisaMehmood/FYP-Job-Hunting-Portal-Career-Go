package com.example.careergo.Utility;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Url;

public interface SupabaseStorageService {
    @PUT
    Call<ResponseBody> uploadFile(
            @Url String url,
            @Body RequestBody file,
            @Header("Authorization") String authorization,
            @Header("x-upsert") String upsert,
            @Header("Content-Type") String contentType
    );
}
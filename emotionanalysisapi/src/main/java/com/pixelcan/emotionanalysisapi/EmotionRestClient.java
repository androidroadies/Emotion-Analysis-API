package com.pixelcan.emotionanalysisapi;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.google.gson.JsonObject;
import com.pixelcan.emotionanalysisapi.models.FaceAnalysis;
import com.pixelcan.emotionanalysisapi.util.FileUtils;
import com.pixelcan.emotionanalysisapi.util.NetworkUtils;

import java.io.IOException;
import java.net.URISyntaxException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;


/**
 * Created by David Pacioianu on 1/12/16.
 */
public class EmotionRestClient {

    private static final String API_BASE_URL = "https://api.projectoxford.ai/emotion/v1.0/";
    private ApiService apiService = null;
    private String subscriptionKey;
    private Context context;

    private static EmotionRestClient client = null;

    public static EmotionRestClient getInstance() {
        return client;
    }

    public synchronized static void init(Context context, String subscriptionKey){
        if( client == null ){
            client = new EmotionRestClient(context, subscriptionKey);
        }
    }

    private EmotionRestClient(Context context, String subscriptionKey) {
        this.context = context;
        this.subscriptionKey = subscriptionKey;

        // Retrofit setup
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Service setup
        apiService = retrofit.create(ApiService.class);
    }

    public Response<FaceAnalysis[]> detect(String url) throws IOException {
        return createUrlCall(url).execute();
    }

    public void detect(String url, final ResponseCallback callback){
        if (!NetworkUtils.hasInternetConnection(context)){
            callback.onError(context.getString(R.string.no_internet_connection));
            return;
        }

        createUrlCall(url).enqueue(new Callback<FaceAnalysis[]>() {
            @Override
            public void onResponse(Response<FaceAnalysis[]> response) {
                if (response.isSuccess()) {
                    // request successful (status code 200, 201)
                    FaceAnalysis[] result = response.body();
                    if (result != null) {
                        callback.onSuccess(result);
                    } else {
                        callback.onError(response.message());
                    }
                } else {
                    //request not successful (like 400,401,403 etc)
                    try {
                        callback.onError(response.errorBody().string());
                    } catch (IOException e) {
                        callback.onError(response.message());
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private Call<FaceAnalysis[]> createUrlCall(String url){
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("url", url);

        Call<FaceAnalysis[]> call = apiService.analyzePicture(subscriptionKey, requestBody);

        return call;
    }

    public Response<FaceAnalysis[]> detect(Uri uri) throws IOException, URISyntaxException {
        // convert the image to bytes array
        byte[] data = FileUtils.toBinary(uri);

        return detect(data);
    }

    public void detect(Uri uri, final ResponseCallback callback){
        // convert the image to bytes array
        byte[] data;

        try {
            data = FileUtils.toBinary(uri);
        } catch (Exception e) {
            callback.onError(e.getMessage());
            return;
        }

        detect(data,callback);
    }

    public Response<FaceAnalysis[]> detect(Bitmap bitmap) throws IOException {
        byte[] data = FileUtils.toBinary(bitmap);

        return detect(data);
    }

    public void detect(Bitmap bitmap, final ResponseCallback callback){
        byte[] data = FileUtils.toBinary(bitmap);

        detect(data,callback);
    }

    public void detect(byte[] data, final ResponseCallback callback){
        if (!NetworkUtils.hasInternetConnection(context)){
            callback.onError(context.getString(R.string.no_internet_connection));
            return;
        }

        getOctetStreamCall(data).enqueue(new Callback<FaceAnalysis[]>() {
            @Override
            public void onResponse(Response<FaceAnalysis[]> response) {
                if (response.isSuccess()) {
                    // request successful (status code 200, 201)
                    FaceAnalysis[] result = response.body();
                    if (result != null) {
                        callback.onSuccess(result);
                    } else {
                        callback.onError(response.message());
                    }
                } else {
                    //request not successful (like 400,401,403 etc)
                    try {
                        callback.onError(response.errorBody().string());
                    } catch (IOException e) {
                        callback.onError(response.message());
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public Response<FaceAnalysis[]> detect(byte[] data) throws IOException {
        return getOctetStreamCall(data).execute();
    }
    
    private Call<FaceAnalysis[]> getOctetStreamCall(byte[] data){
        RequestBody requestBody = RequestBody
                .create(MediaType.parse("application/octet-stream"), data);

        Call<FaceAnalysis[]> call = apiService.analyzePicture(subscriptionKey, requestBody);

        return call;
    }

}

package com.avatar.personate;


import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.util.Base64;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class FaceRecgUtil {
    private final String TAG = "FaceRecgUtil";

    private final int PIC_TIME_OUT = 5000;
    private final String API_SERVER = "http://api.eyekey.com/face";
    private final String APP_ID = "c48317bd6b1d4b50b3dec5f9c26a9bab";
    private final String APP_KEY = "6656d01ceae14c249ff25d8e16081601";
    private final String ID_KEY = "?app_id=" + APP_ID + "&app_key=" + APP_KEY;
    private final String URL_CHECK = API_SERVER + "/Check/checking";
    private final String URL_MATCH = API_SERVER + "/Match/match_search" + ID_KEY;

    private final String AUTHORITY = "com.avatar.face.common.database.FamilyMembers";
    private final String USERINFO_TABLE = "UserInfo";
    private final String PEOPLE_TABLE = "People";
    private final Uri USER_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + USERINFO_TABLE);
    private final Uri PEOPLE_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PEOPLE_TABLE);

    private String mFaceGatherName;
    private ContentResolver mResolver;

    public FaceRecgUtil(Context context) {
        mResolver = context.getContentResolver();
        mFaceGatherName = getUserInfo();
        Util.Logd(TAG, "FaceGatherName=" + mFaceGatherName);
    }


    public String decodeFaceID(final byte[] yuvdata, int widht, int height) {
        YuvImage img = new YuvImage(yuvdata, ImageFormat.NV21, widht, height, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream(yuvdata.length);
        if (!img.compressToJpeg(new Rect(0,0,widht,height), 100, os)) {
            return null;
        }

        byte[] tmp = os.toByteArray();
        Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
        Bitmap bitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(),
                new Matrix(), true);
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bStream);
        String imgData = Base64.encodeToString(bStream.toByteArray(), 0);

        return decode(imgData, widht, height);
    }

    public String recognize(String faceID) {
        if (mFaceGatherName == null) return null;

        String url = URL_MATCH + "&face_id=" + faceID + "&facegather_name=" + mFaceGatherName;
        try {
            HttpGet httpGet = new HttpGet(url);
            String result = queryByHttp(httpGet);
            if (result != null) {
                //Util.Logd(TAG, "HttpResponse:" + result);
                JSONObject jsonObject = new JSONObject(result);
                String resultCode = jsonObject.getString("res_code");
                if (resultCode.equals("0000")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("result");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject item = jsonArray.getJSONObject(i);
                        if (80.0 <= item.getDouble("similarity")) {
                            String name = item.getString("people_name");
                            //Util.Logd(TAG, "Name UUID:" + name);
                            return getPersonByUUID(name);
                        }
                    }
                }
            }
        } catch (JSONException e) {

        }
        return null;
    }

    private String getPersonByUUID(String uuid) {
        Cursor cursor = mResolver.query(PEOPLE_CONTENT_URI,
                new String[]{BaseColumns._ID, "name"},
                "uuid='" + uuid + "'", null, BaseColumns._ID);
        String name = null;
        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex("name");
            name = cursor.getString(nameColumnIndex);
        }
        cursor.close();
        return name;
    }

    private String decode(String faceData, int width, int height) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("app_id", APP_ID));
        params.add(new BasicNameValuePair("app_key", APP_KEY));
        params.add(new BasicNameValuePair("img", faceData));
        try {
            HttpPost httpPost = new HttpPost(URL_CHECK);
            httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            String json = queryByHttp(httpPost);
            if (json != null) {
                //Util.Logd(TAG, "HttpResponse:" + json);
                JSONObject jsonObject = new JSONObject(json);
                String resultCode = jsonObject.getString("res_code");
                if (resultCode.equals("0000")) {
                    JSONArray face = jsonObject.getJSONArray("face");
                    return face.getJSONObject(0).getString("face_id");
                }
            }
        } catch (UnsupportedEncodingException e) {
            // ignore
        } catch (JSONException e) {
            Util.Logd(TAG, "JSONException:" + e.getMessage());
        }

        return null;
    }

    private String queryByHttp(HttpRequestBase http) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), PIC_TIME_OUT);
        HttpConnectionParams.setSoTimeout(httpClient.getParams(), PIC_TIME_OUT);

        try {
            HttpResponse httpResponse = httpClient.execute(http);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return EntityUtils.toString(httpResponse.getEntity());
            }
        } catch (IOException e) {
            Util.Logd(TAG, "IOException:" + e.getMessage());
        }
        return null;
    }

    private String getUserInfo() {
        Cursor cursor = mResolver.query(USER_CONTENT_URI, null,
                "name='FaceGatherName'", null, BaseColumns._ID);
        if (cursor == null) {
            return null;
        }

        String value = null;
        if (cursor.moveToFirst()) {
            int valueColumnIndex = cursor.getColumnIndex("value");
            value = cursor.getString(valueColumnIndex);
        }
        cursor.close();
        return value;
    }


}

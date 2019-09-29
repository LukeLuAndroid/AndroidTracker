package com.sdk.tracker.callback;

import com.sdk.tracker.TrackerResultListener;
import com.sdk.tracker.TrackerUploadListener;
import com.sdk.tracker.core.ThreadPool;
import com.sdk.tracker.log.TraceLog;
import com.sdk.tracker.util.TrackerUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 默认实现上传埋点信息
 */
public class DefaultUploadListenerImpl implements TrackerUploadListener {

    private String requestUrl;

    public DefaultUploadListenerImpl(String url) {
        this.requestUrl = url;
    }

    @Override
    public void uploadLogInfo(final List<TraceLog> list, final TrackerResultListener listener) {
        if (requestUrl == null || "".equals(requestUrl)) {
            return;
        }
        ThreadPool.submitSingleTask(new Runnable() {
            @Override
            public void run() {
                String json = TrackerUtils.getTraceLogJson(list);

                Map<String, String> params = new HashMap<>();
                params.put("json", json);
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");

                post(requestUrl, params, headers, new CallBack() {
                    @Override
                    public void onResponse(final HttpResponse response, final int code, final String message) {
                        try {
                            if (code == 200) {
                                JSONObject object = new JSONObject(message);
                                if (listener != null) {
                                    listener.onSuccess();
                                }
                            } else {
                                if (listener != null) {
                                    listener.onFail();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void post(String url, Map<String, String> params, Map<String, String> headers, CallBack callBack) {
        try {
            List<NameValuePair> formparams = new ArrayList<>();
            if (params != null) {
                Iterator<Map.Entry<String, String>> paramIterator = params.entrySet().iterator();
                while (paramIterator.hasNext()) {
                    Map.Entry<String, String> entry = paramIterator.next();
                    formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
            }
            HttpEntity reqEntity = new UrlEncodedFormEntity(formparams, "utf-8");

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(url);
            if (headers != null) {
                Set<Map.Entry<String, String>> headerSet = headers.entrySet();
                for (Map.Entry<String, String> entry : headerSet) {
                    post.addHeader(entry.getKey(), entry.getValue());
                }
            }
            post.setEntity(reqEntity);
            HttpResponse response = client.execute(post);
            int responseCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity resEntity = response.getEntity();
                String message = EntityUtils.toString(resEntity, "utf-8");
                if (callBack != null) {
                    callBack.onResponse(response, responseCode, message);
                }
            } else {
                if (callBack != null) {
                    callBack.onResponse(response, responseCode, "request fail");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface CallBack {
        void onResponse(HttpResponse response, int code, String message);
    }
}

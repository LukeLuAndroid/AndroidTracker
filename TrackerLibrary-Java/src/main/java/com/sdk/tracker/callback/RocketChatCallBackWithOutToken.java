package com.sdk.tracker.callback;

import android.os.Looper;

import com.sdk.tracker.TrackerAddListener;
import com.sdk.tracker.Trackers;
import com.sdk.tracker.core.ThreadPool;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 自定义埋点上传到rocket
 * 不需要上传token信息
 */
public class RocketChatCallBackWithOutToken implements TrackerAddListener {

    @Override
    public boolean addTracker(String className, String methodName, String group, String tag, Object... args) {
        if (Trackers.instance().isDebug()) {
            sendMessageToRocketChat(className, methodName, args);
            return true;
        }
        return false;
    }

    //发送消息到rocket.chat
    private void sendMessageToRocketChat(String className, String methodName, Object[] args) {
        String message = getMethodInfo(className, methodName, args);
        postMessage(message);
    }

    //获取消息的日志
    String getMethodInfo(String methodClass, String methodName, Object[] args) {
        StringBuilder buffer = new StringBuilder(methodClass);
        buffer.append(".").append(methodName).append("(");
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i != 0) {
                    buffer.append(",");
                }
                if (args[i] != null) {
                    buffer.append(args[i].getClass().getName());
                } else {
                    buffer.append("null");
                }
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

    protected void postMessage(String msg) {
        Map<String, String> params = new HashMap<>();
        params.put("text", msg);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        requestOnThread("http://appguuidea.8866.org:3000/hooks/dwoy8MpeJa7w3RwqD/tnfNbyLRJyKoGqDKDugPydSzG3AtGginLyn6WS3YFSHSneHX", params, headers, new CallBack() {
            @Override
            public void onResponse(HttpResponse response, int code, String message) {
                System.out.println(message);
            }
        });
    }

    //线程加载net请求
    public void requestOnThread(final String url, final Map<String, String> params, final Map<String, String> headers, final CallBack callBack) {
        ThreadPool.submitSingleTask(new Runnable() {
            @Override
            public void run() {
                post(url, params, headers, new CallBack() {
                    @Override
                    public void onResponse(final HttpResponse response, final int code, final String message) {
                        if (callBack != null) {
                            new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    callBack.onResponse(response, code, message);
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void post(String url, Map<String, String> params, Map<String, String> headers, CallBack callBack) {
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

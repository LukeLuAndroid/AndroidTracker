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
 * 自定义实现上传打点到rocket
 * 需要传用户名密码，发给自己
 */
public class RocketChatCallBack implements TrackerAddListener {
    private static final String CHANNEL = "#tracker-channel";
    private String userName;
    private String password;
    private String mAuthToken;
    private String mUserId;
    private String mRoomId;

    public RocketChatCallBack(String userName, String password) {
        this.userName = userName;
        this.password = password;
        if (Trackers.instance().isDebug()) {
            login();
        }
    }

    @Override
    public boolean addTracker(String className, String methodName, String group, String tag, Object... args) {
        if (Trackers.instance().isDebug()) {
            if (mAuthToken == null || mUserId == null) {
                login();
            } else {
                sendMessageToRocketChat(className, methodName, args);
            }
            return true;
        }
        return false;
    }

    //发送消息到rocketchat
    private void sendMessageToRocketChat(String className, String methodName, Object[] args) {
        String message = getMethodInfo(className, methodName, args);
        postMessage(mUserId + mUserId, message);
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

    private boolean isLoading;

    protected void login() {
        if (isLoading) {
            return;
        }
        if (mAuthToken == null || mUserId == null) {
            isLoading = true;

            Map<String, String> params = new HashMap<>();
            params.put("user", userName);
            params.put("password", password);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            requestOnThread("http://appguuidea.8866.org:3000/api/v1/login", params, headers, new CallBack() {
                @Override
                public void onResponse(HttpResponse response, int code, String message) {
                    isLoading = false;
                    try {
                        JSONObject object = new JSONObject(message);
                        JSONObject data = object.optJSONObject("data");
                        if (data != null) {
                            mUserId = data.optString("userId", "");
                            mAuthToken = data.optString("authToken", "");
                        }
                        System.out.println(message);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void createRoomToMe() {
        if (mAuthToken != null && mUserId != null) {
            Map<String, String> params = new HashMap<>();
            params.put("username", userName);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("X-Auth-Token", mAuthToken);
            headers.put("X-User-Id", mUserId);
            requestOnThread("http://appguuidea.8866.org:3000/api/v1/im.create", params, headers, new CallBack() {
                @Override
                public void onResponse(HttpResponse response, int code, String message) {
                    try {
                        JSONObject object = new JSONObject(message);
                        JSONObject room = object.optJSONObject("room");
                        if (room != null) {
                            mRoomId = room.optString("_id", "");
                        }
                        System.out.println(message);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    protected void postMessage(String channel, String msg) {
        if (mAuthToken != null && mUserId != null) {
            Map<String, String> params = new HashMap<>();
            params.put("channel", channel);
            params.put("text", msg);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("X-Auth-Token", mAuthToken);
            headers.put("X-User-Id", mUserId);
            requestOnThread("http://appguuidea.8866.org:3000/api/v1/chat.postMessage", params, headers, new CallBack() {
                @Override
                public void onResponse(HttpResponse response, int code, String message) {
                    System.out.println(message);
                }
            });
        }
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

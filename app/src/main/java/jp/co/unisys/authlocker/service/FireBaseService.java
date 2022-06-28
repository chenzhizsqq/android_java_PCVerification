package jp.co.unisys.authlocker.service;


import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import jp.co.unisys.authlocker.activity.HomeActivity;
import jp.co.unisys.authlocker.application.PCVApplication;
import jp.co.unisys.authlocker.config.Config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class FireBaseService extends FirebaseMessagingService {


    public static void getId(Activity activity){

        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("FCMDemo", "getInstanceId failed", task.getException());
                        return;
                    }
                    // Get new Instance ID token
                    String token = task.getResult().getToken();
                    ((PCVApplication) activity.getApplication()).saveNotifyToken(token);
                    Log.e("FCMDemo", "token: " + token);
                });


    }

    private final String TAG = "FCMDemo";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // 当APP未被kill时，推送消息在这里进行处理
        Log.d(TAG, "From: " + remoteMessage.getFrom());


        //yangw
        Intent intent = new Intent();
        intent.setAction(Config.Key.KEY_NOTIFY_DATA);
        intent.setClass(this, HomeActivity.PushBroadcast.class);

        Map<String, String> message = remoteMessage.getData();
        Map<String, String> data = new HashMap<String, String>();
        for(String key : message.keySet()){
            String value = message.get(key);
            data.put(key,value);
        }

        if (data.size() > 0) {
            intent.putExtra(Config.Key.KEY_NOTIFY_DATA,(Serializable)data);
        }
        if (remoteMessage.getNotification() != null) {
            intent.putExtra(Config.Key.KEY_NOTIFY_TITLE, remoteMessage.getNotification().getTitle());
            intent.putExtra(Config.Key.KEY_NOTIFY_BODY, remoteMessage.getNotification().getBody());
        }
        sendBroadcast(intent);
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        // 当Token发生改变时，通过这个函数获取最新的Token
        Log.d(TAG, "new Token: " + s);
        ((PCVApplication) getApplication()).saveNotifyToken(s);
    }

}

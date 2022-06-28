package jp.co.unisys.authlocker.activity;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.gson.Gson;
import com.neusoft.convenient.activity.ConvenientActivity;
import com.neusoft.convenient.network.http.ConvenientHttpConnection;
import com.neusoft.convenient.network.http.listener.OnHttpConnectionListener;
import com.neusoft.convenient.view.ContentView;
import com.neusoft.pcverification.R;
import com.neusoft.pcverification.databinding.ActivityHomeBinding;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import jp.co.unisys.authlocker.application.InComingCall;
import jp.co.unisys.authlocker.application.LifecycleHandler;
import jp.co.unisys.authlocker.application.LockingTimer;
import jp.co.unisys.authlocker.application.MainActivity;
import jp.co.unisys.authlocker.application.PCVApplication;
import jp.co.unisys.authlocker.config.Config;
import jp.co.unisys.authlocker.db.DBUtils;
import jp.co.unisys.authlocker.db.model.AuthPCModel;
import jp.co.unisys.authlocker.response.GetDataResponse;
import jp.co.unisys.authlocker.response.GetTokenResponse;
import jp.co.unisys.authlocker.response.StatusFlagData;
import jp.co.unisys.authlocker.service.BluetoothService;
import jp.co.unisys.authlocker.util.sweetalert.SweetAlertDialog;

import static jp.co.unisys.authlocker.application.LockingTimer.timer;
import static jp.co.unisys.authlocker.config.Config.AppForgroundFlag;
import static jp.co.unisys.authlocker.config.Config.TimeOutFlag;
import static jp.co.unisys.authlocker.config.Config.VersionName;

@ContentView(R.layout.activity_home)
public class HomeActivity extends ConvenientActivity<ActivityHomeBinding> {


    //アプリ起動認証
    private static final int AUTHENTICATION_REQUEST_CODE = 2;
    //アプリ起動認証設定
    private static final int SETTING_REQUEST_CODE = 3;

    //ローカル状態
    public static final int STATUS_LOCAL = -1;
    //no bind
    public static final int STATUS_NOT_BIND = 0;
    //初期表示/未認証
    public static final int STATUS_NOT_LOGIN = 1;
    //Bluetooth接続中
    public static final int STATUS_BLUETOOTH = 2;
    //利用停止
    public static final int STATUS_NOT_USE = 3;

    private boolean mIsGetData = true;

    private boolean mIsSettingBack = false;

    private int mStatus = STATUS_NOT_LOGIN;

    private KeyguardManager mKeyguardManager;

    private static SweetAlertDialog mDialog;

    private static SweetAlertDialog mLoadingDialog;

    private String mToken = "";

    private String mMessageCode = "";

    private String mBluetoothFlg = "";

    private String mPwdCheckFlg = "";

    private String mBluetoothFlgOld = "";

    private String mPwdCheckFlgOld = "";

    private boolean mIsPush = false;

    private PushBroadcast mPushReceiver = new PushBroadcast();

    private  BroadcastReceiver incomingcallreceiver;
//    private BluetoothBindBroadcast mBluetoothBindOverReceiver = new BluetoothBindBroadcast();

    private ImageView mStatusIcon;
    private TextView mStatusTxt;
    private NotificationManager mNotificationManager;
    private Handler handler = new Handler();
    private LifecycleHandler lifecycleHandler;

    private Executor executor = new Executor() {
        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView VersionView =(TextView)findViewById(R.id.VersionText);

        VersionView.setText("ver "+VersionName);

        initView();
        initChannel();
        PCVApplication.getInstance().setHomeActivity(this);

        lifecycleHandler=new LifecycleHandler();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleHandler());

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(incomingcallreceiver);
        incomingcallreceiver=null;
        Intent intent = new Intent(HomeActivity.this,BluetoothService.class);
        stopService(intent);
        Log.d("HomeActivity","BluetoothService close");
    }


    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getExtras()!= null) {
                Bundle bundle = intent.getExtras();
                Map<String, String> messageData = new HashMap<String, String>();
                for (String key: bundle.keySet()) {
                    messageData.put(key, bundle.getString(key));
                }
                if(messageData.containsKey(Config.PushKey.STARTUP_FLAG) || messageData.containsKey(Config.PushKey.USER_ID) ||
                        messageData.containsKey(Config.PushKey.USER_STATUS) || messageData.containsKey(Config.PushKey.PCUUID) ||
                        messageData.containsKey(Config.PushKey.SMARTPHONE_STATUS)) {
                    updatePushData(messageData);

                    //認証ユーザ削除以外
                    if (!(messageData.containsKey(Config.PushKey.USER_ID) && messageData.containsKey(Config.PushKey.USER_STATUS)
                            && Config.Status.DELETE.equals(messageData.get(Config.PushKey.USER_STATUS))
                            && ((PCVApplication) getApplication()).getUserId().equals(messageData.get(Config.PushKey.USER_ID)))) {
                        mIsPush = true;
                        getData();
                    }
                }
            }
        }
        if(incomingcallreceiver==null){
        incomingcallreceiver=new InComingCall();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PHONE_STAT");
        registerReceiver(incomingcallreceiver,filter);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String title = prefs.getString("title", null);
        String message = prefs.getString("message", null);
        if(title!=null||message!=null){
            AppForgroundFlag = true;
            showMsg(title,message,null);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("title");
            editor.remove("message");
            editor.commit();
        }
    }

    @Override
    public void onRestart(){
        super.onRestart();
        //アプリ起動認証:有効
        if (Config.FlagStatus.PWD_CHECK_FLG_DO.equals(((PCVApplication) getApplication()).getPwdCheckFlg())) {
            if (!Config.FlagStatus.BLUETOOTH_FLG_DONT.equals(((PCVApplication) getApplication()).getBluetoothFlg())) {
                long timeNow = new Date().getTime();
                if (timeNow - PCVApplication.getInstance().getUnlockTime() > 180 * 1000) {
                    Log.d("timer", String.valueOf(timeNow) + "ms");
//                    TimeOutFlag=true;
//                    PCVApplication.getInstance().getHomeActivity().refreshStatus(1);
                    //タイマー停止
                    if(timer!=null) {
                        LockingTimer lockingTimer = new LockingTimer();
                        lockingTimer.stopTimer();
                        lockingTimer.setTimer();
                    }
                }
            }
            //アプリ起動認証:無効
        }else{

        }
    }

    private void initView() {

        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        mStatusIcon = findViewById(R.id.status_icon);
        mStatusTxt = findViewById(R.id.status_txt);

        mStatusIcon.setOnClickListener(OnClickListener -> {
            doAuthentication();
//            bluetoothNotUse();
        });

        refreshStatus(STATUS_NOT_LOGIN);

        Intent intent = getIntent();

        int status = intent.getIntExtra("status",-1);
        if(status == STATUS_NOT_USE) {
            refreshStatus(STATUS_NOT_USE);
            String message = intent.getStringExtra("message");
            if(message != null && !message.isEmpty()) {
                showMsg("",message, null);
                return;
            }
        } else if (status == STATUS_LOCAL){
            getDataEnd();
        }

    }


    private void initChannel() {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "statusNotify";
            String channelName = getString(R.string.notify_channel);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //アプリ起動認証
        if (requestCode == AUTHENTICATION_REQUEST_CODE) {
            // Challenge completed, proceed with using cipher
            if (resultCode == RESULT_OK) {
                //認証成功
                if (mStatus == STATUS_NOT_LOGIN) {
                    //Bluetoothの設定
                    checkBluetoothFlag();
                    PCVApplication.getInstance().setUnlockTime(new Date().getTime());
                }
            } else {
                //認証失敗

            }
        }
        //アプリ起動認証設定
        if (requestCode == SETTING_REQUEST_CODE) {
            Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
            //スマホシステムのアプリ起動認証が未設定
            if (intent == null) {

                //スマホシステムのアプリ起動認証が設定済み
            } else {
                if (mStatus == STATUS_NOT_LOGIN) {
                    //アプリ起動認証画面へ遷移する。
                    showAuthenticationScreen();
                }
            }

        }
    }

    public boolean translucentStatusBar() {
        return true;
    }


    /**
     * アプリ起動認証 check
     */
    private void checkAuthentication() {
        //アプリ起動認証flag 「有効」
        if (Config.FlagStatus.PWD_CHECK_FLG_DO.equals(((PCVApplication) getApplication()).getPwdCheckFlg())) {
            Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
            //スマホシステムのアプリ起動認証が未設定
            if (intent == null) {
                toPwdSetting();
                //スマホシステムのアプリ起動認証が設定済み
            } else {
                showAuthenticationScreen();
            }
            //アプリ起動認証flag 「無効」
        } else {
            checkBluetoothFlag();
        }
    }

    /**
     * 利用停止 check
     */
    private void checkBluetoothFlag() {
        if (Config.FlagStatus.BLUETOOTH_FLG_DO.equals(((PCVApplication) getApplication()).getBluetoothFlg())) {
            bluetoothConnect();
        } else {
            bluetoothNotUse();
        }

    }

    /**
     * アプリ起動認証
     */
    private void doAuthentication() {
        if (!mKeyguardManager.isKeyguardSecure()) {
            toPwdSetting();
        } else {
            showAuthenticationScreen();
        }

    }

    /**
     * アプリ起動認証設定画面
     */
    private void toPwdSetting() {
        SweetAlertDialog.OnSweetClickListener listener = sweetAlertDialog -> {
            mIsSettingBack = true;
            Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            startActivityForResult(intent, SETTING_REQUEST_CODE);
            sweetAlertDialog.dismiss();
        };
        showMsg(getString(R.string.msg_pwd_setting_title), getString(R.string.msg_pwd_setting), listener);

    }

    /**
     * アプリ起動認証画面
     */
    private void showAuthenticationScreen() {
        BiometricManager biometricManager = BiometricManager.from(HomeActivity.this);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d("1","アプリは生体認証を使用して認証できます");
                showBiometricPrompt();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.e("1","このデバイスで利用できる生体認証機能はありません");
                Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
                if (intent != null) {
                    mIsSettingBack = true;
                    startActivityForResult(intent, AUTHENTICATION_REQUEST_CODE);
                }
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e("1","生体認証機能は利用できません");
                intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
                if (intent != null) {
                    mIsSettingBack = true;
                    startActivityForResult(intent, AUTHENTICATION_REQUEST_CODE);
                }
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.e("1","ユーザーはバイオメトリック認証情報を関連付けていません");
                showBiometricPrompt();
                break;
        }
    }

    /**
     * 画面の表示
     *
     * @param status
     */
    public void refreshStatus(int status) {
        mStatus = status;
        //初期表示/未認証
        if (STATUS_NOT_LOGIN == status) {
            mStatusTxt.setText(R.string.lock_not_login);
            mStatusIcon.setImageDrawable(getDrawable(R.drawable.key_close_text_blue));
            mStatusIcon.setClickable(true);
            ((PCVApplication) getApplication()).setIsBlueToothCanConnect(Config.FlagStatus.BLUETOOTH_FLG_DONT);
            //Bluetooth接続中
        } else if (STATUS_BLUETOOTH == status) {
            mStatusTxt.setText(R.string.lock_bluetooth);
            mStatusIcon.setImageDrawable(getDrawable(R.drawable.key_open_text));
            mStatusIcon.setClickable(false);
            ((PCVApplication) getApplication()).setIsBlueToothCanConnect(Config.FlagStatus.BLUETOOTH_FLG_DO);
            PCVApplication.getInstance().setUnlockTime(new Date().getTime());
            TimeOutFlag = false;
            //テナントオプション無効かのIf分
            if (Config.FlagStatus.PWD_CHECK_FLG_DO.equals(((PCVApplication) getApplication()).getPwdCheckFlg())) {
                LockingTimer timer = new LockingTimer();
                timer.setTimer();
            }
            //利用停止
        } else if (STATUS_NOT_USE == status) {
            mStatusTxt.setText(R.string.lock_not_use);
            mStatusIcon.setImageDrawable(getDrawable(R.drawable.key_close_text));
            mStatusIcon.setClickable(false);
            ((PCVApplication) getApplication()).setIsBlueToothCanConnect(Config.FlagStatus.BLUETOOTH_FLG_DONT);
        } else if (STATUS_NOT_BIND == status) {
            mStatusTxt.setText(R.string.lock_not_bind);
            mStatusIcon.setImageDrawable(getDrawable(R.drawable.key_close_text_blue));
            mStatusIcon.setClickable(false);
            ((PCVApplication) getApplication()).setIsBlueToothCanConnect(Config.FlagStatus.BLUETOOTH_FLG_DO);
        }

    }

    public boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    private void getData() {
        getToken();
    }

    private void getToken() {
        if(!isFinishing()) {
            showLoadingMsg("", getString(R.string.loading));
        }
        new Thread(() -> {
            mToken = "";

            ConvenientHttpConnection httpConnection = new ConvenientHttpConnection();
            Map<String, String> headerParams = new HashMap<>();
            Map<String, String> body = new HashMap<>();

            headerParams.put("Content-Type", "application/json");
            //TODO params
            body.put(Config.Key.KEY_USERID2, ((PCVApplication) getApplication()).getUserId());
            body.put(Config.Key.KEY_PHONEKEY2,  ((PCVApplication) getApplication()).getNotifyToken());
            String serverUrl = PCVApplication.getInstance().getUrl();
            if (serverUrl.equals("")) {
                dismissLoadingDialog();
                return;
            }
            //JsonRequest
            httpConnection.sendHttpPost(serverUrl + Config.URL.ADDR_GET_TOKEN, headerParams, null, body, new OnHttpConnectionListener() {
                @Override
                public void onSuccess(String s) {
                    try {
                        GetTokenResponse data = new Gson().fromJson(s, GetTokenResponse.class);
                        //Responseがnullではないとき
                        if (data != null) {
                            //レスポンスデータ-code
                            mMessageCode = data.getMessageCode();
                            //data.getCode()が0の場合
                            if ("0".equals(data.getCode())) {
                                mToken = data.getToken();
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(int i) {
                    dismissLoadingDialog();
                }
                @Override
                public void onTokenFailure() {
                }

                @Override
                public void onEnd() {
                    //
                    if (mToken == null || mToken.isEmpty()) {
                        getDataEnd();
                    } else {
                        getFlag();
                    }
                }
            });
        }).start();

    }

    private void getDataEnd() {
        dismissLoadingDialog();
        (HomeActivity.this).runOnUiThread(() -> {
            //messageCode
            if (!mMessageCode.equals("PCWZ00C15")){
                switch (mMessageCode) {
                    case "PCWZ00C14" :
                        runOnUiThread(() -> {
                            refreshStatus(STATUS_NOT_USE);
                            showMsg("", getString(R.string.msg_not_use_title), null);
                        });
                        return;
                    case "PCWP05C01" :
                        runOnUiThread(() -> {
                            refreshStatus(STATUS_NOT_USE);
                            showMsg("", getString(R.string.msg_tenant_not_login), null);
                        });
                        return;
                    case "PCWZ00C16" :
                        Intent intent = new Intent(HomeActivity.this,MainActivity.class);
                        intent.putExtra("message",getString(R.string.msg_get_information_failed_continue));
                        intent.putExtra("jumpFlag", true);
                        startActivity(intent);
                        finish();
                        return;
                    case "PCWZ00C17" :
                    case "PCWZ00C18" :
                        intent = new Intent(HomeActivity.this,MainActivity.class);
                        intent.putExtra("message",getString(R.string.msg_get_information_failed_close));
                        intent.putExtra("jumpFlag", false);
                        startActivity(intent);
                        finish();
                        return;
                    case "PCWZ00C30" :
                        intent = new Intent(HomeActivity.this,MainActivity.class);
                        intent.putExtra("message",getString(R.string.msg_get_information_failed_continue));
                        intent.putExtra("jumpFlag", true);
                        startActivity(intent);
                        finish();
                        return;
                }
            }


            if (!mIsPush) {
                setView();
            } else {
                mBluetoothFlg = ((PCVApplication) getApplication()).getBluetoothFlg();
                mPwdCheckFlg = ((PCVApplication) getApplication()).getPwdCheckFlg();
                //受信した利用設定情報が変更しない場合、処理終了
//                if ((mBluetoothFlgOld.equals(mBluetoothFlg)) && (mPwdCheckFlgOld.equals(mPwdCheckFlg))) {
//                    return;
//                }

                //受信した利用設定情報が変更した場合、利用設定情報をローカルに更新する。
                pushAction(mBluetoothFlgOld, mPwdCheckFlgOld, mBluetoothFlg, mPwdCheckFlg);
                mIsPush = false;
            }
        });
    }

    private void setView() {
            //1の時、利用停止
        if (Config.FlagStatus.BLUETOOTH_FLG_DONT.equals(((PCVApplication) getApplication()).getBluetoothFlg())) {
            bluetoothNotUse();
            //0の時、スマホ認証無効
        } else if (Config.FlagStatus.PWD_CHECK_FLG_DONT.equals(((PCVApplication) getApplication()).getPwdCheckFlg()) &&
                Config.FlagStatus.BLUETOOTH_FLG_DO.equals(((PCVApplication) getApplication()).getBluetoothFlg())) {
            bluetoothConnect();
            //初期表示画面、未認証
        } else if (Config.FlagStatus.PWD_CHECK_FLG_DO.equals(((PCVApplication) getApplication()).getPwdCheckFlg()) &&
                Config.FlagStatus.BLUETOOTH_FLG_DO.equals(((PCVApplication) getApplication()).getBluetoothFlg())) {
            refreshStatus(STATUS_NOT_LOGIN);
            //PCアプリ設定前
        } else if (!DBUtils.isHaveData()){
            refreshStatus(STATUS_NOT_BIND);
        }
    }

    private void getFlag() {
        new Thread(() -> {
            ConvenientHttpConnection httpConnection = new ConvenientHttpConnection();
            Map<String, String> headerParams = new HashMap<>();
            Map<String, String> body = new HashMap<>();

            body.put(Config.Key.KEY_USERID2, ((PCVApplication) getApplication()).getUserId());
            body.put(Config.Key.KEY_PHONEKEY, ((PCVApplication) getApplication()).getNotifyToken());
//          body.put(Config.Key.KEY_PHONEKEY, BluetoothUtils.getImeiUnlockUUID(HomeActivity.this).toString());
            body.put(Config.Key.KEY_TOKEN, mToken);

            headerParams.put("Content-Type", "application/json");

            String serverUrl = PCVApplication.getInstance().getUrl();
            if (serverUrl.equals("")) {
                dismissLoadingDialog();
                return;
            }
            //JsonResponse
            httpConnection.sendHttpPost(serverUrl + Config.URL.ADDR_GET_DATA, headerParams, null, body, new OnHttpConnectionListener() {
                @Override
                public void onSuccess(String s) {
                    try {
                        GetDataResponse data = new Gson().fromJson(s, GetDataResponse.class);
                        //Responseがnullではない時
                        if (data != null) {
                            //レスポンスデータ-code
                            mMessageCode = data.getCode();
                            if ("0".equals(data.getCode())) {
                                ((PCVApplication) getApplication()).saveFlagInfo(data.getStartupflag(), data.getUsabilityflag());
                                //有効性フラグ
                                DBUtils.refreshAuthPC(data.getSmartphoneList());
                            }
                        }

//                        mMessageCode = "PCWZ00C17";
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(int i) {

                }

                @Override
                public void onTokenFailure() {
                }

                @Override
                public void onEnd() {
                    (HomeActivity.this).runOnUiThread(() -> {
                        getDataEnd();
                    });

                }
            });
        }).start();

    }

    /**
     * 利用停止
     */
    private void bluetoothNotUse() {
        if (Config.FlagStatus.BLUETOOTH_FLG_DO.equals(mBluetoothFlgOld)){
            showMsg("",getString(R.string.msg_not_use_title), null);
        }
        refreshStatus(STATUS_NOT_USE);
        stopBluetooth();
    }

    /**
     * 利用停止解除
     */
    private void bluetoothConnect() {
        refreshStatus(STATUS_BLUETOOTH);
        startBluetooth();
    }

    /**
     * Bluetooth切断
     */
    private void stopBluetooth() {
        ((PCVApplication) getApplication()).setIsBlueToothCanConnect(Config.FlagStatus.BLUETOOTH_FLG_DONT);

//        stopService(new Intent(HomeActivity.this,BluetoothService.class));
    }

    /**
     * Bluetooth接続
     */
    private void startBluetooth() {
        ((PCVApplication) getApplication()).setIsBlueToothCanConnect(Config.FlagStatus.BLUETOOTH_FLG_DO);

//        // 蓝牙没有开启
//        if (!mBluetoothAdapter.isEnabled()) {
//            mIsSettingBack = true;
//            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(intent, ACTION_BLUETOOTH_ENABLE_REQUEST_CODE);
//        }
//        // 蓝牙已经开启
//        else {
//            bluetoothDeviceVisible();
//        }
//        }

    }

    public void showMsg(String title, String message, SweetAlertDialog.OnSweetClickListener listener) {
        if(AppForgroundFlag==true) {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        mDialog = new SweetAlertDialog(HomeActivity.this, SweetAlertDialog.NORMAL_TYPE);
        mDialog.setTitleText(message);
        mDialog.setTitleText(title);
        mDialog.setContentText(message);
        mDialog.showCancelButton(false);
        mDialog.setCanceledOnTouchOutside(true);
        if (listener != null) {
            mDialog.setConfirmClickListener(listener);
        } else {
            mDialog.setConfirmClickListener(sweetAlertDialog -> {
                mDialog.dismiss();
            });
        }
        mDialog.show();
        }else{
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("title", title);
            editor.putString("message", message);
            editor.apply();
        }
    }

    public void showLoadingMsg(String title, String message) {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
        mLoadingDialog = new SweetAlertDialog(HomeActivity.this, SweetAlertDialog.PROGRESS_TYPE);
        mLoadingDialog.setTitleText(title);
        mLoadingDialog.setContentText(message);
        mLoadingDialog.setConfirmText(HomeActivity.this.getString(R.string.dialog_ok));
        mLoadingDialog.showCancelButton(false);
//        mDialog.setCanceledOnTouchOutside(false);
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    //(プッシュ通知受信
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.w("yangw","onNewIntent");
//        Log.w("yangw",intent.getExtras().toString());
        if (intent != null) {
            if (intent.getExtras()!= null) {
                mBluetoothFlgOld = ((PCVApplication) getApplication()).getBluetoothFlg();
                mPwdCheckFlgOld = ((PCVApplication) getApplication()).getPwdCheckFlg();

                Bundle bundle = intent.getExtras();
                Map<String, String> messageData = new HashMap<String, String>();
                for (String key: bundle.keySet()) {
                    messageData.put(key, bundle.getString(key));
                }
                updatePushData(messageData);

                //認証ユーザ削除以外
                if (!(messageData.containsKey(Config.PushKey.USER_ID) && messageData.containsKey(Config.PushKey.USER_STATUS)
                        && Config.Status.DELETE.equals(messageData.get(Config.PushKey.USER_STATUS))
                        && ((PCVApplication) getApplication()).getUserId().equals(messageData.get(Config.PushKey.USER_ID)))) {
                    mIsPush = true;
                    getData();
                }

            }
        }

    }

    /**
     * プッシュ通知情報の受信処理
     *
     * @param bluetoothFlgOld
     * @param pwdCheckFlgOld
     * @param bluetoothFlg
     * @param pwdCheckFlg
     */
    public void pushAction(String bluetoothFlgOld, String pwdCheckFlgOld, String bluetoothFlg, String pwdCheckFlg) {
        switch (mStatus) {
            //初期表示/未認証
            case STATUS_NOT_LOGIN:
                //利用停止 「停止」(B.D)
                if (Config.FlagStatus.BLUETOOTH_FLG_DONT.equals(bluetoothFlg)) {
//                    showMsg(getString(R.string.msg_not_use_title), getString(R.string.msg_not_use_content), null);
                    bluetoothNotUse();
                    //利用停止 「停止解除」+ アプリ起動認証flag 「無効」(C)
                } else if ((Config.FlagStatus.BLUETOOTH_FLG_DO.equals(bluetoothFlg)) && (Config.FlagStatus.PWD_CHECK_FLG_DONT.equals(pwdCheckFlg))) {
                    bluetoothConnect();
                }
                break;
            //Bluetooth接続中
            case STATUS_BLUETOOTH:
                //利用停止 「停止」(B.D)
                if (Config.FlagStatus.BLUETOOTH_FLG_DONT.equals(bluetoothFlg)) {
//                    showMsg(getString(R.string.msg_not_use_title), getString(R.string.msg_not_use_content), null);
                    bluetoothNotUse();
                    //利用停止 「停止解除」+ アプリ起動認証flag 「無効」
                    // ==> 利用停止 「停止解除」+ アプリ起動認証flag 「有効」(C==>A)
                } else if ((Config.FlagStatus.BLUETOOTH_FLG_DO.equals(bluetoothFlg)) && (Config.FlagStatus.PWD_CHECK_FLG_DO.equals(pwdCheckFlg))) {
                    stopBluetooth();
//                    checkAuthentication();
                     setView();
                }
                break;
            //利用停止
            case STATUS_NOT_USE:
                //利用停止 「停止」(B.D)
                if (Config.FlagStatus.BLUETOOTH_FLG_DO.equals(bluetoothFlg)) {
                    setView();
                    SweetAlertDialog.OnSweetClickListener listener = sweetAlertDialog -> {
//                        checkAuthentication();
                        mDialog.dismiss();
                    };
                    showMsg(getString(R.string.msg_bluetooth_use_title), getString(R.string.msg_bluetooth_use_content), null);

                }
                break;
        }
    }

    public void setPushData(Intent intent) {

        mBluetoothFlgOld = ((PCVApplication) getApplication()).getBluetoothFlg();
        mPwdCheckFlgOld = ((PCVApplication) getApplication()).getPwdCheckFlg();

        Map<String, String> messageData= (Map)intent.getSerializableExtra(Config.Key.KEY_NOTIFY_DATA);

        updatePushData(messageData);

        //通知を表示
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String title = intent.getStringExtra(Config.Key.KEY_NOTIFY_TITLE);
        String body = intent.getStringExtra(Config.Key.KEY_NOTIFY_BODY);
        if(title!=null||body!=null) {
            showNotify(title, body);
        }
        //No57
        if (!isNetworkConnected(this)) {
//            showMsg("インターネットなし", "インターネットなし", null);
            return;
        }
        //No57end

        //認証ユーザ削除以外
        if (!(messageData.containsKey(Config.PushKey.USER_ID) && messageData.containsKey(Config.PushKey.USER_STATUS)
                && Config.Status.DELETE.equals(messageData.get(Config.PushKey.USER_STATUS))
                && ((PCVApplication) getApplication()).getUserId().equals(messageData.get(Config.PushKey.USER_ID)))) {
            mIsPush = true;
            getData();
        }


    }

    public void bindPc() {
        mIsPush = true;
        refreshStatus(STATUS_NOT_LOGIN);
    }

    //BluetoothBind
    public static class BluetoothBindBroadcast extends BroadcastReceiver {

        public BluetoothBindBroadcast(){ super(); }

        @Override
        public void onReceive(Context context, Intent intent) {
            PCVApplication.getInstance().getHomeActivity().bindPc();
        }
    }

    public static class PushBroadcast extends BroadcastReceiver {

        public PushBroadcast(){ super(); }

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                PCVApplication.getInstance().getHomeActivity().setPushData(intent);
            }catch (Exception e){
                Log.e("Exception",e.toString());
            }
        }
    }

    private void updatePushData(Map<String, String> messageData){
        //1、アプリ起動認証無効/有効変更
        if (messageData.containsKey(Config.PushKey.STARTUP_FLAG)) {
            ((PCVApplication) getApplication()).savePwdCheckFlg(messageData.get(Config.PushKey.STARTUP_FLAG));
        }
        //2、認証ユーザ変更
        if (messageData.containsKey(Config.PushKey.USER_ID) && messageData.containsKey(Config.PushKey.USER_STATUS)) {
            switch (messageData.get(Config.PushKey.USER_STATUS)) {
                case Config.Status.STOP :
                    break;
                case Config.Status.NORMAL :
                    break;
                case Config.Status.DELETE :
                    if (((PCVApplication) getApplication()).getUserId().equals(messageData.get(Config.PushKey.USER_ID))) {
                        deleteAllData();
                    }
                    getData();
                    break;
            }
        }
        //３、PC削除
        if (messageData.containsKey(Config.PushKey.PCUUID) && !messageData.containsKey(Config.PushKey.SMARTPHONE_STATUS)) {
            DBUtils.deletePC(messageData.get(Config.PushKey.PCUUID));
        }
        //４、スマホ変更
        if (messageData.containsKey(Config.PushKey.PCUUID) && messageData.containsKey(Config.PushKey.SMARTPHONE_STATUS)) {
            //「PC連携」．「有効フラグ」を更新する。
            String webPCUuid = messageData.get(Config.PushKey.PCUUID);
            String pcFlag = messageData.get(Config.PushKey.SMARTPHONE_STATUS);
            if (pcFlag != null && (pcFlag.equals(Config.Status.STOP) || pcFlag.equals(Config.Status.NORMAL) ||  pcFlag.equals(Config.Status.DELETE))) {
                if (!"".equals(webPCUuid)) {
                    ((PCVApplication) getApplication()).saveBluetoothFlg(pcFlag);
                    AuthPCModel model = DBUtils.getAuthPCInfo(webPCUuid);
                    if (model != null) {
                        model.setValidFlg(pcFlag);
                        DBUtils.saveAuthPC(model);
                    }
                }
            } else {
                Toast.makeText(PCVApplication.getInstance(),"異常なデータ",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteAllData() {
        ((PCVApplication) getApplication()).saveFlagInfo("", "");
        DBUtils.deleteAll();
    }

    private void showNotify (String titleText, String contentText) {
        int id = (int) System.currentTimeMillis();
        Intent intent =new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent  = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notifyLiveStart(HomeActivity.this, pendingIntent, id,titleText,contentText, "通知");
    }

    private void notifyLiveStart(Context context, PendingIntent intent, int id, String titleText, String contentText, String tickerText) {
        // 创建notification
        Notification notification = new NotificationCompat.Builder(HomeActivity.this, "statusNotify")
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(intent)
//                .setNumber(1)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
//                .setSmallIcon(R.drawable.app_logo)
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.app_logo))
                .build();
        mNotificationManager.notify(id, notification);
   }

    private void showBiometricPrompt() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("認証してください")
//                .setSubtitle("Log in using your biometric credential")
                .setDeviceCredentialAllowed(true)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(HomeActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                        errString, Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                BiometricPrompt.CryptoObject authenticatedCryptoObject =
                        result.getCryptoObject();
                //認証成功
                if (mStatus == STATUS_NOT_LOGIN) {
                    //Bluetoothの設定
                    checkBluetoothFlag();
                    PCVApplication.getInstance().setUnlockTime(new Date().getTime());
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "認証に失敗しました",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });

        // Displays the "log in" prompt.
        biometricPrompt.authenticate(promptInfo);
    }
}
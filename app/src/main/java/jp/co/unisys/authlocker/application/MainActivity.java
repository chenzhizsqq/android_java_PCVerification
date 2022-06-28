package jp.co.unisys.authlocker.application;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;


import com.google.gson.Gson;
import com.neusoft.convenient.activity.ConvenientMainActivity;
import com.neusoft.convenient.file.download.listener.FileDownloadListener;
import com.neusoft.convenient.network.http.ConvenientHttpConnection;
import com.neusoft.convenient.network.http.listener.OnHttpConnectionListener;
import com.neusoft.convenient.permission.annotations.Permission;
import com.neusoft.convenient.util.CodeUtils;
import com.neusoft.convenient.util.PhoneUtils;
import com.neusoft.convenient.version.VersionUpdate;
import com.neusoft.convenient.view.ContentView;
import com.neusoft.pcverification.BuildConfig;
import com.neusoft.pcverification.R;
import com.neusoft.pcverification.databinding.ActivityMainBinding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jp.co.unisys.authlocker.activity.HomeActivity;
import jp.co.unisys.authlocker.bluetooth.BluetoothStateChangedBroadcast;
import jp.co.unisys.authlocker.config.Config;
import jp.co.unisys.authlocker.db.DBUtils;
import jp.co.unisys.authlocker.response.GetDataResponse;
import jp.co.unisys.authlocker.response.GetTokenResponse;
import jp.co.unisys.authlocker.service.BluetoothService;
import jp.co.unisys.authlocker.service.FireBaseService;
import jp.co.unisys.authlocker.util.sweetalert.SweetAlertDialog;

import static jp.co.unisys.authlocker.activity.HomeActivity.STATUS_LOCAL;
import static jp.co.unisys.authlocker.activity.HomeActivity.STATUS_NOT_USE;
import static jp.co.unisys.authlocker.application.PCVApplication.mBluetoothAdapter;

import static jp.co.unisys.authlocker.config.Config.VersionName;
import static jp.co.unisys.authlocker.config.Config.viewheight;

@Permission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE})
@ContentView(R.layout.activity_main)
public class MainActivity extends ConvenientMainActivity<ActivityMainBinding> {


    private static SweetAlertDialog mDialog;

    private Bundle mBundle;

    private  SweetAlertDialog mLoadingDialog;

    private String mToken = "";

    private String mMessageCode = "";

    private static final int ACTION_BLUETOOTH_ENABLE_REQUEST_CODE = 1;

    private static boolean closeServiceFlag = true;

    @Override
    protected boolean isUseDefaultDownloadDialog() {
        return false;
    }

    @Override
    protected FileDownloadListener getFileDownloadListener() {
        return null;
    }

    @Override
    protected void showUpdateDownload(VersionUpdate versionUpdate) {
    }

    @Override
    protected void requestPermissionsFailure() {
    }

    @Override
    protected void closeMainToHomeActivity() {
        SweetAlertDialog.OnSweetClickListener listener = sweetAlertDialog -> finish();
        // Bluetoothモジュールなし
        if (mBluetoothAdapter == null) {
            showMsg(getString(R.string.msg_bluetooth_not_exist), listener);
        } else if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showMsg(getString(R.string.msg_bluetooth_ble_not_exist), listener);
        } else {
            //TODO start bluetooth
            // Bluetoothがオンになっていない
            if (!mBluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, ACTION_BLUETOOTH_ENABLE_REQUEST_CODE);
            }
            // Bluetoothがオンになっています
            else {
                bluetoothDeviceVisible();
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //ブロードキャストを登録する
        Log.d(this.getClass().getName(),"有効性ステータスが取得されること");

        PCVApplication.getInstance().setMainActivity(this);

        //FCMtoken取得

        FireBaseService.getId(MainActivity.this);
        Intent intent = getIntent();

        closeServiceFlag = true;

        //
        if (intent != null) {
            if (intent.hasExtra("message")){
                String message = intent.getStringExtra("message");
                boolean jumpFlag = intent.getBooleanExtra("jumpFlag", false);
                if (jumpFlag) {
                    showMsg(message , (sweetAlertDialog)->{
                        mDialog.dismiss();
                        Intent intent2 = new Intent(MainActivity.this,HomeActivity.class);
                        intent2.putExtra("status", STATUS_LOCAL);
                        startActivity(intent2);
                        closeServiceFlag = false;
                        finish();
                    });
                    return;
                } else {
                    showMsg(message , (v) -> {
                        mDialog.dismiss();
                        Intent intent2 = new Intent(MainActivity.this,BluetoothService.class);
                        stopService(intent2);
                        Log.d("HomeActivity","BluetoothService close");
                        finish();
                    });
                    return;
                }
            }

            if (intent.getExtras()!= null) {
                mBundle = intent.getExtras();
                Map<String, String> messageData = new HashMap<String, String>();
                for (String key: mBundle.keySet()) {
                    if (Config.PushKey.PCUUID.equals(key) || Config.PushKey.SMARTPHONE_STATUS.equals(key) || Config.PushKey.STARTUP_FLAG.equals(key) ||
                            Config.PushKey.USER_ID.equals(key) || Config.PushKey.USER_STATUS.equals(key)) {
                        messageData.put(key, mBundle.getString(key));
                    }
                }
//                updatePushData(messageData);

            }
//        } else {
//            mBundle = null;
        }

        WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();

        Point realSize = new Point();
        disp.getRealSize(realSize);
        int realScreenHeight = realSize.y;

        WindowManager wm2 = (WindowManager)getSystemService(WINDOW_SERVICE);
        Display disp2 = wm2.getDefaultDisplay();
        Point size = new Point();
        disp2.getSize(size);

        int screenHeight = size.y;
        viewheight=50+realScreenHeight-screenHeight;

        TextView VersionView =(TextView)findViewById(R.id.VersionText);
        VersionName=getVersionName(this);
        Log.e("test",VersionName);
        Log.e("test", String.valueOf(viewheight));
        VersionView.setHeight(viewheight);
        VersionView.setText("ver "+VersionName);

        getToken();
    }

    public void showMsg(String message, SweetAlertDialog.OnSweetClickListener listener) {
        mDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.NORMAL_TYPE);
        mDialog.setTitleText("");
        mDialog.setContentText(message);
        mDialog.setConfirmText(MainActivity.this.getString(R.string.dialog_ok));
        mDialog.showCancelButton(false);
        mDialog.setCancelable(false);
        if (listener != null) {
            mDialog.setConfirmClickListener(listener);
        } else {
            mDialog.setConfirmClickListener(sweetAlertDialog -> {
                mDialog.dismiss();
            });
        }
        mDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Bluetoothモジュール開始
        if (requestCode == ACTION_BLUETOOTH_ENABLE_REQUEST_CODE) {
            // 正常に開く
            if (resultCode == RESULT_OK) {
                bluetoothDeviceVisible();
            }
            // 開かない
            else {
            }
        }
    }

    /**
     * Bluetoothデバイスが見える
     */
    private void bluetoothDeviceVisible() {


        setDiscoverableTimeout(300);

        sendBluetoothVisibleBroadcast();


        // Bluetoothサービスを開始
        Intent bluetoothServiceIntent = new Intent(MainActivity.this, BluetoothService.class);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startForegroundService(bluetoothServiceIntent);
        else
            startService(bluetoothServiceIntent);

        //TODO

//        if (DBUtils.isHaveData()) {
//            Intent intent = new Intent(this, HomeActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
//            if (mBundle != null) {
//                intent.putExtras(mBundle);
//            }
//            startActivity(intent);
//            finish();
//        }
    }

    private void sendBluetoothVisibleBroadcast() {
        Intent intent = new Intent(Config.BroadcastReceiverAction.START_BLE_BLUETOOTH_VISIBLE_BROADCAST);
        sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        if (closeServiceFlag) {
            Intent intent = new Intent(MainActivity.this,BluetoothService.class);
            stopService(intent);
        }
        super.onDestroy();
    }



    //bluetooth bind over
    public static class MainBroadcast extends BroadcastReceiver {

        public MainBroadcast(){ super();}

        @Override
        public void onReceive(Context context, Intent intent) {
            closeServiceFlag=false;
            Intent intent2 = new Intent(context, HomeActivity.class);
            intent2.putExtra("notGetFlag", "1");
            PCVApplication.getInstance().getMainActivity().startActivity(intent2);
            PCVApplication.getInstance().getMainActivity().finish();
        }
    }

    //Bluetoothを永続的に表示するように設定
    public void setDiscoverableTimeout(int timeout) {
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode =BluetoothAdapter.class.getMethod("setScanMode", int.class,int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, timeout);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //VersionNameの取得
    public static String getVersionName(Context context){
        PackageManager pm = context.getPackageManager();
        String versionName = "";
        try{
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
        }catch(PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        //versionName="ver "+versionName;
        return versionName;
    }

    //スマホJWTトークン発行API
    private void getToken() {
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

    //利用設定取得API
    private void getFlag() {
        new Thread(() -> {
            ConvenientHttpConnection httpConnection = new ConvenientHttpConnection();
            Map<String, String> headerParams = new HashMap<>();
            Map<String, String> body = new HashMap<>();

            body.put(Config.Key.KEY_USERID2, ((PCVApplication) getApplication()).getUserId());
            body.put(Config.Key.KEY_PHONEKEY, ((PCVApplication) getApplication()).getNotifyToken());
            body.put(Config.Key.KEY_TOKEN, mToken);

            headerParams.put("Content-Type", "application/json");

            String serverUrl = PCVApplication.getInstance().getUrl();
            if (serverUrl.equals("")) {
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
                            mMessageCode = data.getMessageCode();
                            if ("0".equals(data.getCode())) {
                                ((PCVApplication) getApplication()).saveFlagInfo(data.getStartupflag(), data.getUsabilityflag());
                                //有効性フラグ
                                DBUtils.refreshAuthPC(data.getSmartphoneList());
                            }
                        }
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
                    (MainActivity.this).runOnUiThread(() -> {
                        getDataEnd();
                    });

                }
            });
        }).start();

    }

    private void getDataEnd() {
        (MainActivity.this).runOnUiThread(() -> {
            //messageCode
            Intent intent = new Intent();
            switch (mMessageCode) {
                case "" :
                case "PCWZ00C15" :
                    intent = new Intent(MainActivity.this,HomeActivity.class);
                    intent.putExtra("status", STATUS_LOCAL);
                    startActivity(intent);
                    closeServiceFlag = false;
                    finish();
                    return;
                case "PCWZ00C14" :
                    intent = new Intent(MainActivity.this,HomeActivity.class);
                    intent.putExtra("message",getString(R.string.msg_not_use_title));
                    intent.putExtra("status", STATUS_NOT_USE);
                    startActivity(intent);
                    closeServiceFlag = false;
                    finish();
                    return;
                case "PCWP05C01" :
                    intent = new Intent(MainActivity.this,HomeActivity.class);
                    intent.putExtra("message",getString(R.string.msg_tenant_not_login));
                    intent.putExtra("status", STATUS_NOT_USE);
                    startActivity(intent);
                    closeServiceFlag = false;
                    finish();
                    return;
                case "PCWZ00C16" :
                case "PCWZ00C30" :
                    showMsg( getString(R.string.msg_get_information_failed_continue), (sweetAlertDialog) -> {
                        mDialog.dismiss();
                    Intent intent2 = new Intent(MainActivity.this,HomeActivity.class);
                    intent2.putExtra("status", STATUS_LOCAL);
                    startActivity(intent2);
                    finish();
                });
                    return;
                case "PCWZ00C17" :
                case "PCWZ00C18" :
                    showMsg(getString(R.string.msg_get_information_failed_close),(v)->{
                        mDialog.dismiss();
                        this.finish();
                    });
                    return;
            }
        });
    }

}

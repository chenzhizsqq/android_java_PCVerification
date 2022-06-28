package jp.co.unisys.authlocker.application;


import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;


import com.neusoft.convenient.application.ConvenientApplication;
import com.neusoft.convenient.database.Selector;
import com.neusoft.convenient.database.util.DatabaseUtil;
import com.neusoft.convenient.util.ObjectUtils;
import com.neusoft.pcverification.BuildConfig;

import jp.co.unisys.authlocker.activity.HomeActivity;
import jp.co.unisys.authlocker.bluetooth.BluetoothStateChangedBroadcast;
import jp.co.unisys.authlocker.config.Config;
import jp.co.unisys.authlocker.db.DBUtils;
import jp.co.unisys.authlocker.db.model.AuthPCModel;
import jp.co.unisys.authlocker.db.model.DeviceIdModel;
import jp.co.unisys.authlocker.util.AESUtils;
import jp.co.unisys.authlocker.util.BluetoothUtils;
import jp.co.unisys.authlocker.util.CrashHandler;

import java.util.List;

import static jp.co.unisys.authlocker.db.DBUtils.aes;

public class PCVApplication extends ConvenientApplication {

    static private PCVApplication instance;

    private MainActivity mainActivity;
    private HomeActivity homeActivity;

    private String isBlueToothCanConnect = Config.FlagStatus.BLUETOOTH_FLG_DONT;

    private String serverUrl = "";

    private long unlockTime = 0;

    public static final String CLASSIC_BLUETOOTH_UUID_KEY = "c908fbed-0225-4b3a-9fc8-88e1640c0b01";

    public static BluetoothAdapter mBluetoothAdapter;
    public static String deviceName;
    public static List<AuthPCModel> authPCModels;
    public static String deviceId = "";

    @Override
    protected void initConfigAndData() {
        // Log機能の初期化
//        Log.d("deviceIMEI","App初始化开始");
//        CrashHandler.getInstance().init(this);
//
//        // データベースを初期化する
//        DatabaseUtil.init(this, "pc_verification", 1, null);
//        // Bluetooth機能の初期化
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        deviceName = mBluetoothAdapter == null ? "Ignorance": mBluetoothAdapter.getName();
//        Log.d(PCVApplication.class.getSimpleName(), "device name = " + deviceName);
//
//        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
//        String deviceIMEI = "";
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            String androidID = Settings.System.getString(this.getContentResolver(), Settings.System.ANDROID_ID);
//            if (androidID.equals("")) {
//                SharedPreferences sharedPreferences= getSharedPreferences("data", Context.MODE_PRIVATE);
//                deviceIMEI = sharedPreferences.getString("deviceIMEI","");
//                if (deviceIMEI.equals("")) {
//                    StringBuilder strRand= new StringBuilder();
//                    for(int i=0; i<15; i++) {
//                        strRand.append(String.valueOf((int) (Math.random() * 10)));
//                    }
//                    deviceIMEI = strRand.toString();
//                    SharedPreferences.Editor editor = sharedPreferences.edit();
//                    editor.putString("deviceIMEI", deviceIMEI);
//                    editor.apply();
//                }
//            } else {
//                deviceIMEI = androidID;
//            }
//        } else if (telephonyManager != null && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
//            deviceIMEI = telephonyManager.getDeviceId();
//        }
//        deviceId = deviceIMEI;
//        Log.d("deviceIMEI",deviceIMEI);
//        instance = this;
//
//        String url = DBUtils.getServerAddress();
//        if (!url.equals("")){
//            setServerUrl(url);
//        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Log機能の初期化
        Log.d("deviceIMEI","App初始化开始");
        CrashHandler.getInstance().init(this);

        // データベースを初期化する
        DatabaseUtil.init(this, "pc_verification", 1, null);
        // Bluetooth機能の初期化
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceName = mBluetoothAdapter == null ? "Ignorance": mBluetoothAdapter.getName();
        Log.d(PCVApplication.class.getSimpleName(), "device name = " + deviceName);

        instance = this;

        String url = DBUtils.getServerAddress();
        if (!url.equals("")){
            setServerUrl(url);
        }

        BluetoothStateChangedBroadcast bluetoothStateChangedBroadcast = new BluetoothStateChangedBroadcast();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateChangedBroadcast, filter);
    }

    @Override
    protected String setVersionUpdateDownloadLocalPath() {
        return null;
    }

    @Override
    protected String checkVersionUrl() {
        return null;
    }

    static public PCVApplication getInstance() {
        return instance;
    }

    public String getIsBlueToothCanConnect() {
        return isBlueToothCanConnect;
    }

    public void setIsBlueToothCanConnect(String isBlueToothCanConnect) {
        this.isBlueToothCanConnect = isBlueToothCanConnect;
    }

    public String getPwdCheckFlg() {
        SharedPreferences data = getSharedPreferences("flagInfo", 0);
        return data.getString("pwdCheckFlg", Config.FlagStatus.PWD_CHECK_FLG_DO);
    }

    public String getBluetoothFlg() {
        SharedPreferences data = getSharedPreferences("flagInfo", 0);
        return data.getString("bluetoothFlg", Config.FlagStatus.BLUETOOTH_FLG_DO);
    }

    public void saveFlagInfo(String pwdCheckFlg, String bluetoothFlg) {
        SharedPreferences userInfo = getSharedPreferences("flagInfo", 0);
        SharedPreferences.Editor editor = userInfo.edit();
        editor.putString("pwdCheckFlg", pwdCheckFlg);
        editor.putString("bluetoothFlg", bluetoothFlg);
        editor.commit();
    }

    public void savePwdCheckFlg(String pwdCheckFlg) {
        SharedPreferences userInfo = getSharedPreferences("flagInfo", 0);
        SharedPreferences.Editor editor = userInfo.edit();
        editor.putString("pwdCheckFlg", pwdCheckFlg);
        editor.commit();
    }

    public void saveBluetoothFlg(String bluetoothFlg) {
        SharedPreferences userInfo = getSharedPreferences("flagInfo", 0);
        SharedPreferences.Editor editor = userInfo.edit();
        editor.putString("bluetoothFlg", bluetoothFlg);
        editor.commit();
    }

    public String getUserId() {
        SharedPreferences data = getSharedPreferences("userInfo", 0);
        return data.getString(Config.Key.KEY_USERID, "");
    }

    public void saveUserId(String userId) {
        SharedPreferences userInfo = getSharedPreferences("userInfo", 0);
        SharedPreferences.Editor editor = userInfo.edit();
        editor.putString(Config.Key.KEY_USERID, userId);
        editor.commit();
    }

    public String getNotifyToken() {
        SharedPreferences data = getSharedPreferences("notifyinfo", 0);
        return data.getString(Config.Key.KEY_TOKEN, "");
    }

    public void saveNotifyToken(String token) {
        SharedPreferences userInfo = getSharedPreferences("notifyinfo", 0);
        SharedPreferences.Editor editor = userInfo.edit();
        editor.putString(Config.Key.KEY_TOKEN, token);
        editor.commit();
    }

    public void saveDeviceIdToFile(String deviceId) {
        SharedPreferences sharedPreferences= getSharedPreferences("data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        DeviceIdModel model = new DeviceIdModel(deviceId);
        aes(model, true);
        editor.putString("deviceIMEI", model.getDeviceId());
        editor.apply();
    }

    public String getDeviceIdToFile() {
        SharedPreferences sharedPreferences= getSharedPreferences("data", Context.MODE_PRIVATE);
        String deviceIMEI = sharedPreferences.getString("deviceIMEI","");
        //get aes DeiceId
        DeviceIdModel getModel = new DeviceIdModel(deviceIMEI);
        aes(getModel, false);
        deviceIMEI = getModel.getDeviceId();
        return deviceIMEI;
    }

    public String getDeviceId() {
        boolean getFlag = true;
        if (deviceId.equals("")) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            String deviceIMEI = "";
            deviceIMEI = getDeviceIdToFile();
            if (!deviceIMEI.equals("")) {
                deviceId = deviceIMEI;
                return deviceId;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String androidID = Settings.System.getString(this.getContentResolver(), Settings.System.ANDROID_ID);
                if (androidID.equals("")) {
                    getFlag = false;
                } else {
                    deviceId = androidID;
                    saveDeviceIdToFile(deviceId);
                    return deviceId;
                }
            } else if (telephonyManager != null && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                deviceIMEI = telephonyManager.getDeviceId();
                if (deviceIMEI.equals("")) {
                    getFlag = false;
                } else {
                    deviceId = deviceIMEI;
                    saveDeviceIdToFile(deviceId);
                    return deviceId;
                }
            }
            if (!getFlag) {
                StringBuilder strRand = new StringBuilder();
                for (int i = 0; i < 16; i++) {
                    strRand.append(String.valueOf((int) (Math.random() * 10)));
                }
                deviceIMEI = strRand.toString();
                deviceId = deviceIMEI;
                saveDeviceIdToFile(deviceId);
            }
        }
        return deviceId;
    }

    public HomeActivity getHomeActivity() {
        return homeActivity;
    }

    public void setHomeActivity(HomeActivity homeActivity) {
        this.homeActivity = homeActivity;
    }

    public MainActivity getMainActivity() {
        return mainActivity;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getUrl() {
        return this.serverUrl;
    }

    public long getUnlockTime() {
        return unlockTime;
    }

    public void setUnlockTime(long unlockTime) {
        this.unlockTime = unlockTime;
    }
}

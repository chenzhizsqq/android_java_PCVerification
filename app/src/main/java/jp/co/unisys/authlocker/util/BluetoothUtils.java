package jp.co.unisys.authlocker.util;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.neusoft.convenient.util.CodeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import jp.co.unisys.authlocker.application.PCVApplication;
import jp.co.unisys.authlocker.db.model.AuthPCModel;

import static android.content.Context.TELEPHONY_SERVICE;
import static android.text.TextUtils.split;

public class BluetoothUtils {

    public static String getBluetoothMacAddress() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String bluetoothMacAddress = "";
        try {
            Field mServiceField = bluetoothAdapter.getClass().getDeclaredField("mService");
            mServiceField.setAccessible(true);

            Object btManagerService = mServiceField.get(bluetoothAdapter);

            if (btManagerService != null) {
                bluetoothMacAddress = (String) btManagerService.getClass().getMethod("getAddress").invoke(btManagerService);
            }
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {

        }
        return bluetoothMacAddress;
    }

    public static UUID getImeiUnlockUUID(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        try {
            return CodeUtils.string2UUID(CodeUtils.md5Encode(PCVApplication.getInstance().getDeviceId() + "PCUnlock"));
        } catch (Exception e){
            StringBuilder builder = new StringBuilder(PCVApplication.getInstance().getDeviceId() + "PCUnlock");
            return UUID.fromString(builder.toString());
        }
    }

    public static String getByteData(String dbData){
        String[] byteArray = dbData.split(",");
        byte[] bytes = new byte[byteArray.length];
        if(byteArray == null || byteArray.length == 0) {
            return "";
        }
        int i = 0;
        for(String item : byteArray) {
            bytes[i] =  ((byte)Integer.parseInt(item, 10));
            i++;
        }
        String result = "";
        try {
            result = new String(bytes,"UTF-8");
        }catch (Exception e) { }

        return result;
    }

    private static boolean isSql(String str){

        String inj_str = "'|and|exec|insert|select|delete|update|count|*|%|chr|mid|master|truncate|char|declare|;|or|+|union|=";

        String[] inj_stra = split(inj_str,"\\|");

        for (String value : inj_stra ) {
            if (str.contains(value)){
                Log.d("isSql",value);
                return true;
            }
        }
        return false;
    }

    public static boolean isSqlInjection(AuthPCModel authPCModel) {
        boolean isSql = false;
        if (isSql(authPCModel.getPcUuid())) {
            isSql = true;
        } else if (isSql(authPCModel.getValidFlg())) {
            isSql = true;
        }
        if (isSql) {
            Toast.makeText(PCVApplication.getInstance(),"異常なデータ",Toast.LENGTH_SHORT).show();
        }
        return isSql;
    }
}

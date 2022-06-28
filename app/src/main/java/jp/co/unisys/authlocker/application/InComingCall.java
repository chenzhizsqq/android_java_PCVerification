package jp.co.unisys.authlocker.application;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.neusoft.convenient.database.util.DatabaseUtil;
import com.neusoft.convenient.util.CodeUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import jp.co.unisys.authlocker.db.model.AuthPCModel;
import jp.co.unisys.authlocker.service.BluetoothService;
import static jp.co.unisys.authlocker.application.PCVApplication.mBluetoothAdapter;
import static jp.co.unisys.authlocker.service.BluetoothService.mBleBluetoothAdvertiseCallback;
import static jp.co.unisys.authlocker.service.BluetoothService.mBluetoothLeAdvertiser;

public class InComingCall extends BroadcastReceiver {
    @SuppressWarnings("unused")
    private final String TAG = getClass().getSimpleName();

    private Context ctx;
    @Override
    public void onReceive(Context context, Intent intent) {
        ctx = context;
        try {
            //TelephonyManagerの生成
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            //リスナーの登録
            MyPhoneStateListener PhoneListener = new MyPhoneStateListener();
            tm.listen(PhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

        } catch (Exception e) {
            Log.e(TAG, ":" + e);
        }

    }

    /**
     * カスタムリスナーの登録
     * 着信〜終了 CALL_STATE_RINGING > CALL_STATE_OFFHOOK > CALL_STATE_IDLE
     * 不在着信 CALL_STATE_RINGING > CALL_STATE_IDLE
     */
    private class MyPhoneStateListener extends PhoneStateListener {
        @SuppressWarnings("unused")
        private final String TAG = getClass().getSimpleName();
        public void onCallStateChanged(int state, String callNumber) {
            switch(state){
                //待ち受け（終了時）(BLE Advertiseを元に戻す)
                case TelephonyManager.CALL_STATE_IDLE:
                    //Toast.makeText(ctx, "待ち受け（終了時）", Toast.LENGTH_LONG).show();
                    mBluetoothLeAdvertiser.stopAdvertising(mBleBluetoothAdvertiseCallback);

                    PCVApplication.authPCModels = DatabaseUtil.findAll(AuthPCModel.class, null);
                    String temp = CodeUtils.md5Encode(PCVApplication.getInstance().getDeviceId() + "PCUnlock");
                    //  p3a IMEI 359676091494629
                    //temp 5520e8c333e44b3774bd4d5bd2e86736
                    ParcelUuid tempUUID = null;
                    try {
                        tempUUID = new ParcelUuid(CodeUtils.string2UUID(temp));
                        // p3a value c3e82055-e433-374b-74bd-4d5bd2e86736
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Advertise設定（必須)
                    AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                            // 送信電力レベル
                            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                            // 接続できます
                            .setConnectable(false)
                            .build();
                    // Advertiseデータ
                    byte[] TelState;
                    TelState=new byte[1];
                    ByteBuffer byteBuffer = ByteBuffer.wrap(TelState);
                    byteBuffer.put((byte) 0x00);

                    AdvertiseData advertiseData = new AdvertiseData.Builder()
                            // Bluetooth名
//                .setIncludeDeviceName(true)
                            .addServiceUuid(tempUUID)
                            // 送信電力レベル
                            .addManufacturerData(0,TelState)
                            .setIncludeTxPowerLevel(true)
                            .build();

                    mBluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData,null, mBleBluetoothAdvertiseCallback);
                    break;

                    //着信(BLE Advertise変更)
                case TelephonyManager.CALL_STATE_RINGING:

                    //通話(BLE Advertise変更)
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    //Toast.makeText(ctx, "通話", Toast.LENGTH_LONG).show();
                    //Toast.makeText(ctx, "着信: " + callNumber, Toast.LENGTH_LONG).show();
                    mBluetoothLeAdvertiser.stopAdvertising(mBleBluetoothAdvertiseCallback);

                    PCVApplication.authPCModels = DatabaseUtil.findAll(AuthPCModel.class, null);
                    temp = CodeUtils.md5Encode(PCVApplication.getInstance().getDeviceId() + "PCUnlock");
                    //  p3a IMEI 359676091494629
                    //temp 5520e8c333e44b3774bd4d5bd2e86736
                    tempUUID = null;
                    try {
                        tempUUID = new ParcelUuid(CodeUtils.string2UUID(temp));
                        // p3a value c3e82055-e433-374b-74bd-4d5bd2e86736
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Advertise設定（必須)
                    advertiseSettings = new AdvertiseSettings.Builder()
                            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                            // 送信電力レベル
                            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                            // 接続できます
                            .setConnectable(false)
                            .build();
                    // Advertiseデータ
                    TelState=new byte[1];
                    byteBuffer = ByteBuffer.wrap(TelState);
                    byteBuffer.put((byte) 0x01);

                    advertiseData = new AdvertiseData.Builder()
                            // Bluetooth名
//                .setIncludeDeviceName(true)
                            .addServiceUuid(tempUUID)
                            // 送信電力レベル
                            .addManufacturerData(0,TelState)
                            .setIncludeTxPowerLevel(true)
                            .build();

                    mBluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData,null, mBleBluetoothAdvertiseCallback);
                    break;

                //  p3a IMEI 359676091494629
                    //temp 5520e8c333e44b3774bd4d5bd2e86736

                // Advertise設定（必須)
                // Advertiseデータ

            }
        }
    }
}

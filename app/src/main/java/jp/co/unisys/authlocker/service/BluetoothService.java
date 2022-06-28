package jp.co.unisys.authlocker.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.neusoft.convenient.database.Selector;
import com.neusoft.convenient.database.util.DatabaseUtil;
import com.neusoft.convenient.util.CodeUtils;
import com.neusoft.pcverification.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import jp.co.unisys.authlocker.application.MainActivity;
import jp.co.unisys.authlocker.application.PCVApplication;
import jp.co.unisys.authlocker.bluetooth.MessageContentModel;
import jp.co.unisys.authlocker.config.Config;
import jp.co.unisys.authlocker.db.DBUtils;
import jp.co.unisys.authlocker.db.model.AuthPCModel;
import jp.co.unisys.authlocker.util.BluetoothUtils;

import static jp.co.unisys.authlocker.application.PCVApplication.CLASSIC_BLUETOOTH_UUID_KEY;
import static jp.co.unisys.authlocker.application.PCVApplication.mBluetoothAdapter;
import static jp.co.unisys.authlocker.config.Config.TimeOutFlag;
import static jp.co.unisys.authlocker.config.Config.VersionName;

public class BluetoothService extends Service {

    private static final String TAG = BluetoothService.class.getSimpleName();
    private static final String NAME = "Name";

    private BluetoothServerSocket bluetoothServerSocket;
    public static BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    public static AdvertiseCallback mBleBluetoothAdvertiseCallback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(BluetoothService.class.getSimpleName(), "BLEブロードキャストを有効にする");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(BluetoothService.class.getSimpleName(), "BLEブロードキャストに失敗しました ==> " + errorCode);

            if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                Log.e(BluetoothService.class.getSimpleName(), "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.");
            } else if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                Log.e(BluetoothService.class.getSimpleName(), "Failed to start advertising because no advertising instance is available.");
            } else if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                Log.e(BluetoothService.class.getSimpleName(), "Failed to start advertising as the advertising is already started");
            } else if (errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
                Log.e(BluetoothService.class.getSimpleName(), "Operation failed due to an internal error");
            } else if (errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                Log.e(BluetoothService.class.getSimpleName(), "This feature is not supported on this platform");
            }
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Config.BroadcastReceiverAction.START_BLE_BLUETOOTH_VISIBLE_BROADCAST.equals(action)) {
                sendBluetoothVisibleBroadcast();
            }
            abortBroadcast();
        }
    };

    private Map<String, BluetoothSocket> bluetoothSockets;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            setForeground();

        bluetoothSockets = new HashMap<>();
        startBlueServer();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Config.BroadcastReceiverAction.START_BLE_BLUETOOTH_VISIBLE_BROADCAST);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // ブロードキャストリスナーのバインド解除
        unregisterReceiver(broadcastReceiver);

        stopForeground(true);

        mBluetoothLeAdvertiser.stopAdvertising(mBleBluetoothAdvertiseCallback);
        // Bluetoothサービスを停止します
        if (bluetoothServerSocket != null) {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mBluetoothLeAdvertiser=null;
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setForeground() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(getPackageName(), NAME, NotificationManager.IMPORTANCE_HIGH);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
        Notification notification = new Notification.Builder(this, getPackageName())
                .setContentTitle("Bluetoothサービスを開く")
                .setContentText("Bluetoothサービス")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .build();
        startForeground(1, notification);
    }

    // Bluetooth可視Advertiseを初期化する
    @SuppressLint("HardwareIds")
    private void sendBluetoothVisibleBroadcast() {
        if (mBluetoothLeAdvertiser != null) {
//            try {
//                mBluetoothLeAdvertiser.stopAdvertising(mBleBluetoothAdvertiseCallback);
//            } catch (Exception ignored) {
//            }
            return ;
        }
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
        TelState=new byte[4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(TelState);
        byteBuffer.put((byte) 0x00);

        String[] version = VersionName.split(Pattern.quote("."));
        if(version.length == 3)
        {
            //Log.d(BluetoothService.class.getSimpleName(),version[0] + "." + version[1] + "."+version[2]);
            for(String str : version)
            {
                int num = Integer.parseInt(str);
                byteBuffer.put((byte) num);
            }
        }

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                // Bluetooth名
//                .setIncludeDeviceName(true)
                .addServiceUuid(tempUUID)
                // 送信電力レベル
                .addManufacturerData(0,TelState)
                .setIncludeTxPowerLevel(true)
                .build();
//        AdvertiseData.Builder advertiseDataBuilder = new AdvertiseData.Builder();
//        AdvertiseData scanResponse = advertiseDataBuilder.build();

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mBluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData,null, mBleBluetoothAdvertiseCallback);
    }

    private void startBlueServer() {

        try {
            bluetoothServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("", UUID.fromString(CLASSIC_BLUETOOTH_UUID_KEY));
            sendBluetoothVisibleBroadcast();
            Log.d(BluetoothService.class.getSimpleName(), "");
            new Thread(() -> {
                try {
                    BluetoothSocket bluetoothSocket = bluetoothServerSocket.accept();
                    if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                        Log.d(BluetoothService.class.getSimpleName(), "クラシックBluetoothが接続されています");
                        bluetoothSocketManager(bluetoothSocket);
                    } else
                        Log.d(BluetoothService.class.getSimpleName(), "クラシックBluetoothが接続されていません");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            ).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Bluetooth接続管理
     */
    private void bluetoothSocketManager(BluetoothSocket bluetoothSocket) {
        new Thread(() -> {
            InputStream inputStream;
            OutputStream outputStream;
            boolean stop = false;
            try {
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();

                String[] securityFileData = new String[2];
                while (!stop) {
                    byte[] inputData = new byte[1024];
                    inputStream.read(inputData);
                    stop = commandParsing(inputData, securityFileData, outputStream);
                }
                if(bluetoothSocket.isConnected()) {
                    bluetoothSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * コマンド解析
     *
     * @param inputData
     * @return
     */
    private boolean commandParsing(byte[] inputData, String[] securityFileData,OutputStream outputStream) throws IOException {
        // 分散ファイルUUID
        String securityFileUUID = "";
        // 分散ファイル内容
        String securityFileContent = "";

        String inputMessage = CodeUtils.bytes2IntString(inputData);
        int code = -1;
        int length = 0;
        String message = null;

        String[] messageArray = inputMessage.split(",");
        if (messageArray.length > 0)  {
            code = Integer.valueOf(messageArray[1]);
            length = Integer.valueOf(messageArray[3]);
            if (Integer.valueOf(messageArray[4]) != 0) {
                length += Integer.valueOf(messageArray[4])*256;
            }
        }

        switch (code) {
            case 1:
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                outPutMessage(code, PCVApplication.getInstance().getDeviceId(), outputStream);
                break;
            case 2:
                outPutMessage(code, PCVApplication.getInstance().getNotifyToken(), outputStream);
                break;
            case 3:
                String deviceName = android.os.Build.MODEL;
                outPutMessage(code, deviceName, outputStream);
                break;
            case 4:
                try {
                    //messageArray[21-length-16] to securityFileContent
                    String[] fileContent = new String[length - 16];
                    for (int i = 0; i < length - 16 ; i++) fileContent[i] = messageArray[ i + 21];
                    securityFileContent = stringArrayToString(fileContent);

                    //messageArray[5-21] to securityFileUUID
                    String[] fileUUID = new String[16];
                    for (int i=0; i < 16 ; i++) fileUUID[i] = messageArray[i+5];
                    securityFileUUID =  stringArrayToString(fileUUID);

                    securityFileData[0] = securityFileUUID;
                    securityFileData[1] = securityFileContent;

                    outPutMessage(code, "0", outputStream);
                } catch (Exception e) {
                    outPutMessage(code, "1", outputStream);
                }
                break;
            case 5:
                try{
                    //messageArray[5-21] to securityFileUUID
                    String[] fileUUID = new String[16];
                    for (int i=0; i < 16 ; i++) fileUUID[i] = messageArray[i+5];
                    securityFileUUID =  stringArrayToString(fileUUID);

                    //messageArray[21-length-16] to pcUUID
                    String[] fileContent = new String[length - 16];
                    for (int i = 0; i < length - 16 ; i++) fileContent[i] = messageArray[ i + 21];
                    String pcUUId = stringArrayToString(fileContent);

                    Selector securitySelector = new Selector("securityFileUUID", "=", securityFileUUID);
                    AuthPCModel securityAuthPCModel = DatabaseUtil.find(AuthPCModel.class, securitySelector);
                    if (securityAuthPCModel == null) {
                        if (securityFileData[0] == null || securityFileData[0].equals("")) {
                            outPutMessage(code, "1", outputStream);
                            break;
                        } else {
                            securityAuthPCModel = new AuthPCModel();
                            securityAuthPCModel.setSecurityFileUUID(securityFileData[0]);
                            securityAuthPCModel.setSecurityFile(securityFileData[1]);
                            securityAuthPCModel.setPcUuid(BluetoothUtils.getByteData(pcUUId));
                            securityAuthPCModel.setValidFlg(Config.FlagStatus.BLUETOOTH_FLG_DO);
                        }
                    } else {
                        securityAuthPCModel.setSecurityFileUUID(securityFileUUID);
                    }
                    DatabaseUtil.saveOrUpdate(securityAuthPCModel);
                    outPutMessage(code, "0", outputStream);
                } catch (Exception e) {
                    outPutMessage(code, "1", outputStream);
                }
                break;
            case 6:
                try{
                    //messageArray[5-21] to securityFileUUID
                    String[] fileUUID = new String[16];
                    for (int i=0; i < 16 ; i++) fileUUID[i] = messageArray[i+5];
                    securityFileUUID =  stringArrayToString(fileUUID);

                    //messageArray[21-length-16] to UserUUID
                    String[] userUUID = new String[length - 16];
                    for (int i = 0; i < length - 16 ; i++) userUUID[i] = messageArray[ i + 21];
                    String userUUId = stringArrayToString(userUUID);

                    PCVApplication.getInstance().saveUserId(BluetoothUtils.getByteData(userUUId));

                    outPutMessage(code, "0", outputStream);
                } catch (Exception e) {
                    outPutMessage(code, "1", outputStream);
                }
                break;
            // PC2Android 分散ファイルの取得
            case 7:
                /*if(lastOutputCode ==9){
                    TimeOutFlag=false;
                }*/
                String[] fileUUID = new String[16];
                for (int i=0; i < 16 ; i++) fileUUID[i] = messageArray[i+5];
                securityFileUUID = stringArrayToString(fileUUID);

                Selector securityUUIDSelector = new Selector("securityFileUUID", "=", securityFileUUID);
                AuthPCModel securityUUIDAuthPCModel = DatabaseUtil.find(AuthPCModel.class, securityUUIDSelector);

                String content = securityFileUUID + ",1";
                //outTime
                if (TimeOutFlag==true) {
                    Log.d(BluetoothService.class.getSimpleName(),"TimeOutFlagを送信されました");
                    String contentTimeOut= securityFileUUID + ",2";
                    outPutMessage(code,contentTimeOut, outputStream);
                    TimeOutFlag=false;
                    if (PCVApplication.getInstance().getHomeActivity() != null) {
                        PCVApplication.getInstance().getHomeActivity().runOnUiThread(()-> {
                            PCVApplication.getInstance().getHomeActivity().refreshStatus(1);
                        });
                    }
                    return true;
                }
                if (securityUUIDAuthPCModel != null
                        && PCVApplication.getInstance().getIsBlueToothCanConnect().equals(Config.FlagStatus.BLUETOOTH_FLG_DO)
                        && securityUUIDAuthPCModel.getValidFlg().equals(Config.FlagStatus.BLUETOOTH_FLG_DO)) {
                    securityFileContent = securityUUIDAuthPCModel.getSecurityFile();

                    //No4
                    content = securityFileUUID  + ",0," + securityFileContent;
                }
                outPutMessage(code,content, outputStream);

                return true;
            case 8:
                try {
                    String[] serverUUID = new String[16];
                    for (int i=0; i < 16 ; i++) serverUUID[i] = messageArray[i+5];
                    securityFileUUID = stringArrayToString(serverUUID);

                    //messageArray[21-length-16] to serverUrl
                    String[] serverUrls = new String[length - 16];
                    for (int i = 0; i < length - 16 ; i++) serverUrls[i] = messageArray[ i + 21];
                    String serverUrl = stringArrayToString(serverUrls);

                    PCVApplication.getInstance().setServerUrl(BluetoothUtils.getByteData(serverUrl));
                    DBUtils.saveServerAddress(BluetoothUtils.getByteData(serverUrl));

                    outPutMessage(code, "0", outputStream);
                    } catch (Exception e) {
                        outPutMessage(code, "1", outputStream);
                    }
                break;
            case 9:
                Log.d(BluetoothService.class.getSimpleName(),"Locking通知を受信しました。");
                if (PCVApplication.getInstance().getHomeActivity() != null) {
                    PCVApplication.getInstance().getHomeActivity().runOnUiThread(()-> {
                        PCVApplication.getInstance().getHomeActivity().refreshStatus(1);
                    });
                }
                outPutMessage(code, "0", outputStream);
                return true;
        }
        BitmapFactory.Options a = new BitmapFactory.Options();
        a.inJustDecodeBounds = true;

        return false;
    }

    /**
     * Android --> PC 出力データ
     *
     * @param code
     * @param outputStream
     */
    private void outPutMessage(int code, String content, OutputStream outputStream) throws IOException {
        MessageContentModel messageContentModel = null;
        byte[] responseByte;
        switch (code) {
            case 1:
                //IMEIを送信する
                messageContentModel = new MessageContentModel(code, content);
                try {
                    responseByte = messageContentModel.toBytes();
                    Log.d(BluetoothService.class.getSimpleName(), "IMEIを送信されました");
                    outputStream.write(responseByte);
                    outputStream.flush();
                } catch (IOException e) {
                    bluetoothServerSocket.close();
                    startBlueServer();
                    e.printStackTrace();
                }
                break;
            case 2:
                //FireBaseTokenを送信する
                if (content == null || content.equals("")) {
                    messageContentModel = new MessageContentModel(code, "");
                } else {
                    messageContentModel = new MessageContentModel(code, content);
                }
                try {
                    responseByte = messageContentModel.toBytes();
                    Log.d(BluetoothService.class.getSimpleName(), "TOKENを送信されました");
                    outputStream.write(responseByte);
                    outputStream.flush();
                } catch (IOException e) {
                    bluetoothServerSocket.close();
                    startBlueServer();
                    e.printStackTrace();
                }
                break;
            case 3:
                //deviceNameを送信する
                messageContentModel = new MessageContentModel(code, content);
                try {
                    responseByte = messageContentModel.toBytes();
                    Log.d(BluetoothService.class.getSimpleName(), "DEVICE_IDを送信されました");
                    outputStream.write(responseByte);
                    outputStream.flush();
                } catch (IOException e) {
                    bluetoothServerSocket.close();
                    startBlueServer();
                    e.printStackTrace();
                }
                break;
            case 4:
                //分散ファイル取得の成功フラグを送信する
                messageContentModel = new MessageContentModel(code, content);
                try {
                    responseByte = messageContentModel.toBytes();
                    Log.d(BluetoothService.class.getSimpleName(), "成功フラグを送信されました");
                    outputStream.write(responseByte);
                    outputStream.flush();
                    Log.d(BluetoothService.class.getSimpleName(),content);
                } catch (IOException e) {
                    bluetoothServerSocket.close();
                    startBlueServer();
                    e.printStackTrace();
                }
                break;
            case 5:
                //PCUUID取得の成功フラグを送信する
                messageContentModel = new MessageContentModel(code, content);
                try {
                    responseByte = messageContentModel.toBytes();
                    Log.d(BluetoothService.class.getSimpleName(), "PCUUID取得の成功フラグが送信されました");
                    outputStream.write(responseByte);
                    outputStream.flush();
                } catch (IOException e) {
                    bluetoothServerSocket.close();
                    startBlueServer();
                    e.printStackTrace();
                }
                break;
            case 6:
                //UserUUID取得の成功フラグを送信する
                messageContentModel = new MessageContentModel(code, content);
                try {
                    responseByte = messageContentModel.toBytes();
                    Log.d(BluetoothService.class.getSimpleName(), "UserUUID取得の成功フラグが送信されました");
                    outputStream.write(responseByte);
                    outputStream.flush();
                    Intent intent = new Intent(Config.Key.PAIRING_SUCCESS);
//                    intent.setClass(this, HomeActivity.BluetoothBindBroadcast.class);
                    intent.setClass(this, MainActivity.MainBroadcast.class);
                    sendBroadcast(intent);
                    bluetoothServerSocket.close();
                    startBlueServer();
                } catch (IOException e) {
                    bluetoothServerSocket.close();
                    startBlueServer();
                    e.printStackTrace();
                }
                break;
            case 7:
                // 分散ファイルを送信する
                messageContentModel = new MessageContentModel(code, content);
                try {
                    responseByte = messageContentModel.toBytes();
                    outputStream.write(responseByte);
                    outputStream.flush();
                    bluetoothServerSocket.close();
                    startBlueServer();
                    Log.d(BluetoothService.class.getSimpleName(), "分散ファイルが送信されました");
                } catch (IOException e) {
                    bluetoothServerSocket.close();
                    startBlueServer();
                    e.printStackTrace();
                }
                break;
            case 8:
                messageContentModel = new MessageContentModel(code, content);
                try {
                    responseByte = messageContentModel.toBytes();
                    Log.d(BluetoothService.class.getSimpleName(), "ServerGet");
                    outputStream.write(responseByte);
                    outputStream.flush();
                } catch (IOException e) {
                    bluetoothServerSocket.close();
                    startBlueServer();
                    e.printStackTrace();
                }
                break;
            case 9:
                messageContentModel = new MessageContentModel(code, content);
                try {
                    responseByte = messageContentModel.toBytes();
                    outputStream.write(responseByte);
                    outputStream.flush();
                    bluetoothServerSocket.close();
                    startBlueServer();
                    Log.d(BluetoothService.class.getSimpleName(), "Locking処理完了");
                } catch (IOException e) {
                    bluetoothServerSocket.close();
                    startBlueServer();
                    e.printStackTrace();
                }
                break;
        }

    }

    private byte[] stringToByteArray2(String[] data) {
        if(data == null || data.length == 0) {
            return new byte[1];
        }

        byte[] result = new byte[data.length];
        int i = 0;
        for(String item : data) {
            result[i] =  ((byte)Integer.parseInt(item, 10));
            i++;
        }
        return result;
    }

    private String stringArrayToString(String[] data) {
        if(data == null || data.length == 0) {
            return "";
        }
        String result = "";
        for (int i = 0; i < data.length; i++) {
            if( i != 0 || i == data.length -1 ) {
                result += ",";
            }
            result += data[i];
        }
        return result;
    }

}

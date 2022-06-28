package jp.co.unisys.authlocker.config;

public class Config {

    // URL
    public static class URL {
        //TODO
        //JWTトークン発行API address
        public static final String ADDR_GET_TOKEN = "/api/TokenPublish/SmartPhone";
        //利用設定取得API address
        public static final String ADDR_GET_DATA = "/api/UsageSettingFetch";

//        JWTトークン発行API address
//        public static final String ADDR_GET_TOKEN = "https://FrontDoor-prd.azurefd.net/api/TokenPublish/SmartPhone";
//        //利用設定取得API address
//        public static final String ADDR_GET_DATA = "https://FrontDoor-prd.azurefd.net/api/UsageSettingFetch";
//        //        JWTトークン発行API address
//        public static final String ADDR_GET_TOKEN = "http://10.10.132.99:8080/api/TokenPublish/SmartPhone";
//        //利用設定取得API address
//        public static final String ADDR_GET_DATA = "http://10.10.132.99:8080/api/UsageSettingFetch";

    }

    // BroadcastReceiver action
    public static class BroadcastReceiverAction {
        // open bluetooth
        public static final String START_BLE_BLUETOOTH_VISIBLE_BROADCAST = "START_BLE_BLUETOOTH_VISIBLE_BROADCAST";
    }

    //利用設定取得FLAG
    public static class FlagStatus {
        //アプリ起動認証flag 「無効」
        public static final String PWD_CHECK_FLG_DONT = "0";
        //アプリ起動認証flag 「有効」
        public static final String PWD_CHECK_FLG_DO = "1";
        //利用停止 「停止」
        public static final String BLUETOOTH_FLG_DONT = "1";
        //利用停止 「停止解除」
        public static final String BLUETOOTH_FLG_DO = "0";
    }

    public static class PushKey {
        //アプリ起動認証flag
        public static final String STARTUP_FLAG = "startupflag";
        //認証ユーザ変更 userid
        public static final String USER_ID = "userid";
        //認証ユーザ変更 userstatus
        public static final String USER_STATUS = "userstatus";
        //PC pcuuid
        public static final String PCUUID = "pcuuid";
        //PC smartphonestatus
        public static final String SMARTPHONE_STATUS = "smartphonestatus";


    }

    public static class Key {
        //api pcuuid
        public static final String KEY_PCUUID = "pcuuid";
        //api userUuid
        public static final String KEY_USERID = "userUuid";
        //api userKey
        public static final String KEY_USERID2 = "userUuid";
        //api phoneKey
        public static final String KEY_PHONEKEY2 = "smartphoneUuid";
        //api smartphoneUuid
        public static final String KEY_PHONEKEY = "smartphoneUuid";
        //api token
        public static final String KEY_TOKEN = "token";
        //PC有効flag
        public static final String KEY_PC_STATUS_FLG = "smartphonestatus";
        //アプリ起動認証flag
        public static final String KEY_PWD_CHECK_FLG = "startupFlag";
        //利用停止flag
        public static final String KEY_BLUETOOTH_FLG = "usabilityFlag";
        //プッシュ通知 key
        public static final String KEY_NOTIFY_DATA = "messageData";
        //プッシュ通知 key
        public static final String KEY_NOTIFY_TITLE = "title";
        //プッシュ通知 key
        public static final String KEY_NOTIFY_BODY = "body";
        //pairingSuccess key
        public static final String PAIRING_SUCCESS = "pairingSuccess";
        //mPushReceiver key
        public static final String MESSAGE_DATA = "messageData";
    }

    public static class Status {
        //利用停止
        public static final String STOP = "1";
        //利用停止解除
        public static final String NORMAL = "0";
        //削除
        public static final String DELETE = "2";
    }
    public static String VersionName="";

    public static int viewheight=0;

    public static boolean TimeOutFlag=false;

    public static boolean AppForgroundFlag=false;
}

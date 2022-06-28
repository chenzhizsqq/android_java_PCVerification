////////////////////////////////////////////////////////////////////////////////
// system:Tmeic
//
// file name: LoginLogic.java
// class name: LoginLogic
//
// Ver.   date       auther           comment
//-----------------------------------------------------------------------------
// V1.00  2018年05月11日 yangw
////////////////////////////////////////////////////////////////////////////////
package jp.co.unisys.authlocker.db;

import android.text.TextUtils;
import android.util.Log;


import com.neusoft.convenient.database.Selector;
import com.neusoft.convenient.database.util.DatabaseUtil;
import com.neusoft.convenient.util.ObjectUtils;

import jp.co.unisys.authlocker.db.model.AuthPCModel;
import jp.co.unisys.authlocker.db.model.ServerInfo;
import jp.co.unisys.authlocker.response.StatusFlagData;
import jp.co.unisys.authlocker.util.AESUtils;
import jp.co.unisys.authlocker.util.BluetoothUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class DBUtils {

    public static void saveAuthPC(AuthPCModel model) {
        if (model != null && !TextUtils.isEmpty(model.getPcUuid())) {
            if (BluetoothUtils.isSqlInjection(model)) {
                return;
            }

            AuthPCModel dbAuthPCModel = new AuthPCModel();
            try {
                ObjectUtils.objectCopy(dbAuthPCModel, model, null);
                aes(dbAuthPCModel, true);
                DatabaseUtil.saveOrUpdate(dbAuthPCModel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static AuthPCModel getAuthPCInfo(String webPCUuid) {
        try {
            Selector selector = new Selector("pcUuid", "=", AESUtils.encryption(webPCUuid));
            List<AuthPCModel> list = DatabaseUtil.findAll(AuthPCModel.class, selector);
            if (list != null && list.size() > 0) {
                aes(list.get(0), false);
                return list.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void refreshAuthPC(List<StatusFlagData> list) {
        if (list == null || list.size() == 0) {
            return;
        }
        for (StatusFlagData data : list) {
            AuthPCModel model = getAuthPCInfo(data.getPcuuid());
            if (model == null) {
                model = new AuthPCModel();
                model.setPcUuid(data.getPcuuid());
            }
            model.setValidFlg(data.getSmartphonestatus());

            saveAuthPC(model);
        }
    }

    public static boolean isHaveData() {
        List<AuthPCModel> list = DatabaseUtil.findAll(AuthPCModel.class, null);
        boolean result = true;
        if (list == null || list.size() == 0) {
            result = false;
        }
        // TODO test
//        return true;
        return result;
    }

    public static void deletePC(String webPCUuid) {
        Selector selector = null;
        try {
            selector = new Selector("PcUuid", "=", AESUtils.encryption(webPCUuid));
            DatabaseUtil.deleteAll(AuthPCModel.class, selector);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteAll() {
        DatabaseUtil.deleteAll(AuthPCModel.class, null);
    }

    public static void saveServerAddress(String address) {
        DatabaseUtil.deleteAll(ServerInfo.class, null);
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setReleaseUrl(address);
        ServerInfo dbServerAddress = new ServerInfo();
        try {
            ObjectUtils.objectCopy(dbServerAddress, serverInfo, null);
            aes(dbServerAddress, true);
            DatabaseUtil.saveOrUpdate(dbServerAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getServerAddress() {
        try {
            List<ServerInfo> list = DatabaseUtil.findAll(ServerInfo.class, null);
            if (list != null && list.size() > 0) {
                aes(list.get(0), false);
                return list.get(0).getReleaseUrl();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void aes(Object object, boolean encryption) {
        Class<? extends Object> ormClass = object.getClass();
        Field[] fields = ormClass.getDeclaredFields();

        for (Field field : fields) {
            try {
                // get
                String fieldName = field.getName();
                String getMethodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                Method getMethod = ormClass.getDeclaredMethod(getMethodName);
                Object result = getMethod.invoke(object);

                // set
                String setMethodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                Method setMethod = ormClass.getDeclaredMethod(setMethodName, field.getType());
                setMethod.invoke(object, ("null".equals(result) || result == null) ? null :
                        encryption ? AESUtils.encryption((String) result) : AESUtils.decryption((String) result));
            } catch (Exception ignored) {
            }
        }
    }

    //TODO test
    public static void printAllPCStatus() {
        List<AuthPCModel> list = DatabaseUtil.findAll(AuthPCModel.class, null);
        if (list != null && list.size() > 0) {
            Log.w("test use", "##DB  data  start##");
            for (AuthPCModel model : list) {
                Log.w("test use", "pcuuid: " + model.getPcUuid() + "  " + "smartphonestatus:" + model.getValidFlg());

            }
            Log.w("test use", "##DB  data  end##");
        } else {
            Log.w("test use", "## DB  no data ##");
        }
    }
}

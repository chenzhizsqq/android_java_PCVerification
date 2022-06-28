package jp.co.unisys.authlocker.db.model;

import java.io.Serializable;

public class DeviceIdModel implements Serializable {

    public String deviceId;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public DeviceIdModel(String deviceId) {
        this.deviceId = deviceId;
    }
}

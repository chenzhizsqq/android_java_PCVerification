package jp.co.unisys.authlocker.response;

public class StatusFlagData {

    private String pcuuid;

    private String smartphonestatus;

    public String getPcuuid() {
        return pcuuid;
    }

    public void setPcuuid(String pcuuid) {
        this.pcuuid = pcuuid;
    }

    public String getSmartphonestatus() {
        return smartphonestatus;
    }

    public void setSmartphonestatus(String smartphonestatus) {
        this.smartphonestatus = smartphonestatus;
    }
}

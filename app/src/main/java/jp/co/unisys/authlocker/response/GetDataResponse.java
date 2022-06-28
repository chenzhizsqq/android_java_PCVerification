package jp.co.unisys.authlocker.response;

import java.util.List;

public class GetDataResponse  {

    private String code;

    private String message;

    /** アプリ起動認証Flag*/
    private String startupflag;

    /** 利用停止Flag*/
    private String usabilityflag;

    private String messageCode;

    private List<StatusFlagData> smartphoneList;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStartupflag() {
        return startupflag;
    }

    public void setStartupflag(String startupflag) {
        this.startupflag = startupflag;
    }

    public String getUsabilityflag() {
        return usabilityflag;
    }

    public void setUsabilityflag(String usabilityflag) {
        this.usabilityflag = usabilityflag;
    }

    public List<StatusFlagData> getSmartphoneList() {
        return smartphoneList;
    }

    public void setSmartphoneList(List<StatusFlagData> smartphoneList) {
        this.smartphoneList = smartphoneList;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }
}

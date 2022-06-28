package jp.co.unisys.authlocker.db.model;


import com.neusoft.convenient.database.annotation.Column;
import com.neusoft.convenient.database.annotation.PrimaryKey;
import com.neusoft.convenient.database.annotation.Table;

import java.io.Serializable;

/**
 * 認証PCの連携情報
 */
@Table(value = "t_auth_pc")
public class AuthPCModel implements Serializable {

    /**
     * PC識別番号(PC侧)
     */
    @PrimaryKey
    private String pcUuid;

    /**
     * 分散片UUID
     */
    @Column
    private String securityFileUUID;

    /**
     * 分散ファイル
     */
    @Column
    private String securityFile;

    /**
     * 有効フラグ
     * <p>
     * 0：　利用停止解除
     * 1：　利用停止
     * 2：  削除
     */
    @Column
    private String validFlg;

    public String getPcUuid() {
        return pcUuid;
    }

    public void setPcUuid(String pcUuid) {
        this.pcUuid = pcUuid;
    }

    public String getSecurityFileUUID() {
        return securityFileUUID;
    }

    public void setSecurityFileUUID(String securityFileUUID) {
        this.securityFileUUID = securityFileUUID;
    }

    public String getSecurityFile() {
        return securityFile;
    }

    public void setSecurityFile(String securityFile) {
        this.securityFile = securityFile;
    }

    public String getValidFlg() {
        return validFlg;
    }

    public void setValidFlg(String validFlg) {
        this.validFlg = validFlg;
    }
}

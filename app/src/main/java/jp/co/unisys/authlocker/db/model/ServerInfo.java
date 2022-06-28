package jp.co.unisys.authlocker.db.model;

import com.neusoft.convenient.database.annotation.PrimaryKey;
import com.neusoft.convenient.database.annotation.Table;

import java.io.Serializable;

@Table(value = "release_url")
public class ServerInfo implements Serializable {

    @PrimaryKey
    private String releaseUrl;

    public String getReleaseUrl() {
        return releaseUrl;
    }

    public void setReleaseUrl(String releaseUrl) {
        this.releaseUrl = releaseUrl;
    }
}

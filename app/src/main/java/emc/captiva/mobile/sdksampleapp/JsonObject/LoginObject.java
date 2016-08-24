package emc.captiva.mobile.sdksampleapp.JsonObject;

/**
 * Created by david on 8/24/16.
 */
public class LoginObject {

    final String culture;
    final String licenseKey;
    final String deviceId;
    final String applicationId;
    final String username;
    final String password;
    final String extraAuthInfo;

    public LoginObject(String culture, String licenseKey, String deviceId, String applicationId, String username, String password, String extraAuthInfo) {
        this.culture = culture;
        this.licenseKey = licenseKey;
        this.deviceId = deviceId;
        this.applicationId = applicationId;
        this.username = username;
        this.password = password;
        this.extraAuthInfo = extraAuthInfo;
    }
}

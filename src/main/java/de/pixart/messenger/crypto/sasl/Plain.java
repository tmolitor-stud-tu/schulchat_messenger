package de.pixart.messenger.crypto.sasl;

import android.content.Context;
import android.util.Base64;
import android.os.Build;

import java.nio.charset.Charset;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import de.pixart.messenger.R;
import de.pixart.messenger.BuildConfig;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.xml.TagWriter;
import de.pixart.messenger.utils.PhoneHelper;

public class Plain extends SaslMechanism {
	private Context mContext;
	
    public Plain(final TagWriter tagWriter, final Account account, final Context context) {
        super(tagWriter, account, null);
		mContext = context;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public String getMechanism() {
        return "PLAIN";
    }

    @Override
    public String getClientFirstMessage() {
        return getMessage(account.getUsername(), account.getPassword(), PhoneHelper.getAndroidId(mContext));
    }

    public static String getMessage(String username, String password, String deviceID) {
		password = createPassword(password, deviceID);
        final String message = '\u0000' + username + '\u0000' + password;
        return Base64.encodeToString(message.getBytes(Charset.defaultCharset()), Base64.NO_WRAP);
    }
    
    private static String createPassword(final String password, final String deviceID) {
        StringBuilder pw = new StringBuilder("");
        String deviceName = getDeviceName();
        try {
            pw.append(deviceID);
            pw.append("|");
            pw.append(deviceName);
            pw.append("|");
			pw.append(BuildConfig.VERSION_CODE);
			//hmac this and add the resulting has to our passwd string
			String hash = hash_hmac(hash_hmac(pw.toString(), password), BuildConfig.APP_SECRET);
            pw.append("|" + hash);
            return pw.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return password;
    }
    
    private static String hash_hmac(String text, String password) throws NoSuchAlgorithmException {
        Mac sha256_HMAC = null;
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(password.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            return bytesToString(sha256_HMAC.doFinal(text.getBytes()));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }
    
        private static String bytesToString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    private static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model;
        } else {
            return manufacturer + " " + model;
        }
    }

}
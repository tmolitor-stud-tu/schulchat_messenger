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

public class Kwo extends SaslMechanism {
    private Context mContext;

    public Kwo(final TagWriter tagWriter, final Account account, final Context context) {
        super(tagWriter, account, null);
        mContext = context;
    }

    @Override
    public int getPriority() {
        return 30;		//highest prio (SCRAM-SHA256 and EXTERNAL have prio 25, SCRAM-SHA1 has prio 20)
    }

    @Override
    public String getMechanism() {
        return "KWO";
    }

    @Override
    public String getClientFirstMessage() {
        return Base64.encodeToString(account.getUsername().getBytes(Charset.defaultCharset()), Base64.NO_WRAP);
    }
    
    @Override
    public String getResponse(final String challenge) throws AuthenticationException {
        if(challenge == null)
            return null;
        return getMessage(account.getPassword(), PhoneHelper.getAndroidId(mContext), new String(Base64.decode(challenge, Base64.DEFAULT)));
    }

    private static String getMessage(final String password, final String deviceID, final String challenge) {
        return Base64.encodeToString(createPassword(password, deviceID, challenge).getBytes(Charset.defaultCharset()), Base64.NO_WRAP);
    }
    
    private static String createPassword(final String password, final String deviceID, final String challenge) {
        StringBuilder pw = new StringBuilder("");
        String deviceName = getDeviceName();
        try {
            pw.append(deviceID);
            pw.append("|");
            pw.append(deviceName);
            pw.append("|");
            pw.append(BuildConfig.VERSION_CODE);
            //hmac all of this using several keys and add the resulting hash to our passwd string
            String hash = hash_hmac(hash_hmac(hash_hmac(pw.toString(), password), BuildConfig.APP_SECRET), challenge);
            pw.append("|" + hash);
            return pw.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return password;
    }
    
    private static String hash_hmac(String text, String key) throws NoSuchAlgorithmException {
        Mac sha256_HMAC = null;
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
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
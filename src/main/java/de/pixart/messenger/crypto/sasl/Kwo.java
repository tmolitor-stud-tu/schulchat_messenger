package de.pixart.messenger.crypto.sasl;

import android.util.Log;
import android.content.Context;
import android.util.Base64;
import android.os.Build;
import android.provider.Settings;
import android.bluetooth.BluetoothAdapter;

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
    private static final String LOGTAG = "KWO_SASL";
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
        return getMessage(account.getPassword(), mContext, new String(Base64.decode(challenge, Base64.DEFAULT)));
    }

    private static String getMessage(final String password, Context context, final String challenge) {
        return Base64.encodeToString(createPassword(password, context, challenge).getBytes(Charset.defaultCharset()), Base64.NO_WRAP);
    }
    
    private static String createPassword(final String password, Context context, final String challenge) {
        StringBuilder pw = new StringBuilder("");
        String deviceName = getDeviceName(context);
        try {
            pw.append(PhoneHelper.getAndroidId(context));
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

    //see https://medium.com/capital-one-tech/how-to-get-an-android-device-nickname-d5eab12f4ced
    private static String getDeviceName(Context context) {
        String name = "";
        
        //try to get user-settable device name, see https://medium.com/capital-one-tech/how-to-get-an-android-device-nickname-d5eab12f4ced
        //if more than one of this returns a name, the last one that is not null wins
        name = UseStringIfNotNull(name, Settings.System.getString(context.getContentResolver(), "bluetooth_name"));
        name = UseStringIfNotNull(name, Settings.Secure.getString(context.getContentResolver(), "bluetooth_name"));
        name = UseStringIfNotNull(name, BluetoothAdapter.getDefaultAdapter().getName());
        name = UseStringIfNotNull(name, Settings.System.getString(context.getContentResolver(), "device_name"));
        //the next one is taken from a commentary int he medium.com blog post, not from the main article
        name = UseStringIfNotNull(name, Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME));
        name = UseStringIfNotNull(name, Settings.Secure.getString(context.getContentResolver(), "lock_screen_owner_info"));
        
        //add manufacturer and build
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return name!=null && name.length()>0 ? name + " (" + model + ")" : model;
        } else {
            return name!=null && name.length()>0 ? name + " (" + manufacturer + " " + model + ")" : manufacturer + " " + model;
        }
    }
    
    private static String UseStringIfNotNull(String oldstr, String newstr)
    {
        Log.d(LOGTAG, "DEVIE NAME: '"+(oldstr == null || oldstr.length() == 0 ? "" : oldstr)+"' --> '"+(newstr == null || newstr.length() == 0 ? "" : newstr)+"'");
        return newstr == null || newstr.length() == 0 ? oldstr : newstr;
    }

}
package de.pixart.messenger.ui;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.utils.FirstStartManager;
import de.pixart.messenger.utils.PhoneHelper;
/*
import de.pixart.messenger.ui.util.IntroHelper;
import de.pixart.messenger.utils.InstallReferrerUtils;
import de.pixart.messenger.utils.SignupUtils;
import de.pixart.messenger.utils.XmppUri;
import me.drakeet.support.toast.ToastCompat;
*/
import rocks.xmpp.addr.Jid;

import static de.pixart.messenger.utils.PermissionUtils.allGranted;

public class WelcomeActivity extends XmppActivity {

    private static final int REQUEST_SCAN_QR_CODE = 0x54fb;
    private static final int REQUEST_CAMERA_PERMISSIONS_TO_SCAN = 0x6774;
    private String domain;
    Button scanQRCode = null;
    private static final String algo = "HmacSHA256";

    @Override
    protected void refreshUiReal() {
    }

    @Override
    void onBackendConnected() {
    }

    @Override
    public void onStart() {
        super.onStart();
        final int theme = findTheme();
        if (this.mTheme != theme) {
            recreate();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent != null) {
            setIntent(intent);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        setSupportActionBar(findViewById(R.id.toolbar));
        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(false);
            ab.setDisplayHomeAsUpEnabled(false);
        }

        scanQRCode = findViewById(R.id.scan_qr_code);
        final TextView scanQRCodeText = findViewById(R.id.scan_qr_code_text);
        if (hasCameraPermission(REQUEST_CAMERA_PERMISSIONS_TO_SCAN)) {
            scanQRCode.setVisibility(View.VISIBLE);
        } else {
            scanQRCodeText.setText(R.string.no_camera_permission);
        }
        scanQRCode.setOnClickListener(v -> {
            scanQRCode();
			//Toast.makeText(this, R.string.error_closed_beta, Toast.LENGTH_SHORT).show();
        });
    }

    private void scanQRCode() {
        final Intent intent = new Intent(WelcomeActivity.this, ScanActivity.class);
        startActivityForResult(intent, REQUEST_SCAN_QR_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, requestCode, intent);
        if (requestCode == REQUEST_SCAN_QR_CODE && resultCode == RESULT_OK) {
            String result = intent.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
            if (result != null) {
                if (getRootDomain(result).equalsIgnoreCase(Config.MAGIC_CREATE_DOMAIN)) {
                    String[] strings = result.split("&");
                    final String domain = strings[0].substring(("domain=").length());
                    final String username = strings[1].substring(("user=").length());
                    final String password = strings[2].substring(("token=").length());
                    addAccount(domain, username, createPassword(password));
                } else {
                    Toast.makeText(this, R.string.error_scanning_QR_code, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.error_scanning_QR_code, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.error_scanning_QR_code, Toast.LENGTH_SHORT).show();
        }
    }

    private String createPassword(final String password) {
        StringBuilder pw = new StringBuilder("");
        String deviceID = PhoneHelper.getAndroidId(this);
        String deviceName = getDeviceName();
        try {
            pw.append(deviceID);
            pw.append("|");
            pw.append(deviceName);
            pw.append("|");
            pw.append(hash_hmac(algo, hash_hmac(algo, deviceID + "|" + deviceName, password), getString(R.string.app_secret)));
            return pw.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return password;
    }

    public static String hash_hmac(String algo, String text, String password) throws NoSuchAlgorithmException {
        Mac sha256_HMAC = null;
        try {
            sha256_HMAC = Mac.getInstance(algo);
            SecretKeySpec secret_key = new SecretKeySpec(password.getBytes(), algo);
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

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model;
        } else {
            return manufacturer + " " + model;
        }
    }

    private String getRootDomain(final String result) {
        String fulldomain = "";
        String rootdomain = "";
        if (result.startsWith("domain=")) {
            String[] results = result.split("&");
            fulldomain = results[0].substring(("domain=").length());
            rootdomain = fulldomain.substring(fulldomain.indexOf(".") + 1);
        }
        return rootdomain;
    }

    private void addAccount(final String domain, final String username, final String password) {
        Jid jid = Jid.of(username.toLowerCase(), domain, null);
        if (!jid.getEscapedLocal().equals(jid.getLocal()) || username.length() < 3) {
            Toast.makeText(this, R.string.error_scanning_QR_code, Toast.LENGTH_SHORT).show();
        } else {
            Account account = xmppConnectionService.findAccountByJid(jid);
            if (account == null) {
                account = new Account(jid, password);
                account.setOption(Account.OPTION_REGISTER, false);
                account.setOption(Account.OPTION_DISABLED, false);
                account.setOption(Account.OPTION_MAGIC_CREATE, false);
                xmppConnectionService.createAccount(account);
                FirstStartManager firstStartManager = new FirstStartManager(this);
                if (firstStartManager.isFirstTimeLaunch()) {
                    Intent intent = new Intent(this, SetSettingsActivity.class);
                    intent.putExtra("setup", true);
                    startActivity(intent);
                    overridePendingTransition(R.animator.fade_in, R.animator.fade_out);
                } else {
                    Intent intent = new Intent(this, PublishProfilePictureActivity.class);
                    intent.putExtra(PublishProfilePictureActivity.EXTRA_ACCOUNT, account.getJid().asBareJid().toEscapedString());
                    intent.putExtra("setup", true);
                    startActivity(intent);
                    overridePendingTransition(R.animator.fade_in, R.animator.fade_out);
                }
            }
        }
        finish();
    }

    public static void launch(AppCompatActivity activity) {
        Intent intent = new Intent(activity, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            if (allGranted(grantResults)) {
                switch (requestCode) {
                    case REQUEST_CAMERA_PERMISSIONS_TO_SCAN:
                        scanQRCode.setVisibility(View.VISIBLE);
                        break;
                }
            } else {
                Toast.makeText(this, R.string.no_camera_permission, Toast.LENGTH_SHORT).show();
            /*
	    } else if (Arrays.asList(permissions).contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ToastCompat.makeText(this, R.string.no_storage_permission, Toast.LENGTH_SHORT).show();
            }
        }
        if (readGranted(grantResults, permissions)) {
            if (xmppConnectionService != null) {
                xmppConnectionService.restartFileObserver();
            */
	    }
        }
    }
}

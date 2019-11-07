package de.pixart.messenger.ui;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import de.pixart.messenger.Config;
import de.pixart.messenger.R;
import de.pixart.messenger.entities.Account;
import rocks.xmpp.addr.Jid;

import static de.pixart.messenger.utils.PermissionUtils.allGranted;

public class WelcomeActivity extends XmppActivity {

    private static final int REQUEST_SCAN_QR_CODE = 0x54fb;
    private static final int REQUEST_CAMERA_PERMISSIONS_TO_SCAN = 0x6774;
    Button scanQRCode = null;

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
                if (result.startsWith("domain=kurswahl-online.de")) {
                    String[] strings = result.split("&");
                    /*
                    strings[0]; //domain=kurswahl-online.de
                    strings[1]; //user=b011e554-c9ee-4a12-a9d0-24c4ab8b21e7
                    strings[2]; //token=571bb231023b209d9536f641adf647a5006ce5d597965266f21161599b28eb0c
                    */
                    final String domain = strings[0].substring(("domain=").length());
                    final String username = strings[1].substring(("user=").length());
                    final String password = strings[2].substring(("token=").length());
                    Log.d(Config.LOGTAG, "ScanCode domain " + domain + " user " + username + " pw " + password);
                    addAccount(domain, username, password);
                }
            }
        }
    }

    private void addAccount(final String domain, final String username, final String password) {
        Jid jid = Jid.of(username.toLowerCase(), domain, null);
        if (!jid.getEscapedLocal().equals(jid.getLocal()) || username.length() < 3) {
            Toast.makeText(this, R.string.error_scanning_QR_code, Toast.LENGTH_SHORT).show();
        } else {
            Account account = xmppConnectionService.findAccountByJid(jid);
            if (account == null) {
                account = new Account(jid, password);
                account.setOption(Account.OPTION_REGISTER, true);
                account.setOption(Account.OPTION_DISABLED, true);
                account.setOption(Account.OPTION_MAGIC_CREATE, true);
                xmppConnectionService.createAccount(account);
            }
            Intent intent = new Intent(WelcomeActivity.this, EditAccountActivity.class);
            intent.putExtra("jid", account.getJid().asBareJid().toString());
            intent.putExtra("init", true);
            intent.putExtra("existing", false);
            intent.putExtra("useownprovider", false);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            StartConversationActivity.addInviteUri(intent, getIntent());
            startActivity(intent);
        }
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
            }
        }
    }
}
package de.pixart.messenger.ui;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.text.Editable;
import android.view.View;
import android.widget.Toast;

import java.util.Collections;

import de.pixart.messenger.R;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.entities.Blockable;
import de.pixart.messenger.entities.ListItem;
import de.pixart.messenger.entities.RawBlockable;
import de.pixart.messenger.ui.interfaces.OnBackendConnected;
import de.pixart.messenger.xmpp.OnUpdateBlocklist;
import rocks.xmpp.addr.Jid;

public class BlocklistActivity extends AbstractSearchableListItemActivity implements OnUpdateBlocklist {
    private Account account = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getListView().setOnItemLongClickListener((parent, view, position, id) -> {
            BlockContactDialog.show(BlocklistActivity.this, (Blockable) getListItems().get(position));
            return true;
        });
    }

    @Override
    public void onBackendConnected() {
        for (final Account account : xmppConnectionService.getAccounts()) {
            if (account.getJid().toString().equals(getIntent().getStringExtra(EXTRA_ACCOUNT))) {
                this.account = account;
                break;
            }
        }
        filterContacts();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_DIALOG);
        if (fragment instanceof OnBackendConnected) {
            ((OnBackendConnected) fragment).onBackendConnected();
        }
    }

    @Override
    protected void filterContacts(final String needle) {
        getListItems().clear();
        if (account != null) {
            for (final Jid jid : account.getBlocklist()) {
                ListItem item;
                if (jid.isFullJid()) {
                    item = new RawBlockable(account, jid);
                } else {
                    item = account.getRoster().getContact(jid);
                }
                if (item.match(this, needle)) {
                    getListItems().add(item);
                }
            }
            Collections.sort(getListItems());
        }
        getListItemAdapter().notifyDataSetChanged();
    }

    protected void refreshUiReal() {
        final Editable editable = getSearchEditText().getText();
        if (editable != null) {
            filterContacts(editable.toString());
        } else {
            filterContacts();
        }
    }

    @Override
    public void OnUpdateBlocklist(final OnUpdateBlocklist.Status status) {
        refreshUi();
    }
}

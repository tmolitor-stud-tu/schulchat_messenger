package de.pixart.messenger.ui.adapter;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import de.pixart.messenger.R;
import de.pixart.messenger.databinding.UserPreviewBinding;
import de.pixart.messenger.entities.MucOptions;
import de.pixart.messenger.entities.Account;
import de.pixart.messenger.entities.Contact;
import de.pixart.messenger.entities.Conversation;
import de.pixart.messenger.ui.XmppActivity;
import de.pixart.messenger.ui.util.AvatarWorkerTask;
import de.pixart.messenger.ui.util.MucDetailsContextMenuHelper;
import rocks.xmpp.addr.Jid;


public class UserPreviewAdapter extends ListAdapter<MucOptions.User, UserPreviewAdapter.ViewHolder> implements View.OnCreateContextMenuListener {

    private MucOptions.User selectedUser = null;

    public UserPreviewAdapter() {
        super(UserAdapter.DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
        return new ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(viewGroup.getContext()), R.layout.user_preview, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        final MucOptions.User user = getItem(position);
        AvatarWorkerTask.loadAvatar(user, viewHolder.binding.avatar, R.dimen.media_size);
        viewHolder.binding.getRoot().setOnClickListener(v -> {
            final XmppActivity activity = XmppActivity.find(v);
			final Conversation conversation = user.getConversation();
			final Jid jid = user.getRealJid();
			final Account account = conversation.getAccount();
			final Contact contact = jid == null ? null : account.getRoster().getContact(jid);
            if (activity != null && contact != null) {
				activity.switchToContactDetails(contact, null);
            }
            /*if (activity != null) {
                activity.highlightInMuc(user.getConversation(), user.getName());
            }*/
        });
        viewHolder.binding.getRoot().setOnCreateContextMenuListener(this);
        viewHolder.binding.getRoot().setTag(user);
        viewHolder.binding.getRoot().setOnLongClickListener(v -> {
            selectedUser = user;
            return false;
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MucDetailsContextMenuHelper.onCreateContextMenu(menu, v);
    }

    public MucOptions.User getSelectedUser() {
        return selectedUser;
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        private final UserPreviewBinding binding;

        private ViewHolder(UserPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
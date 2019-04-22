package co.chatsdk.ui.custom.inbox.handlers;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import co.chatsdk.core.interfaces.MessageDisplayHandler;
import co.chatsdk.ui.R;

public abstract class ManchAbstractMessageDisplayHandler implements MessageDisplayHandler {

    protected View row(boolean isReply, Activity activity) {
        View row;
        LayoutInflater inflater = LayoutInflater.from(activity);
        if (isReply) {
            row = inflater.inflate(R.layout.inbox_row_message_reply, null);
        } else {
            row = inflater.inflate(R.layout.inbox_row_message_me, null);
        }
        return row;
    }
}

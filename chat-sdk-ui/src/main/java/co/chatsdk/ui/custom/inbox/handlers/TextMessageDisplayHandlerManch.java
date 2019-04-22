package co.chatsdk.ui.custom.inbox.handlers;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import co.chatsdk.ui.custom.inbox.viewholder.TextMessageViewHolder;

import co.chatsdk.core.base.AbstractMessageViewHolder;
import co.chatsdk.core.dao.Message;

public class TextMessageDisplayHandlerManch extends ManchAbstractMessageDisplayHandler {

    @Override
    public void updateMessageCellView(Message message, AbstractMessageViewHolder viewHolder, Context context) {

    }

    @Override
    public String displayName(Message message) {
        return message.getText();
    }

    @Override
    public AbstractMessageViewHolder newViewHolder(boolean isReply, Activity activity) {
        View row = row(isReply, activity);
        return new TextMessageViewHolder(row, activity);
    }
}

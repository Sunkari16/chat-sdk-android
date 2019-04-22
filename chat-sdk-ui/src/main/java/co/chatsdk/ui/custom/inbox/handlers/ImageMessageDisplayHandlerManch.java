package co.chatsdk.ui.custom.inbox.handlers;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import co.chatsdk.core.base.AbstractMessageViewHolder;
import co.chatsdk.core.dao.Message;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.ui.R;
import co.chatsdk.ui.custom.inbox.viewholder.ImageMessageViewHolder;

public class ImageMessageDisplayHandlerManch extends ManchAbstractMessageDisplayHandler {
    @Override
    public void updateMessageCellView(Message message, AbstractMessageViewHolder viewHolder, Context context) {

    }

    @Override
    public String displayName(Message message) {
        return ChatSDK.shared().context().getString(R.string.image_message);
    }

    @Override
    public AbstractMessageViewHolder newViewHolder(boolean isReply, Activity activity) {
        View row = row(isReply, activity);
        return new ImageMessageViewHolder(row, activity);
    }
}

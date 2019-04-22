package co.chatsdk.ui.custom.inbox;

import android.app.Activity;
import android.content.Context;

import co.chatsdk.core.base.AbstractMessageViewHolder;
import co.chatsdk.core.dao.Message;

/**
 * Created by ben on 10/11/17.
 */

public interface MessageDisplayHandler {

    void updateMessageCellView (Message message, AbstractMessageViewHolder viewHolder, Context context);
    String displayName (Message message);
    AbstractMessageViewHolder newViewHolder(boolean isReply, Activity activity);

}

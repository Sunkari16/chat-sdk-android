package co.chatsdk.ui.custom.inbox.viewholder;

import android.app.Activity;
import android.view.View;

import co.chatsdk.ui.custom.inbox.BaseMessageViewHolder;
import co.chatsdk.ui.custom.inbox.ImageMessageOnClickHandler;

import co.chatsdk.core.dao.Keys;
import co.chatsdk.core.dao.Message;


public class ImageMessageViewHolder extends BaseMessageViewHolder {
    public ImageMessageViewHolder(View itemView, Activity activity) {
        super(itemView, activity);
    }

    @Override
    public void setMessage(Message message) {
        super.setMessage(message);
        setImageHidden(false);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        if (message != null) {
            ImageMessageOnClickHandler.onClick(activity, v, getImageURL());
        }
    }

    public String getImageURL() {
        return message.stringForKey(Keys.MessageImageURL);
    }
}

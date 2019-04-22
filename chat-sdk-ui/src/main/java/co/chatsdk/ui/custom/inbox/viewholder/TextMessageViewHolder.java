package co.chatsdk.ui.custom.inbox.viewholder;

import android.app.Activity;
import android.view.View;

import co.chatsdk.ui.custom.inbox.BaseMessageViewHolder;

import co.chatsdk.core.dao.Message;

public class TextMessageViewHolder extends BaseMessageViewHolder {

    public TextMessageViewHolder(View itemView, Activity activity) {
        super(itemView, activity);
    }

    @Override
    public void setMessage(Message message) {
        super.setMessage(message);

        messageTextView.setText(message.getText() == null ? "" : message.getText());
        setBubbleHidden(false);
        setTextHidden(false);
    }
}
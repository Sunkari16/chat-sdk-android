package co.chatsdk.ui.custom.inbox.viewholder;

import android.app.Activity;
import android.util.Patterns;
import android.view.View;

import co.chatsdk.core.dao.Message;
import co.chatsdk.ui.custom.inbox.BaseMessageViewHolder;
import co.chatsdk.ui.custom.inbox.utils.Utility;
import co.chatsdk.ui.utils.InternalLinkMovementMethod;
import timber.log.Timber;

public class TextMessageViewHolder extends BaseMessageViewHolder {

    public TextMessageViewHolder(View itemView, Activity activity) {
        super(itemView, activity);
    }

    @Override
    public void setMessage(Message message) {
        super.setMessage(message);
        messageTextView.setText(message.getText() == null ? "" : message.getText());
        InternalLinkMovementMethod internalLinkMovementMethod = new InternalLinkMovementMethod(new InternalLinkMovementMethod.OnLinkClickedListener() {
            @Override
            public boolean onLinkClicked(String url) {
                if (Patterns.WEB_URL.matcher(url).matches()) {
                    Utility.openLinksInChrome(activity, url);
                    return true;
                }
                Timber.v("comment link clicked");
                return false;
            }
        });
        messageTextView.setMovementMethod(internalLinkMovementMethod);
        setBubbleHidden(false);
        setTextHidden(false);
    }
}
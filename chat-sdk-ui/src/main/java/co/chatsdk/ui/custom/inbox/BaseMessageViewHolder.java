package co.chatsdk.ui.custom.inbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.util.Linkify;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import co.chatsdk.core.base.AbstractMessageViewHolder;
import co.chatsdk.core.dao.Message;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.types.MessageSendStatus;
import co.chatsdk.core.utils.CrashReportingCompletableObserver;
import co.chatsdk.ui.R;

public class BaseMessageViewHolder extends AbstractMessageViewHolder {

    public static final String MoreThanYearFormat = "MM/yy";
    public static final String MoreThanDayFormat = "MMM dd";

    protected SimpleDraweeView avatarImageView;
    protected TextView messageTextView;

    public BaseMessageViewHolder(View itemView, Activity activity) {
        super(itemView, activity);
        messageTextView = itemView.findViewById(R.id.tvMessageContent);
        avatarImageView = itemView.findViewById(R.id.sdvUserProfile);

        itemView.setOnClickListener(this::onClick);

        itemView.setOnLongClickListener(this::onLongClick);

        // Enable linkify
        messageTextView.setAutoLinkMask(Linkify.ALL);

        avatarImageView.setOnClickListener(this::onProfileClick);
    }

    @Override
    public void showProgressBar() {

    }

    @Override
    public void showProgressBar(float progress) {

    }

    public void onClick(View v) {
        if (onClickListener != null) {
            onClickListener.onClick(v);
        }
    }

    public void onProfileClick(View view) {
        ChatSDK.ui().startProfileActivity(activity,message.getSender().getEntityID());
    }

    public boolean onLongClick(View v) {
        if (onLongClickListener != null) {
            onLongClickListener.onLongClick(v);
        } else if (message != null && message.getSender().isMe()) {

            Context context = v.getContext();

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete");

            // Set up the buttons
            builder.setPositiveButton("ok", (dialog, which) -> {
                try {
                    ChatSDK.thread().deleteMessage(message).subscribe(new CrashReportingCompletableObserver());
                } catch (NoSuchMethodError e) {
                    ChatSDK.logError(e);
                }
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

            builder.show();

        }
        return false;
    }

    public void setMessage(Message message) {
        super.setMessage(message);

        setBubbleHidden(true);
        setTextHidden(true);
        setIconHidden(true);
        setImageHidden(true);

        float alpha = message.getMessageStatus() == MessageSendStatus.Sent || message.getMessageStatus() == MessageSendStatus.Delivered ? 1.0f : 0.7f;
        setAlpha(alpha);

        avatarImageView.setImageURI(message.getSender().getAvatarURL());

        if (message.getSender().isMe()) {
            messageTextView.setBackgroundResource(R.drawable.bg_chat_me);
        } else {
            messageTextView.setBackgroundResource(R.drawable.bg_chat_reply);
        }
    }

    public void setAlpha(float alpha) {
        messageTextView.setAlpha(alpha);
    }

    @Override
    public LinearLayout getExtraLayout() {
        return null;
    }

    public void hideProgressBar() {

    }

    public void setIconSize(int width, int height) {

    }

    public void setImageSize(int width, int height) {

    }

    public void setBubbleHidden(boolean hidden) {

    }

    public void setIconHidden(boolean hidden) {

    }

    public void setImageHidden(boolean hidden) {
    }

    public void setTextHidden(boolean hidden) {

    }

    public View viewForClassType(Class classType) {
        return null;
    }

    protected SimpleDateFormat getTimeFormat(Message message) {

        Date curTime = new Date();
        long interval = (curTime.getTime() - message.getDate().toDate().getTime()) / 1000L;

        String dateFormat = ChatSDK.config().messageTimeFormat;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.getDefault());

        // More then a day ago
        if (interval < 3600 * 24) {
            return simpleDateFormat;
        } else if (interval < 3600 * 24 * 365) {
            simpleDateFormat.applyPattern(dateFormat + " " + MoreThanDayFormat);
        } else {
            simpleDateFormat.applyPattern(dateFormat + " " + MoreThanYearFormat);
        }
        return simpleDateFormat;
    }

}

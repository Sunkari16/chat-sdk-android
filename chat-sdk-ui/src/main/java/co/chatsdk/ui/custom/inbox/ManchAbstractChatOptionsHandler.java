package co.chatsdk.ui.custom.inbox;

import android.content.Intent;

import java.lang.ref.WeakReference;

import co.chatsdk.core.interfaces.ChatOption;
import co.chatsdk.core.interfaces.ChatOptionsDelegate;
import co.chatsdk.core.interfaces.ChatOptionsHandler;
import co.chatsdk.core.session.ChatSDK;

/**
 * Created by ben on 10/11/17.
 */

public abstract class ManchAbstractChatOptionsHandler implements ChatOptionsHandler {

    protected WeakReference<ChatOptionsDelegate> delegate;

    public ManchAbstractChatOptionsHandler(ChatOptionsDelegate delegate) {
        this.delegate = new WeakReference<>(delegate);
    }

    public void executeOption(ChatOption option) {
        if (delegate != null) {
            delegate.get().executeChatOption(option);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (ChatOption option : ChatSDK.ui().getChatOptions()) {

        }
    }

}

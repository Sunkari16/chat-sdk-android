package co.chatsdk.ui.custom.inbox.options;

import android.app.Activity;

import co.chatsdk.core.dao.Thread;
import co.chatsdk.core.interfaces.ChatOption;
import co.chatsdk.core.types.ChatOptionType;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * Created by ben on 10/11/17.
 */

public class BaseChatOption implements ChatOption {

    protected Action action;
    protected String title;
    protected Integer iconResourceId;
    protected ChatOptionType type;
    protected Disposable activityResultDisposable = null;

    public BaseChatOption (String title, Integer iconResourceId, Action action, ChatOptionType type) {
        this.action = action;
        this.title = title;
        this.iconResourceId = iconResourceId;
        this.type = type;
    }

    public BaseChatOption (String title, Action action, ChatOptionType type) {
        this(title, null, action, type);
    }

    @Override
    public int getIconResourceId() {
        return iconResourceId;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Observable<?> execute(Activity activity, Thread thread) {
        if(action != null) {
            return action.execute(activity, thread);
        }
        return Completable.complete().toObservable();
    }

    @Override
    public ChatOptionType getType() {
        return type;
    }

    public interface Action {
        Observable<?> execute(Activity activity, Thread thread);
    }

    protected void dispose () {
        if(activityResultDisposable != null) {
            activityResultDisposable.dispose();
            activityResultDisposable = null;
        }
    }
}

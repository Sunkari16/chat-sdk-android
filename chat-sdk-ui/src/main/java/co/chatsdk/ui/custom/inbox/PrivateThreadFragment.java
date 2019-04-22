package co.chatsdk.ui.custom.inbox;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.MenuItem;

import java.util.List;
import java.util.concurrent.Callable;

import co.chatsdk.core.dao.Thread;
import co.chatsdk.core.events.NetworkEvent;
import co.chatsdk.core.interfaces.ThreadType;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.ui.R;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;

/**
 * private thread view showing the user in contact
 */
public class PrivateThreadFragment extends BaseThreadFragment {

    protected static PrivateThreadFragment getInstance() {
        return new PrivateThreadFragment();
    }

    @SuppressLint("CheckResult")
    @Override
    public void initViews() {
        super.initViews();

        adapter.onLongClickObservable().subscribe(thread -> showDialog(getContext(), "",
                 getResources().getString(R.string.chat_alert_delete_thread),
                getResources().getString(R.string.chat_button_cancel),
                getResources().getString(R.string.delete), null, () -> {
                    ChatSDK.thread().deleteThread(thread)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new CompletableObserver() {
                                @Override
                                public void onSubscribe(Disposable d) {
                                }

                                @Override
                                public void onComplete() {
                                    adapter.clearData();
                                    reloadData();
//                                    ToastHelper.show(getContext(), R.string.delete_thread_success_toast);
                                }

                                @Override
                                public void onError(Throwable e) {
//                                    ToastHelper.show(getContext(),R.string.delete_thread_fail_toast);
                                }
                            });
                    return null;
                }));
    }

    @Override
    protected Predicate<NetworkEvent> mainEventFilter() {
        return NetworkEvent.filterPrivateThreadsUpdated();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        /* Cant use switch in the library*/
        int id = item.getItemId();

//        if (id == R.id.action_chat_sdk_add) {
//            TODO here start add contacts activity
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected List<Thread> getThreads() {
        return ChatSDK.thread().getThreads(ThreadType.Private);
    }

    private void showDialog(Context context, String title, String body, String buttonNeg, String buttonPos, Callable neg, Callable pos) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        // set title if not null
        if (title != null && !title.equals("")) {
            alertDialogBuilder.setTitle(title);
        }
        // set dialog message
        alertDialogBuilder
                .setMessage(body)
                .setCancelable(false)
                .setPositiveButton(buttonPos, (dialog, id) -> {
                    if (pos != null)
                        try {
                            pos.call();
                        } catch (Exception e) {
                            ChatSDK.logError(e);
                        }
                    dialog.dismiss();
                })
                .setNegativeButton(buttonNeg, (dialog, id) -> {
                    // if this button is clicked, just close
                    // the dialog box and do nothing
                    if (neg != null)
                        try {
                            neg.call();
                        } catch (Exception e) {
                            ChatSDK.logError(e);
                        }

                    dialog.cancel();
                });
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }

}

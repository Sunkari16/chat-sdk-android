package co.chatsdk.ui.custom.inbox;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import co.chatsdk.core.dao.Keys;
import co.chatsdk.core.dao.Message;
import co.chatsdk.core.dao.Thread;
import co.chatsdk.core.dao.User;
import co.chatsdk.core.events.EventType;
import co.chatsdk.core.events.NetworkEvent;
import co.chatsdk.core.handlers.TypingIndicatorHandler;
import co.chatsdk.core.interfaces.ChatOption;
import co.chatsdk.core.interfaces.ChatOptionsDelegate;
import co.chatsdk.core.interfaces.ThreadType;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.types.ChatOptionType;
import co.chatsdk.core.types.MessageSendProgress;
import co.chatsdk.core.types.MessageSendStatus;
import co.chatsdk.core.types.ReadStatus;
import co.chatsdk.core.utils.CrashReportingCompletableObserver;
import co.chatsdk.core.utils.CrashReportingObserver;
import co.chatsdk.core.utils.DisposableList;
import co.chatsdk.core.utils.StringChecker;
import co.chatsdk.core.utils.Strings;
import co.chatsdk.ui.R;
import co.chatsdk.ui.custom.inbox.utils.ToastHelper;
import co.chatsdk.ui.main.BaseActivity;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * a base view for showing the chat view android
 */
public class PrivateChatActivity extends BaseActivity implements ChatOptionsDelegate, View.OnKeyListener, TextView.OnEditorActionListener {

    public static final int ADD_USERS = 103;
    public static final int SHOW_DETAILS = 200;

    // Should we remove the user from the public chat when we stop this activity?
    // If we are showing a temporary screen like the sticker message screen
    // this should be set to no
    protected boolean removeUserFromChatOnExit = true;

    protected enum ListPosition {
        Top, Current, Bottom
    }

    protected static boolean enableTrace = false;

    /**
     * The key to get the thread long id.
     */
    public static final String LIST_POS = "list_pos";

    protected EditText textInputView;
    protected RecyclerView recyclerView;
    protected PrivateChatAdapter messageListAdapter;
    protected Thread thread;
    protected TextView subtitleTextView;
    protected ImageView btnSend;

    protected DisposableList disposableList = new DisposableList();
    protected Disposable typingTimerDisposable;

    protected ProgressBar progressBar;

    protected int listPos = -1;

    protected Bundle bundle;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_chat);
        initViews();

        if (!updateThreadFromBundle(savedInstanceState)) {
            return;
        }

        if (savedInstanceState != null) {
            listPos = savedInstanceState.getInt(LIST_POS, -1);
            savedInstanceState.remove(LIST_POS);
        }

        if (thread.typeIs(ThreadType.Private1to1) && thread.otherUser() != null && ChatSDK.lastOnline() != null) {
            ChatSDK.lastOnline().getLastOnline(thread.otherUser())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((date, throwable) -> {
                        if (throwable == null && date != null) {
                            Locale current = getResources().getConfiguration().locale;
                            PrettyTime pt = new PrettyTime(current);
                            setSubtitleText(String.format(getString(R.string.chat_last_seen), pt.format(date)));
                        }
                    });
        }

        initActionBar();

        // If the context is just been created we load regularly, else we load and retain position
        loadMessages(true, -1, PrivateChatActivity.ListPosition.Bottom);

        setChatState(TypingIndicatorHandler.State.active);

        if (enableTrace) {
            android.os.Debug.startMethodTracing("chat");
        }

    }


    protected void initActionBar() {
        String displayName = Strings.nameForThread(thread);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });
        TextView title = findViewById(R.id.tvToolbarTitle);
        title.setText(displayName);
        SimpleDraweeView userView = findViewById(R.id.sdvUserProfile);
        ThreadImageBuilder.load(userView, thread);

        //profile click listener
        userView.setOnClickListener(view -> {
            for (User user : thread.getUsers()) {
                if (!user.isMe()) {
                    ChatSDK.ui().startProfileActivity(this, user.getEntityID());
                    break;
                }
            }
        });
    }

    protected void initViews() {
        textInputView = findViewById(R.id.etSearchQuery);

        progressBar = findViewById(R.id.pbChatLoader);

        subtitleTextView = findViewById(R.id.tvSubtitle);

        btnSend = findViewById(R.id.ivSearchIcon);

        final SwipeRefreshLayout mSwipeRefresh = findViewById(R.id.srlRefreshList);

        mSwipeRefresh.setOnRefreshListener(() -> {

            List<MessageListItem> items = messageListAdapter.getMessageItems();
            Message firstMessage = null;
            if (items.size() > 0) {
                firstMessage = items.get(0).message;
            }

            disposableList.add(ChatSDK.thread().loadMoreMessagesForThread(firstMessage, thread)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((messages, throwable) -> {
                        if (throwable == null) {
                            if (messages.size() < 2) {
                                showToastMsg(R.string.chat_no_more_msg);
                            } else {
                                for (Message m : messages) {
                                    messageListAdapter.addRow(m, false, false);
                                }
                                messageListAdapter.sortAndNotify();
                                recyclerView.getLayoutManager().scrollToPosition(messages.size());
                            }
                        }
                        mSwipeRefresh.setRefreshing(false);
                    }));
        });

        recyclerView = findViewById(R.id.rvChatList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                scrollListTo(ListPosition.Bottom, false));

        if (messageListAdapter == null) {
            messageListAdapter = new PrivateChatAdapter(PrivateChatActivity.this);
        }

        recyclerView.setAdapter(messageListAdapter);

        //button send action
        btnSend.setOnClickListener(v -> sendMessage(getMessageText(), true));

        //edit text layout for editing and other events

        textInputView.setOnEditorActionListener(this);
        textInputView.setOnKeyListener(this);
        textInputView.setInputType(InputType.TYPE_CLASS_TEXT);

        textInputView.setOnFocusChangeListener((view, focus) -> {
            if (focus) {
                onKeyboardShow();
            } else {
                onKeyboardHide();
            }
        });

        textInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                startTyping();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    /**
     * Send text message
     *
     * @param text          to send.
     * @param clearEditText if true clear the message edit text.
     */
    public void sendMessage(String text, boolean clearEditText) {

        if (StringUtils.isEmpty(text) || StringUtils.isBlank(text)) {
            return;
        }

        handleMessageSend(ChatSDK.thread().sendMessageWithText(text.trim(), thread));

        if (clearEditText && textInputView != null) {
            textInputView.getText().clear();
        }

        stopTyping(false);
        scrollListTo(PrivateChatActivity.ListPosition.Bottom, false);
    }

    protected void handleMessageSend(Observable<MessageSendProgress> observable) {
        observable.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<MessageSendProgress>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        //disposableList.add(d);
                    }

                    @Override
                    public void onNext(@NonNull MessageSendProgress messageSendProgress) {
                        Timber.d("Message Status: " + messageSendProgress.getStatus());
                        // It's best not to sort here because then we are just adding the message
                        // to the bottom of the list. We only sort after the message has also been
                        // received from Firebase so the datestamp is also correct
                        if (messageListAdapter.addRow(messageSendProgress.message, false, true, messageSendProgress.uploadProgress)) {
                            scrollListTo(PrivateChatActivity.ListPosition.Bottom, false);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        ChatSDK.logError(e);
                        ToastHelper.show(getApplicationContext(), e.getLocalizedMessage());
                    }

                    @Override
                    public void onComplete() {
                        messageListAdapter.notifyDataSetChanged();
                        scrollListTo(PrivateChatActivity.ListPosition.Bottom, false);
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the thread ID
        if (thread != null) {
            outState.putString(Keys.THREAD_ENTITY_ID, thread.getEntityID());
        }

        // Save the list position
        outState.putInt(LIST_POS, layoutManager().findFirstVisibleItemPosition());
    }

    protected LinearLayoutManager layoutManager() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        return layoutManager;
    }

    @Override
    protected void onStart() {
        super.onStart();

        disposableList.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.MessageAdded, EventType.ThreadReadReceiptUpdated, EventType.MessageRemoved))
                .filter(NetworkEvent.filterThreadEntityID(thread.getEntityID()))
                .subscribe(networkEvent -> {

                    Message message = networkEvent.message;

                    // Check that the message is relevant to the current thread.
                    if (message.getThreadId() != thread.getId().intValue()) {
                        return;
                    }

                    message.setRead(true);
                    message.update();

                    boolean isAdded = messageListAdapter.addRow(message, false, false);
                    if (isAdded || message.getMessageStatus() == MessageSendStatus.None) {
                        messageListAdapter.sortAndNotify();
                        // Make sure to update for read receipts if necessary
                    } else if (ChatSDK.readReceipts() != null && message.getSender().isMe() && message.getReadStatus() != ReadStatus.read()) {
                        messageListAdapter.sortAndNotify();
                    }

                    // Check if the message from the current user, If so return so we wont vibrate for the user messages.
                    if (message.getSender().isMe() && isAdded) {
                        scrollListTo(PrivateChatActivity.ListPosition.Bottom, layoutManager().findLastVisibleItemPosition() > messageListAdapter.size() - 2);
                    } else {
                        // If the user is near the bottom, then we scroll down when a message comes in
                        if (layoutManager().findLastVisibleItemPosition() > messageListAdapter.size() - 5) {
                            scrollListTo(PrivateChatActivity.ListPosition.Bottom, true);
                        }
                    }

                    if (ChatSDK.readReceipts() != null) {
                        ChatSDK.readReceipts().markRead(thread);
                    }
                }));

        disposableList.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.MessageRemoved))
                .filter(NetworkEvent.filterThreadEntityID(thread.getEntityID()))
                .subscribe(networkEvent -> {
                    messageListAdapter.removeRow(networkEvent.message, true);
                }));

        disposableList.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(
                        EventType.ThreadDetailsUpdated,
                        EventType.ThreadUsersChanged))
                .filter(NetworkEvent.filterThreadEntityID(thread.getEntityID()))
                .subscribe(networkEvent -> messageListAdapter.notifyDataSetChanged()));

        disposableList.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.UserMetaUpdated)).subscribe(networkEvent -> messageListAdapter.notifyDataSetChanged()));

        disposableList.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.TypingStateChanged)).subscribe(networkEvent -> {
                    if (networkEvent.thread.equals(thread)) {
                        String typingText = networkEvent.text;
                        if (typingText != null) {
                            typingText += getString(R.string.typing);
                        }
                        Timber.v(typingText);
                        setSubtitleText(typingText);
                    }
                }));
    }

    protected void setSubtitleText(String text) {
        if (StringChecker.isNullOrEmpty(text)) {
            if (thread.typeIs(ThreadType.Private1to1)) {
                text = "";
            } else {
                text = "";
                for (User u : thread.getUsers()) {
                    if (!u.isMe()) {
                        String name = u.getName();
                        if (name != null && name.length() > 0) {
                            text += name + ", ";
                        }
                    }
                }
                if (text.length() > 0) {
                    text = text.substring(0, text.length() - 2);
                }
            }
        }
        final String finalText = text;
        new Handler(getMainLooper()).post(() -> subtitleTextView.setText(finalText));
    }

    @Override
    protected void onResume() {
        super.onResume();

        removeUserFromChatOnExit = true;

        if (!updateThreadFromBundle(bundle)) {
            return;
        }

        if (thread != null && thread.typeIs(ThreadType.Public)) {
            User currentUser = ChatSDK.currentUser();
            ChatSDK.thread().addUsersToThread(thread, currentUser)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new CrashReportingCompletableObserver(disposableList));
        }

        markRead();

        // We have to do this because otherwise if we background the app
        // we will miss any messages that came through while we were in
        // the background
        loadMessages(true, -1, PrivateChatActivity.ListPosition.Bottom);

        // Show a local notification if the message is from a different thread
        ChatSDK.ui().setLocalNotificationHandler(thread -> !thread.equals(PrivateChatActivity.this.thread));

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Sending a broadcast that the chat was closed, Only if there were new messages on this chat.
     * This is used for example to update the thread list that messages has been read.
     */
    @Override
    protected void onStop() {
        super.onStop();

        disposableList.dispose();

        stopTyping(true);
        markRead();

        if (thread != null && thread.typeIs(ThreadType.Public) && removeUserFromChatOnExit) {
            ChatSDK.thread().removeUsersFromThread(thread, ChatSDK.currentUser()).observeOn(AndroidSchedulers.mainThread()).subscribe(new CrashReportingCompletableObserver());
        }
    }

    /**
     * Not used, There is a piece of code here that could be used to clean all images that was loaded for this chat from cache.
     */
    @Override
    protected void onDestroy() {
        if (enableTrace) {
            android.os.Debug.stopMethodTracing();
        }
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);


        if (!updateThreadFromBundle(intent.getExtras()))
            return;

        if (messageListAdapter != null)
            messageListAdapter.clear();

        initActionBar();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_USERS) {
            if (resultCode == RESULT_OK) {
                updateChat();
            }
        } else if (requestCode == SHOW_DETAILS) {

            if (resultCode == RESULT_OK) {
                // Updating the selected chat id.
                if (data != null && data.getExtras() != null && data.getExtras().containsKey(Keys.THREAD_ENTITY_ID)) {
                    if (!updateThreadFromBundle(data.getExtras())) {
                        return;
                    }

                    if (messageListAdapter != null) {
                        messageListAdapter.clear();
                    }

                    initActionBar();
                } else {
                    updateChat();
                }
            }
        }
    }

    protected void markRead() {
        if (ChatSDK.readReceipts() != null) {
            ChatSDK.readReceipts().markRead(thread);
        } else {
            thread.markRead();
        }
    }

    /**
     * Get the current thread from the bundle bundle, CoreThread could be in the getIntent or in onNewIntent.
     */
    protected boolean updateThreadFromBundle(Bundle bundle) {

        if (bundle != null && (bundle.containsKey(Keys.THREAD_ENTITY_ID))) {
            this.bundle = bundle;
        } else {
            if (getIntent() == null || getIntent().getExtras() == null) {
                finish();
                return false;
            }
            this.bundle = getIntent().getExtras();
        }

        if (this.bundle.containsKey(Keys.THREAD_ENTITY_ID)) {
            String threadEntityID = this.bundle.getString(Keys.THREAD_ENTITY_ID);
            if (threadEntityID != null) {
                thread = ChatSDK.db().fetchThreadWithEntityID(threadEntityID);
            }
        }
        if (this.bundle.containsKey(LIST_POS)) {
            listPos = (Integer) this.bundle.get(LIST_POS);
            scrollListTo(PrivateChatActivity.ListPosition.Current, false);
        }

        if (thread == null) {
            finish();
            return false;
        }

        return true;
    }

    /**
     * Update chat current thread using the {@link PrivateChatActivity#bundle} bundle saved.
     * Also calling the option menu to update it self. Used for showing the thread users icon if thread users amount is bigger then 2.
     * Finally update the action bar for thread messageImageView and name, The update will occur only if needed so free to call.
     */
    protected void updateChat() {
        updateThreadFromBundle(this.bundle);
        supportInvalidateOptionsMenu();
        initActionBar();
    }

    protected void stopTyping(boolean inactive) {
        if (typingTimerDisposable != null) {
            typingTimerDisposable.dispose();
            typingTimerDisposable = null;
        }
        if (inactive) {
            setChatState(TypingIndicatorHandler.State.inactive);
        } else {
            setChatState(TypingIndicatorHandler.State.active);
        }
    }

    protected void setChatState(TypingIndicatorHandler.State state) {
        if (ChatSDK.typingIndicator() != null) {
            ChatSDK.typingIndicator().setChatState(state, thread)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new CrashReportingCompletableObserver(disposableList));
        }
    }

    public void loadMessages(final boolean showLoadingIndicator, final int amountToLoad, final PrivateChatActivity.ListPosition toPosition) {

        if (showLoadingIndicator) {
            recyclerView.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }

        Disposable d = ChatSDK.thread().loadMoreMessagesForThread(null, thread)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((messages, throwable) -> {
                    progressBar.setVisibility(View.INVISIBLE);

                    messageListAdapter.setMessages(messages);

                    if (showLoadingIndicator) {
                        //animateListView();
                    }
                    recyclerView.setVisibility(View.VISIBLE);

                    scrollListTo(toPosition, !showLoadingIndicator);
                });
    }

    public void markAsDelivered(List<Message> messages) {
        for (Message m : messages) {
            markAsDelivered(m);
        }
    }

    public void markAsDelivered(Message message) {
        message.setMessageStatus(MessageSendStatus.Delivered);
        message.update();
    }

    public void scrollListTo(final int position, final boolean animated) {
        listPos = position;

        if (animated) {
            recyclerView.smoothScrollToPosition(listPos);
        } else {
            recyclerView.getLayoutManager().scrollToPosition(listPos);
        }
    }

    public void scrollListTo(final PrivateChatActivity.ListPosition position, final boolean animated) {
        int pos = 0;
        switch (position) {
            case Top:
                pos = 0;
                break;
            case Current:
                pos = listPos == -1 ? messageListAdapter.size() - 1 : listPos;
                break;
            case Bottom:
                pos = messageListAdapter.size() - 1;
                break;
        }
        scrollListTo(pos, animated);
    }

    public String getMessageText() {
        return textInputView.getText().toString();
    }

    public void startTyping() {
        setChatState(TypingIndicatorHandler.State.composing);
        typingTimerDisposable = Observable.just(true).delay(5000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(aBoolean -> setChatState(TypingIndicatorHandler.State.active));
    }

    public void onKeyboardShow() {
        scrollListTo(PrivateChatActivity.ListPosition.Bottom, false);
    }

    public void onKeyboardHide() {
        scrollListTo(PrivateChatActivity.ListPosition.Bottom, false);
    }

    /**
     * show the option popup when the menu key is pressed.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void executeChatOption(ChatOption option) {
        if (option.getType() == ChatOptionType.SendMessage) {
            handleMessageSend((Observable<MessageSendProgress>) option.execute(this, thread));
        } else {
            option.execute(this, thread).subscribe(new CrashReportingObserver<>());
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            sendMessage(getMessageText(), true);
        }
        return false;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // if enter is pressed start calculating
        startTyping();
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
            int editTextLineCount = ((EditText) v).getLineCount();
            return editTextLineCount >= getResources().getInteger(R.integer.chat_sdk_max_message_lines);
        }
        return false;
    }

    /**
     * call this to show toast msg
     *
     * @param id
     */
    public void showToastMsg(int id) {
        Toast.makeText(getApplicationContext(), id, Toast.LENGTH_SHORT).show();
    }

}

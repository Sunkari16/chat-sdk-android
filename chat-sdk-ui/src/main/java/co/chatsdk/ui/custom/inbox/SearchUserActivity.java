package co.chatsdk.ui.custom.inbox;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import co.chatsdk.core.dao.User;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.utils.DisposableList;
import co.chatsdk.ui.R;
import co.chatsdk.ui.custom.inbox.utils.ToastHelper;
import co.chatsdk.ui.main.BaseActivity;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * ui component for adding a contact
 */
public class SearchUserActivity extends BaseActivity {

    private ImageView ivSearchIcon;
    private EditText etSearchQuery;
    private RecyclerView rvSearchUsers;
    private ProgressBar pbProgress;
    private DisposableList disposableList = new DisposableList();
    private SearchUserAdapter searchUserAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);
        initViews();
        setSearchUserRecyclerView();
    }

    private void setSearchUserRecyclerView() {

        searchUserAdapter = new SearchUserAdapter();

        rvSearchUsers.setHasFixedSize(true);
        rvSearchUsers.setLayoutManager(new LinearLayoutManager(this));
        rvSearchUsers.setAdapter(searchUserAdapter);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Observable<String> observableInput = addInputObservable();
        Observable<String> observableClick = getClickObservable();
        Observable<User> observableAdapterClick = searchUserAdapter.getUserClickSubject();

        Disposable disposableAdapterItem = observableAdapterClick
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(it -> disposableList.add(getThreadCreationObserver(it))).subscribe();

        @SuppressWarnings("unchecked")
        Disposable clickDisposable = observableClick.observeOn(AndroidSchedulers.mainThread()).doOnNext(it -> {
            showLocalProgress();
            searchUserAdapter.clear();
            getOnlineUsers(it);
        }).subscribe();

        @SuppressWarnings("unchecked")
        Disposable disposable = observableInput
                .observeOn(AndroidSchedulers.mainThread())
                .filter(o -> {
                    if (o.length() > 2) {
                        ivSearchIcon.setVisibility(View.VISIBLE);
                        return true;
                    } else {
                        ivSearchIcon.setVisibility(View.GONE);
                        return false;
                    }
                }).debounce(500, TimeUnit.MILLISECONDS).subscribe();

        disposableList.add(disposable);
        disposableList.add(clickDisposable);
        disposableList.add(disposableAdapterItem);
    }

    private void getOnlineUsers(String query) {
        ChatSDK.search().usersForIndex(query).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<User>() {
            @Override
            public void onSubscribe(Disposable d) {
                disposableList.add(d);
            }

            @Override
            public void onNext(User user) {
                if (!user.isMe()) {
                    searchUserAdapter.addUser(user);
                }
            }

            @Override
            public void onError(Throwable e) {
                ToastHelper.show(getApplicationContext(),R.string.chat_no_user_found);
            }

            @Override
            public void onComplete() {
                hideLocalProgress();
            }
        });
    }

    private Observable<String> addInputObservable() {
        return Observable.create(emitter -> {
            TextWatcher textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    emitter.onNext(editable.toString());
                }
            };

            etSearchQuery.addTextChangedListener(textWatcher);
            emitter.setCancellable(() -> etSearchQuery.removeTextChangedListener(textWatcher));
        });
    }

    private Observable<String> getClickObservable() {
        return Observable.create(emitter -> {
            ivSearchIcon.setOnClickListener(view -> emitter.onNext(etSearchQuery.getText().toString()));
            emitter.setCancellable(() -> ivSearchIcon.setOnClickListener(null));
        });
    }

    private void initViews() {

        ivSearchIcon = findViewById(R.id.ivSearchIcon);
        etSearchQuery = findViewById(R.id.etSearchQuery);
        rvSearchUsers = findViewById(R.id.rvSearchUsers);
        pbProgress = findViewById(R.id.pbProgress);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    void showLocalProgress() {
        rvSearchUsers.setVisibility(View.GONE);
        pbProgress.setVisibility(View.VISIBLE);
    }

    private void hideLocalProgress() {
        rvSearchUsers.setVisibility(View.VISIBLE);
        pbProgress.setVisibility(View.GONE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (disposableList != null) {
            disposableList.dispose();
        }
    }

    private Disposable getThreadCreationObserver(User user) {
        return ChatSDK.thread().createThread("", user, ChatSDK.currentUser())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(thread -> {
                    ChatSDK.ui().startChatActivityForID(getApplicationContext(), thread.getEntityID());
                }, throwable -> ToastHelper.show(getApplicationContext(), throwable.getLocalizedMessage()));
    }
}

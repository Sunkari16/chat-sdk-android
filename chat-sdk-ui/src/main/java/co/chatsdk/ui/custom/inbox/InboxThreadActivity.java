package co.chatsdk.ui.custom.inbox;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.ui.R;
import co.chatsdk.ui.main.BaseActivity;

/**
 * inbox chat view
 */
public class InboxThreadActivity extends BaseActivity {

    /**
     * change fragment by calling from other UI parts
     *
     * @param fragment           fragment to be opened
     * @param isBackPressEnabled allow back or not
     */
    final protected void replaceFragment(int containerId, Fragment fragment, Boolean isBackPressEnabled) {
        String tag = fragment.getClass().getCanonicalName();
        if (getSupportFragmentManager().findFragmentByTag(tag) != null) return;
        if (isBackPressEnabled) {
            getSupportFragmentManager().beginTransaction().replace(containerId, fragment, tag)
                    .addToBackStack(tag)
                    .commitAllowingStateLoss();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(containerId, fragment, tag)
                    .commitAllowingStateLoss();
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox_thread);
        replaceFragment(R.id.flContainerThread, PrivateThreadFragment.getInstance(), false);
        initViews();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        toolbar.setTitle(R.string.chatr_label_message);
        findViewById(R.id.ivAddUser).setOnClickListener(view -> ChatSDK.ui().startActivity(this, SearchUserActivity.class));
    }
}

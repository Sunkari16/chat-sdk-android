package co.chatsdk.ui.custom.inbox;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.ocpsoft.prettytime.PrettyTime;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import co.chatsdk.core.dao.DaoCore;
import co.chatsdk.core.dao.Message;
import co.chatsdk.core.dao.Thread;
import co.chatsdk.core.interfaces.ThreadType;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.utils.Strings;
import co.chatsdk.ui.R;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * adapter view to show the threads currently running
 */
public class PrivateThreadAdapter extends RecyclerView.Adapter<PrivateThreadAdapter.ViewHolder> {

    public static int ThreadCellType = 0;

    protected WeakReference<Context> context;

    protected List<Thread> threads = new ArrayList<>();

    protected HashMap<Thread, String> typing = new HashMap<>();
    protected PublishSubject<Thread> onClickSubject = PublishSubject.create();
    protected PublishSubject<Thread> onLongClickSubject = PublishSubject.create();

    public PrivateThreadAdapter(FragmentActivity activity) {
        this.context = new WeakReference(activity);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_private_thread, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setData(threads.get(position));
    }

    public String getLastMessageDateAsString(Date date) {
        if (date != null) {
            return Strings.dateTime(date);
        }
        return null;
    }

    public String getLastMessageText(Message lastMessage) {
        String messageText = Strings.t(R.string.no_messages);
        if (lastMessage != null) {

            messageText = Strings.payloadAsString(lastMessage);
        }
        return messageText;
    }

    @Override
    public int getItemViewType(int position) {
        return ThreadCellType;
    }

    @Override
    public int getItemCount() {
        return threads.size();
    }

    public boolean addRow(Thread thread, boolean notify) {
        for (Thread t : threads) {
            if (t.getEntityID().equals(thread.getEntityID())) {
                return false;
            }
        }

        threads.add(thread);
        if (notify) {
            notifyDataSetChanged();
        }
        return true;
    }

    public void addRow(Thread thread) {
        addRow(thread, true);
    }

    public void setTyping(Thread thread, String message) {
        if (message != null) {
            typing.put(thread, message);
        } else {
            typing.remove(thread);
        }
    }

    protected void sort() {
        Collections.sort(threads, new InboxThreadComparator());
    }

    public void clearData() {
        clearData(true);
    }

    public void clearData(boolean notify) {
        threads.clear();
        if (notify) {
            notifyDataSetChanged();
        }
    }

    public Observable<Thread> onClickObservable() {
        return onClickSubject;
    }

    public Observable<Thread> onLongClickObservable() {
        return onLongClickSubject;
    }


    public void updateThreads(List<Thread> threads) {
        boolean added = false;
        for (Thread t : threads) {
            added = addRow(t, false) || added;
        }
        // Maybe the last message has changed. I think this can lead to a race condition
        // Which causes the thread not to update when a new message comes in
//        if (added) {
        sort();
        notifyDataSetChanged();
//        }
    }

    public void setThreads(List<Thread> threads) {
        clearData(false);
        updateThreads(threads);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private SimpleDraweeView ivUserProfile;
        private TextView tvUserName, tvLastMsg, tvLastMsgTime;
        private View sdvOnlineStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserProfile = itemView.findViewById(R.id.ivUserProfile);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMsg = itemView.findViewById(R.id.tvLastMsg);
            tvLastMsgTime = itemView.findViewById(R.id.tvLastMsgTime);
            sdvOnlineStatus = itemView.findViewById(R.id.sdvOnlineStatus);
        }

        public void setData(Thread thread) {
            tvUserName.setText(Strings.nameForThread(thread));

            itemView.setOnClickListener(view -> onClickSubject.onNext(thread));

            itemView.setOnLongClickListener(view -> {
                onLongClickSubject.onNext(thread);
                return true;
            });

            Date lastMessageAddedDate = thread.getLastMessageAddedDate();
            if (lastMessageAddedDate != null) {
                String rawTime = getTimesAgo(lastMessageAddedDate.getTime());
                tvLastMsgTime.setText(rawTime);

                Message message = thread.getLastMessage();
                if (message == null) {
                    List<Message> messages = thread.getMessagesWithOrder(DaoCore.ORDER_DESC, 1);
                    if (messages.size() > 0) {
                        message = messages.get(0);
                        thread.setLastMessage(message);
                        thread.update();
                    }
                }

                tvLastMsg.setText(getLastMessageText(message));
            }

            if (typing.get(thread) != null) {
                tvLastMsg.setText(String.format(context.get().getString(R.string.chat_placeholder_typing), typing.get(thread)));
            }

            int unreadMessageCount = thread.getUnreadMessagesCount();

            if (unreadMessageCount != 0 && (thread.typeIs(ThreadType.Private) || ChatSDK.config().unreadMessagesCountForPublicChatRoomsEnabled)) {
                tvLastMsg.setTextColor(ContextCompat.getColor(context.get(), R.color.chat_text_color_slate));
                Typeface boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD);
                tvLastMsg.setTypeface(boldTypeface);
                sdvOnlineStatus.setVisibility(View.VISIBLE);
            } else {
                tvLastMsg.setTextColor(ContextCompat.getColor(context.get(), R.color.chat_grey_light_500));
                sdvOnlineStatus.setVisibility(View.GONE);
                Typeface boldTypeface = Typeface.defaultFromStyle(Typeface.NORMAL);
                tvLastMsg.setTypeface(boldTypeface);
            }

            ThreadImageBuilder.load(ivUserProfile, thread);
        }
    }

    /**
     * time unit
     *
     * @param time
     * @return
     */
    public static String getTimesAgo(long time) {
        PrettyTime prettyTime = new PrettyTime(Locale.getDefault());
        return String.format("\u202f%1s", prettyTime.format(new Date(time)));
    }
}

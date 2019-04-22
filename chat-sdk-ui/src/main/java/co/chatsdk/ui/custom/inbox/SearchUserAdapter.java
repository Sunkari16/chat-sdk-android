package co.chatsdk.ui.custom.inbox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import co.chatsdk.core.dao.User;
import co.chatsdk.core.interfaces.UserListItem;
import co.chatsdk.ui.R;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * search user adapter class for binding the user details
 */
public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.ViewHolder> {

    private ArrayList<User> usersList = new ArrayList<>();
    private PublishSubject<User> userClickSubject = PublishSubject.create();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setData(usersList.get(position));
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public void clear() {
        this.usersList.clear();
        notifyDataSetChanged();
    }

    void addUser(User user) {
        this.usersList.add(user);
        notifyItemInserted(usersList.size() - 1);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private SimpleDraweeView ivUserProfile;
        private TextView tvUserName;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserProfile = itemView.findViewById(R.id.ivUserProfile);
            tvUserName = itemView.findViewById(R.id.tvUserName);

            itemView.setOnClickListener(view -> userClickSubject.onNext(usersList.get(getAdapterPosition())));
        }

        public void setData(UserListItem user) {
            if (user == null) return;
            tvUserName.setText(user.getName());

            ivUserProfile.setImageURI(user.getAvatarURL());
        }
    }

    public void setData(ArrayList<User> users) {
        this.usersList.clear();
        this.usersList.addAll(users);
        notifyDataSetChanged();
    }

    Observable<User> getUserClickSubject() {
        return userClickSubject;
    }
}

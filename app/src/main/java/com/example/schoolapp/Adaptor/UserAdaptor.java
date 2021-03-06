package com.example.schoolapp.Adaptor;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.schoolapp.Body.User;
import com.example.schoolapp.MessageActivity;
import com.example.schoolapp.R;

import java.util.List;

public class UserAdaptor extends RecyclerView.Adapter<UserAdaptor.ViewHolder>{

    private Context mContext;
    private List<User> mUsers;
    private boolean ischat;

    public UserAdaptor(Context mContext, List<User> mUsers, boolean ischat){
        this.mUsers = mUsers;
        this.mContext = mContext;
        this.ischat = ischat;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.user_element, viewGroup, false);
        return new UserAdaptor.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        final User users = mUsers.get(i);
        viewHolder.student_Name.setText(users.getName());
        if (users.getImageURL().equals("default")){
            viewHolder.profile_Image.setImageResource(R.mipmap.ic_launcher);
        }else{
            Glide.with(mContext).load(users.getImageURL()).into(viewHolder.profile_Image);
        }
        if (ischat){
            if(users.getCurrentStatus().equals("Online")){
                viewHolder.img_Online.setVisibility(View.VISIBLE);
                viewHolder.img_Offline.setVisibility(View.GONE);
            }else{
                viewHolder.img_Online.setVisibility(View.GONE);
                viewHolder.img_Offline.setVisibility(View.VISIBLE);
            }
        }else{
            viewHolder.img_Online.setVisibility(View.GONE);
            viewHolder.img_Offline.setVisibility(View.GONE);
        }
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MessageActivity.class);
                intent.putExtra("userid", users.getId());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }
    public  class ViewHolder extends RecyclerView.ViewHolder{

        public TextView student_Name;
        public ImageView profile_Image;
        private ImageView img_Online;
        private ImageView img_Offline;

        public ViewHolder(View itemView) {
            super(itemView);

            student_Name = itemView.findViewById(R.id.name);
            profile_Image = itemView.findViewById(R.id.profile_image);
            img_Online = itemView.findViewById(R.id.img_online);
            img_Offline = itemView.findViewById(R.id.img_offline);
        }
    }
}

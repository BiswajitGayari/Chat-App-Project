package com.application.chat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.chat.Adapters.UserAdapter;
import com.application.chat.CacheDb.DatabaseChatListHelper;
import com.application.chat.Models.Friend;
import com.application.chat.Models.Message;
import com.application.chat.Models.User;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserActivity extends AppCompatActivity{
    FirebaseUser fUser;
    DrawerLayout drawerLayout;
    View drawerView;
    NavigationView navigationView;
    TextView emailTV, nameTV;
    RecyclerView recyclerView;
    List<User> userList;
    UserAdapter userAdapter;
    DatabaseReference userRef;
    DatabaseChatListHelper cache;
    Cursor cursor;
    TextInputEditText searchDataInput;
    DatabaseReference chatRef;
    BroadcastReceiver broadcastReceiver;
    ExecutorService exe;
    String sender;
    boolean checkBroadCast=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_user);
        this.userRef= FirebaseDatabase.getInstance().getReference("Users");
        this.cache=DatabaseChatListHelper.getInstance(getApplicationContext());
        this.fUser =FirebaseAuth.getInstance().getCurrentUser();
        this.exe=Executors.newFixedThreadPool(2);
        updateToken();
        this.userList=new ArrayList<>();
        this.searchDataInput=findViewById(R.id.searchItems);
        this.recyclerView=findViewById(R.id.recyclerList);
        LinearLayoutManager layout=new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layout);
        userAdapter=new UserAdapter(userList,this);
        recyclerView.setAdapter(userAdapter);
        loadChatList();
        loadDrawer();
        chatRef=FirebaseDatabase.getInstance().getReference("Chats");
        ImageView menu_button=findViewById(R.id.menu_btn);
        menu_button.setOnClickListener(v->{
            if(drawerLayout.isDrawerOpen(GravityCompat.START) && drawerLayout!=null){
                drawerLayout.closeDrawer(GravityCompat.START);
            }else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        pushStatus();
        searchDataInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int squenceLengh=charSequence.toString().length();
                if(squenceLengh!=0) {
                    filterTheSearchedItem(charSequence.toString());
                }else{
                    userAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        searchDataInput.setOnFocusChangeListener((v,hasFocus)->{
            if (!hasFocus){
                loadChatList();
            }
        });
        FloatingActionButton floatAddButton=findViewById(R.id.floatAdd);
        floatAddButton.setOnClickListener(v->{
            Intent i=new Intent(getApplicationContext(), UserAddActivity.class);
            startActivity(i);
        });
        AppCompatButton logout=findViewById(R.id.logoutButton);
        logout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            redirect(new MainActivity());
        });
        broadcastReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkBroadCast=intent.getBooleanExtra("seen",false);
                sender=intent.getStringExtra("sender");
            }
        };
        IntentFilter intentFilter=new IntentFilter("com.application.chat.ACTION_REFRESH");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(broadcastReceiver,intentFilter);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.hasChildren()){
                    for (DataSnapshot data:snapshot.getChildren()){
                        Boolean isOnline=data.child("isOnline").getValue(Boolean.class);
                        if(isOnline!=null){
                            int pos=getPositionById(data.getKey());
                            if(pos!=-1)
                                updateStatus(isOnline,pos);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message =snapshot.getValue(Message.class);
                String senderId= message.getSender();
                if(message !=null && message.getReciver()!=null && senderId!=null && !message.isSeen()){
                    if(message.getReciver().equals(fUser.getUid()) && checkUser(senderId)){
                        int pos=getPositionById(senderId);
                        updateMessageIndicator(pos);
                    }
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void updateStatus(boolean isOnline,int pos){
        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(pos);
        if(viewHolder!=null){
            TextView status=viewHolder.itemView.findViewById(R.id.status);
            if(isOnline) {
                status.setVisibility(View.VISIBLE);
                return;
            }
            else
                status.setVisibility(View.GONE);

            userAdapter.notifyItemChanged(pos);
        }
    }
    public void updateMessageIndicator(int pos){
        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(pos);
        if (viewHolder != null) {
            TextView indicator = viewHolder.itemView.findViewById(R.id.indicator);
            if (indicator.getVisibility() == View.GONE) {
                indicator.setText("1");
                indicator.setVisibility(View.VISIBLE);
                userAdapter.notifyItemChanged(pos);
            }else {
                String count = indicator.getText().toString();
                int parsedCount = Integer.parseInt(count);
                parsedCount++;
                indicator.setText(String.valueOf(parsedCount));
                userAdapter.notifyItemChanged(pos);
            }
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(checkBroadCast){
            int pos = getPositionById(sender);
            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(pos);
            if (viewHolder != null) {
                TextView indicator = viewHolder.itemView.findViewById(R.id.indicator);
                indicator.setVisibility(View.GONE);
                userAdapter.notifyItemChanged(pos);
                Log.e("Broadcast","sended");
            }
        }
    }
    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    public boolean checkUser(String id){
        for(User user:userList){
            if(user.getId().equals(id)){
                return true;
            }
        }
        return false;
    }
    public void loadChatList() {
        /*cursor=cache.getAllUserId();
        if(cursor!=null){
            int columnIndex=cursor.getColumnIndex(cache.getColumnUserId());
            if(columnIndex!=-1) {
                while (cursor.moveToNext()) {
                    String gotId = cursor.getString(columnIndex);
                    queryAndAdd(gotId);
                }
            }
        }*/
        DatabaseReference friendsRef=FirebaseDatabase.getInstance().getReference("Friends");
        friendsRef.child(fUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data:snapshot.getChildren()){
                    Friend friend=data.getValue(Friend.class);
                    if(friend!=null && friend.getFriendId()!=null){
                        queryAndAdd(friend.getFriendId());
                    }else {
                        Log.e("Null Friend","Not found friend");
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirendNode",error.getMessage());
            }
        });
    }
    public int getPositionById(String target){
        for(int i=0;i<userList.size();i++){
            if(userList.get(i).getId().equals(target)){
                return i;
            }
        }
        return -1;
    }
    public void queryAndAdd(String id){
        userRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User userInfo=snapshot.getValue(User.class);
                if(userInfo!=null && !userInfo.getId().equals(fUser.getUid())){
                    userList.add(userInfo);
                    userAdapter.notifyItemInserted(userList.size()-1);
                    recyclerView.smoothScrollToPosition(userList.size()-1);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error)  {
                Log.e("Firebase error",error.getMessage());
            }
        });
    }
    public void filterTheSearchedItem(String username) {
        List<User> newList = new ArrayList<>();
        for (User user : userList) {
            if (user.getName().toLowerCase().contains(username.toLowerCase())){
                newList.add(user);
            }
        }
        userAdapter.updateList(newList);
    }
    public boolean matchesAnyLatter(String originalName,String query){
        originalName=originalName.toLowerCase();
        query=query.toLowerCase();
        for(int i=0;i<originalName.length();i++){
            char currentChar=originalName.charAt(i);
            if(query.indexOf(currentChar)!=-1){
                return true;
            }
        }
        return false;
    }
    public void loadDrawer(){
        drawerLayout=findViewById(R.id.drawerlayout);
        ActionBarDrawerToggle drawerToggle=new ActionBarDrawerToggle(this,drawerLayout,
                R.string.navigation_drawe_open,R.string.navigation_drawe_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        navigationView=findViewById(R.id.navigation_view);
        drawerView=navigationView.getHeaderView(0);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);
            if (id == R.id.editProfile) {
                Intent i = new Intent(getApplicationContext(), ProfileEditActivity.class);
                startActivity(i);
            } else if (id == R.id.settings_item) {
                Intent i = new Intent(getApplicationContext(), SettingsActivtiy.class);
                startActivity(i);
                return true;

            } else if (id == R.id.share_item) {
                return true;

            } else {
                return false;
            }
            return false;
        });
        this.emailTV = drawerView.findViewById(R.id.emailText);
        this.nameTV = drawerView.findViewById(R.id.username);
        emailTV.setText(fUser.getEmail());
        nameTV.setText(fUser.getDisplayName());
        ImageView drawerPic=drawerView.findViewById(R.id.drawer_profile_pic);
        Uri uriImage= fUser.getPhotoUrl();
        if(uriImage!=null) {
            Glide.with(getApplicationContext())
                    .load(uriImage)
                    .centerCrop()
                    .placeholder(R.drawable.default_pic)
                    .error(R.drawable.default_pic)
                    .into(drawerPic);
        }
        else{
            Toast.makeText(getApplicationContext(),"Image not found",Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        super.onBackPressed();
    }
    public void updateToken(){
        Executors.newSingleThreadExecutor().execute(()-> {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if(!task.isSuccessful()){
                    Log.e("Firebase","Task for token was not successfull");
                    return;
                }
                String token=task.getResult();
                DatabaseReference tokenRef=userRef.child(fUser.getUid()).child("token");
                tokenRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String data=snapshot.getValue(String.class);
                        if(data==null){
                            tokenRef.setValue(token);
                            return;
                        }else if(data!=null && !data.equals(token)){
                            tokenRef.setValue(token);
                            return;
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase","Token Database reference error");
                    }
                });
            });
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_bar, menu);
        return true;
    }
    public void redirect(Activity ui){
        Intent i=new Intent(getApplicationContext(),ui.getClass());
        startActivity(i);
        finish();
    }
    public void pushStatus(){
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users").child(fUser.getUid());
        ref.child("isOnline").setValue(true);
        ref.child("isOnline").onDisconnect().setValue(false);
    }
}
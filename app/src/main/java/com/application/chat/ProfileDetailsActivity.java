package com.application.chat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.application.chat.Models.User;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileDetailsActivity extends AppCompatActivity {
    ImageView profileImage;
    TextInputLayout inputName;
    Bitmap imageBitmap;
    FirebaseUser fUser;
    boolean check=true;
    ExecutorService exe;
    DatabaseReference userRef;
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        this.fUser =FirebaseAuth.getInstance().getCurrentUser();
        setContentView(R.layout.activity_profile_details);
        this.userRef=FirebaseDatabase.getInstance().getReference("Users");
        this.progressDialog=new ProgressDialog(this);
        this.inputName=findViewById(R.id.nameInput);
        this.exe=Executors.newFixedThreadPool(2);
        AppCompatButton nextButton=findViewById(R.id.buttonNext);
        nextButton.setOnClickListener(view -> {
            String name= inputName.getEditText().getText().toString();
            progressDialog.show();
            if(isValid(name) && check){
                updateName(name);
                firebaseStore();
                switchActivity(new UserActivity());
            }
            progressDialog.dismiss();
        });
        this.profileImage=findViewById(R.id.profile_pic);
        this.profileImage.setOnClickListener(v->selectImage());
    }
    public void updateProfilePic(Uri image){
        UserProfileChangeRequest profileChangeRequest=new UserProfileChangeRequest.Builder()
                .setPhotoUri(image)
                .build();
        fUser.updateProfile(profileChangeRequest).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Toast.makeText(getApplicationContext(),"Profile Updated",Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            check=false;
            Toast.makeText(getApplicationContext(),"Couldn't update profile",Toast.LENGTH_SHORT).show();
        });
    }
    public void updateName(String name){
        UserProfileChangeRequest profileChangeRequest=new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        fUser.updateProfile(profileChangeRequest).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Toast.makeText(getApplicationContext(),"Name Updated",Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            check=false;
            Toast.makeText(getApplicationContext(),"Couldn't update profile",Toast.LENGTH_SHORT).show();
        });
    }
    public void uploadToStorage(Uri uri){
        StorageReference storageReference=FirebaseStorage.getInstance().getReference();
        StorageReference imageRef=storageReference.child(fUser.getUid()).child("photo.jpg");
        exe.execute(()-> {
            UploadTask uploadTask = imageRef.putFile(uri);
            uploadTask.addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uriImage -> {
                updateProfilePic(uriImage);
                Toast.makeText(getApplicationContext(), "Image uploaded", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            })).addOnFailureListener(e -> {
                progressDialog.dismiss();
                check = false;
                Toast.makeText(getApplicationContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
            });
        });
    }
    public void firebaseStore(){
        exe.execute(()-> {
            if(fUser !=null && fUser.getPhotoUrl()!=null && fUser.getDisplayName()!=null){
                User userInfo = new User(fUser.getUid(), fUser.getDisplayName(), fUser.getPhotoUrl().toString(),null,false);
                userRef.child(fUser.getUid()).setValue(userInfo);
            }
            else{
                Log.e("Null pointer","No such firebase user");
            }
        });
    }
    public void selectImage(){
        Intent i=new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        launchSomeActivity.launch(i);
    }

    ActivityResultLauncher<Intent> launchSomeActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
               if(result.getResultCode()==Activity.RESULT_OK){
                   Intent data=result.getData();
                   if(data!=null && data.getData()!=null) {
                        Uri uri = data.getData();
                        uploadToStorage(uri);
                       try{
                           imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                       } catch (Exception ex) {
                           ex.printStackTrace();
                       }
                       profileImage.setImageBitmap(imageBitmap);
                   }
               }
            });
    public boolean isValid(String name){
        if(TextUtils.isEmpty(name)){
            inputName.setError("this field is empty");
            check=false;
        }
        else{
            inputName.setError(null);
        }
        return check;
    }
    public void switchActivity(Activity act){
        Intent i=new Intent(getApplicationContext(), act.getClass());
        startActivity(i);
        finish();
    }
}
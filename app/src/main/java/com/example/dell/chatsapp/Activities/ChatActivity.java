package com.example.dell.chatsapp.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.dell.chatsapp.Adapters.MessagesAdapter;
import com.example.dell.chatsapp.Models.Message;
import com.example.dell.chatsapp.R;
import com.example.dell.chatsapp.databinding.ActivityChatBinding;
import com.example.dell.chatsapp.databinding.ActivityChatBinding;
import com.example.dell.chatsapp.databinding.AttachmentPopupBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {
    ActivityChatBinding binding;
    MessagesAdapter adapter;
    ArrayList<Message> messages;

    String senderRoom,receiverRoom;
    FirebaseDatabase database;
    FirebaseStorage storage;
    ProgressDialog dialog;
    String senderUid;
    String receiverUid;

    private static final int CAMERA_REQUEST_CODE=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        final BottomSheetDialog bottomSheet = new BottomSheetDialog(ChatActivity.this);

        View view1 = ChatActivity.this.getLayoutInflater().inflate(R.layout.attachment_popup, (ViewGroup)null);
        AttachmentPopupBinding binding1=AttachmentPopupBinding.bind(view1);

        bottomSheet.setContentView(binding1.getRoot());

        setSupportActionBar(binding.toolbar);

        database=FirebaseDatabase.getInstance();
        storage=FirebaseStorage.getInstance();

        dialog=new ProgressDialog(this);
        dialog.setMessage("Uploading image...");
        dialog.setCancelable(false);


        messages=new ArrayList<>();


        String name= getIntent().getStringExtra("name");
        String profile= getIntent().getStringExtra("image");
         receiverUid= getIntent().getStringExtra("uid");

         binding.name.setText(name);
         Glide.with(this).load(profile)
                 .placeholder(R.drawable.avatar)
                 .into(binding.profile);
         binding.imageView2.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 finish();
             }
         });

         senderUid= FirebaseAuth.getInstance().getUid();

         database.getReference().child("presence").child(receiverUid).addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(@NonNull DataSnapshot snapshot) {
                 if (snapshot.exists()) {
                     String status= snapshot.getValue(String.class);
                     if(!status.isEmpty()) {
                         if(status.equals("Offline")){
                             binding.status.setVisibility(View.GONE);
                         }else {
                             binding.status.setText(status);
                             binding.status.setVisibility(View.VISIBLE);
                         }
                     }
                 }
             }

             @Override
             public void onCancelled(@NonNull DatabaseError error) {

             }
         });

        senderRoom=senderUid+receiverUid;
        receiverRoom=receiverUid+senderUid;
        adapter=new MessagesAdapter(this,messages,senderRoom,receiverRoom);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);



        database.getReference().child("chats")
                .child(senderRoom)
                .child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for(DataSnapshot snapshot1: snapshot.getChildren()){
                            Message message=snapshot1.getValue(Message.class);
                            message.setMessageId(snapshot1.getKey());
                            messages.add(message);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageTxt=binding.messageBox.getText().toString();
                Date date=new Date();
                Message message = new Message(messageTxt,senderUid,date.getTime());
                binding.messageBox.setText("");
                String randomKey=database.getReference().push().getKey();


                HashMap<String, Object> lastMsgObj = new HashMap<>();
                lastMsgObj.put("lastMsg",substr( message.getMessage()));
                lastMsgObj.put("lastMsgTime", date.getTime());

                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                database.getReference().child("chats").child(senderRoom).child("messages").
                        child(randomKey)
                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        database.getReference().child("chats").child(receiverRoom).child("messages").child(randomKey)
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                            }
                        });

                    }
                });
            }
        });
        binding.camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent= new Intent();
                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent,47);
            }
        });


        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                bottomSheet.show();





//                Intent intent=new Intent();
//                intent.setAction(Intent.ACTION_GET_CONTENT);
//                intent.setType("image/*");
//                startActivityForResult(intent,25);
            }
        });


        binding1.cam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,25);
            }
        });
        binding1.video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                startActivityForResult(intent,85);


            }
        });

        final Handler handler=new Handler();

        binding.messageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                database.getReference().child("presence").child(senderUid).setValue("typing...");

                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStoppedTyping,1000);

            }

            Runnable userStoppedTyping =new Runnable() {
                @Override
                public void run() {
                    database.getReference().child("presence").child(senderUid).setValue("Online");

                }
            };
        });
        getSupportActionBar().setDisplayShowTitleEnabled(false);

//        getSupportActionBar().setTitle(name);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==25 ){
            if(data!=null){
                if(data.getData()!=null){
                    Uri selectedImage =data.getData();
                    Calendar calendar= Calendar.getInstance();
                    StorageReference reference = storage.getReference().child("chats").child(calendar.getTimeInMillis()+"");
                    dialog.show();
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            dialog.dismiss();
                            if(task.isSuccessful()){
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String filepath= uri.toString();


                                                String messageTxt=binding.messageBox.getText().toString();
                                                Date date=new Date();
                                                Message message = new Message(messageTxt,senderUid,date.getTime());
                                                message.setImageUrl(filepath);
                                                message.setMessage("photo");
                                                binding.messageBox.setText("");
                                                String randomKey=database.getReference().push().getKey();


                                                HashMap<String, Object> lastMsgObj = new HashMap<>();
                                                lastMsgObj.put("lastMsg", message.getMessage());
                                                lastMsgObj.put("lastMsgTime", date.getTime());

                                                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                                                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                                                database.getReference().child("chats").child(senderRoom).child("messages").
                                                        child(randomKey)
                                                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        database.getReference().child("chats").child(receiverRoom).child("messages").child(randomKey)
                                                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {

                                                            }
                                                        });

                                                    }
                                                });


                                    }
                                });
                            }
                        }
                    });

                }
            }
        }
        if(resultCode==Activity.RESULT_OK && requestCode==85){{
            if(data!=null){
                if(data.getData()!=null){
                    Uri selectedVideo =data.getData();
                    Calendar calendar= Calendar.getInstance();
                    StorageReference reference = storage.getReference().child("chats").child("videos").child(calendar.getTimeInMillis()+"");
                    dialog.show();
                    reference.putFile(selectedVideo).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            dialog.dismiss();
                            if(task.isSuccessful()){
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String filepath= uri.toString();


                                        String messageTxt=binding.messageBox.getText().toString();
                                        Date date=new Date();
                                        Message message = new Message(messageTxt,senderUid,date.getTime());
                                        message.setVideoUrl(filepath);
                                        message.setMessage("video");
                                        binding.messageBox.setText("");
                                        String randomKey=database.getReference().push().getKey();


                                        HashMap<String, Object> lastMsgObj = new HashMap<>();
                                        lastMsgObj.put("lastMsg", message.getMessage());
                                        lastMsgObj.put("lastMsgTime", date.getTime());

                                        database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                                        database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                                        database.getReference().child("chats").child(senderRoom).child("messages").
                                                child(randomKey)
                                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                database.getReference().child("chats").child(receiverRoom).child("messages").child(randomKey)
                                                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {

                                                    }
                                                });

                                            }
                                        });


                                    }
                                });
                            }
                        }
                    });

                }
            }
        }


        }
       if(resultCode== Activity.RESULT_OK){
           if(requestCode == 47){
               onCaptureImageResult(data);
           }
       }
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail= (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG,100,bytes);
        byte bb[]=bytes.toByteArray();

        uploadToFirebase(bb);
    }

    private void uploadToFirebase(byte[] bb) {
        Calendar calendar= Calendar.getInstance();
        StorageReference reference = storage.getReference().child("chats").child(calendar.getTimeInMillis()+"");
        dialog.show();
        reference.putBytes(bb).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                dialog.dismiss();
                Toast.makeText(ChatActivity.this, "image uploaded", Toast.LENGTH_SHORT).show();
                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String filepath= uri.toString();


                        String messageTxt=binding.messageBox.getText().toString();
                        Date date=new Date();
                        Message message = new Message(messageTxt,senderUid,date.getTime());
                        message.setImageUrl(filepath);
                        message.setMessage("photo");
                        binding.messageBox.setText("");
                        String randomKey=database.getReference().push().getKey();


                        HashMap<String, Object> lastMsgObj = new HashMap<>();
                        lastMsgObj.put("lastMsg", message.getMessage());
                        lastMsgObj.put("lastMsgTime", date.getTime());

                        database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                        database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                        database.getReference().child("chats").child(senderRoom).child("messages").
                                child(randomKey)
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                database.getReference().child("chats").child(receiverRoom).child("messages").child(randomKey)
                                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                    }
                                });

                            }
                        });


                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void  scrollToPosition(int position_mess){
        if(binding.recyclerView== null){
            return;
        }
        binding.recyclerView.scrollToPosition(position_mess);

    }

public String substr(String mess){
        int l=mess.length();
        String mess1=" ";
        if(l<=55){
            return mess;
        }
        else{
            mess1= mess.substring(0,56);
            mess1=mess1+"...";
            return mess1;
        }
}
    @Override
    protected void onResume() {
        super.onResume();
        String currentId= FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Online");

      //  Toast.makeText(this, messages.size()-1, Toast.LENGTH_SHORT).show();
       // binding.recyclerView.smoothScrollToPosition(messages.size());
    }
    @Override
    protected void onPause() {
        Date date=new Date();

        HashMap<String, Object> lastSeenObj = new HashMap<>();
        lastSeenObj.put("lastSeenTime", date.getTime());
        database.getReference().child("chats").child(senderRoom).updateChildren(lastSeenObj);
        database.getReference().child("chats").child(receiverRoom).updateChildren(lastSeenObj);
        String currentId= FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Offline");
        super.onPause();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
package com.example.dell.chatsapp.Adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dell.chatsapp.Models.Message;
import com.example.dell.chatsapp.R;
import com.example.dell.chatsapp.databinding.DeleteDialogBinding;
import com.example.dell.chatsapp.databinding.ItemReceiveBinding;
import com.example.dell.chatsapp.databinding.ItemSendBinding;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class MessagesAdapter extends RecyclerView.Adapter {
    Context context;
    ArrayList<Message> messages;
    final  int ITEM_SENT=1;
    final  int ITEM_RECEIVE=2;

    String senderRoom;
    String receiverRoom;
    int count=0;


    public MessagesAdapter(Context context, ArrayList<Message> messages, String senderRoom,String receiverRoom){
        this.context=context;
        this.messages=messages;
        this.senderRoom=senderRoom;
        this.receiverRoom=receiverRoom;

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType== ITEM_SENT){
            View view= LayoutInflater.from(context).inflate(R.layout.item_send,parent,false);
            return new SentViewHolder(view);
        }else{
            View view= LayoutInflater.from(context).inflate(R.layout.item_receive,parent,false);
            return new ReceiverViewHolder(view);
        }

    }
    public void setClipboard(Context context, String text) {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
            Toast.makeText(context, "message copied", Toast.LENGTH_SHORT).show();
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "message copied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message=messages.get(position);
        if(FirebaseAuth.getInstance().getUid().equals(message.getSenderId())){
            return ITEM_SENT;
        }else {
            return ITEM_RECEIVE;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message= messages.get(position);
        int reactions[]=new int[]{
                R.drawable.ic_fb_like,
                R.drawable.ic_fb_love,
                R.drawable.ic_fb_laugh,
                R.drawable.ic_fb_wow,
                R.drawable.ic_fb_sad,
                R.drawable.ic_fb_angry
        };

        ReactionsConfig config = new ReactionsConfigBuilder(context)
                .withReactions(reactions)
                .build();


        ReactionPopup popup = new ReactionPopup(context, config, (pos) -> {
            if(holder.getClass() == SentViewHolder.class ){
              try {
                  SentViewHolder viewHolder = (SentViewHolder) holder;


                  viewHolder.binding.feeling.setImageResource(reactions[pos]);
                  viewHolder.binding.feeling.setVisibility(View.VISIBLE);
              } catch (Exception e) {
                  e.printStackTrace();
              }

            }
            else {
                try {
                    ReceiverViewHolder viewHolder = (ReceiverViewHolder) holder;
                    viewHolder.binding.feeling.setImageResource(reactions[pos]);
                    viewHolder.binding.feeling.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }



//            HashMap<String, Object> total_messages = new HashMap<>();
//            total_messages.put("lastSeenTime", messages.size()-1);
//            FirebaseDatabase.getInstance().getReference().child("chats").child(senderRoom).updateChildren(total_messages);
//            FirebaseDatabase.getInstance().getReference().child("chats").child(receiverRoom).updateChildren(total_messages);

            message.setFeeling(pos);
            FirebaseDatabase.getInstance().getReference()
                    .child("chats")
                    .child(senderRoom)
                    .child("messages")
                    .child(message.getMessageId()).setValue(message);
            FirebaseDatabase.getInstance().getReference()
                    .child("chats")
                    .child(receiverRoom)
                    .child("messages")
                    .child(message.getMessageId()).setValue(message);
            return true; // true is closing popup, false is requesting a new selection
        });
        if(holder.getClass() == SentViewHolder.class){
            SentViewHolder viewHolder=(SentViewHolder)holder;

            if(message.getMessage().equals("photo")){
                viewHolder.binding.image.setVisibility(View.VISIBLE);
                viewHolder.binding.message.setVisibility(View.GONE);
                Glide.with(context).load(message.getImageUrl()).placeholder(R.drawable.placeholder).into(viewHolder.binding.image);
            }

            if(message.getMessage().equals("video")){
                viewHolder.binding.videoView2.setVisibility(View.VISIBLE);
                viewHolder.binding.message.setVisibility(View.GONE);
                Uri uri= Uri.parse(String.valueOf(message.getVideoUrl()));
                MediaController mediaController=new MediaController(context);
                mediaController.setAnchorView( viewHolder.binding.videoView2);
                viewHolder.binding.videoView2.setVideoURI(uri);
                viewHolder.binding.videoView2.setMediaController(mediaController);
                viewHolder.binding.videoView2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        viewHolder.binding.videoView2.start();
                    }
                });
            }



            viewHolder.binding.message.setText(message.getMessage());

            if(message.getFeeling()>=0){
                viewHolder.binding.feeling.setImageResource(reactions[message.getFeeling()]);
                viewHolder.binding.feeling.setVisibility(View.VISIBLE);
            }else {
                viewHolder.binding.feeling.setVisibility(View.GONE);

            }

            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    View view1 = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null);
                    DeleteDialogBinding binding = DeleteDialogBinding.bind(view1);
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setView(binding.getRoot())
                            .create();
                    binding.everyone.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            message.setMessage("This message is removed.");
                            message.setFeeling(-1);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(message);

                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(receiverRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(message);
                            dialog.dismiss();
                        }
                    });

                    binding.delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(null);
                            dialog.dismiss();
                        }
                    });

                    binding.cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    binding.copy.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            setClipboard(context,message.getMessage());
                        }
                    });

                    dialog.show();

                    return false;
                }
            });


//            viewHolder.binding.message.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    popup(view,position);
//                }
//            });


           viewHolder.binding.message.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {

                        popup.onTouch(view, event);
                        return false;


                }
            });


            viewHolder.binding.image.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {

                    popup.onTouch(view, event);
                    return false;


                }
            });
            viewHolder.binding.videoView2.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {

                    popup.onTouch(view, event);
                    return false;


                }
            });

        }else {
            ReceiverViewHolder viewHolder=(ReceiverViewHolder) holder;

            if(message.getMessage().equals("photo")){
                viewHolder.binding.image.setVisibility(View.VISIBLE);
                viewHolder.binding.message.setVisibility(View.GONE);
                Glide.with(context).load(message.getImageUrl()).placeholder(R.drawable.placeholder).into(viewHolder.binding.image);
            }
            if(message.getMessage().equals("video")){
                viewHolder.binding.videoView2.setVisibility(View.VISIBLE);
                viewHolder.binding.message.setVisibility(View.GONE);
                Uri uri= Uri.parse(String.valueOf(message.getVideoUrl()));
                MediaController mediaController=new MediaController(context);
                mediaController.setAnchorView( viewHolder.binding.videoView2);
                viewHolder.binding.videoView2.setVideoURI(uri);
                viewHolder.binding.videoView2.setMediaController(mediaController);
                viewHolder.binding.videoView2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        viewHolder.binding.videoView2.start();
                    }
                });
            }


            viewHolder.binding.message.setText(message.getMessage());

            if(message.getFeeling()>=0){
                //  message.setFeeling(reactions[(int)message.getFeeling()]);
                viewHolder.binding.feeling.setImageResource(reactions[message.getFeeling()]);
                viewHolder.binding.feeling.setVisibility(View.VISIBLE);
            }else {
                viewHolder.binding.feeling.setVisibility(View.GONE);

            }

            viewHolder.binding.message.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {

                    popup.onTouch(view, event);
                    return false;


                }
            });


            viewHolder.binding.image.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {

                    popup.onTouch(view, event);
                    return false;


                }
            });

            viewHolder.binding.videoView2.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {

                    popup.onTouch(view, event);
                    return false;


                }
            });


            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    View view1 = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null);
                    DeleteDialogBinding binding = DeleteDialogBinding.bind(view1);
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setView(binding.getRoot())
                            .create();
                    binding.everyone.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            message.setMessage("This message is removed.");
                            message.setFeeling(-1);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(message);

                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(receiverRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(message);
                            dialog.dismiss();
                        }
                    });

                    binding.delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(null);
                            dialog.dismiss();
                        }
                    });

                    binding.cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    binding.copy.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            setClipboard(context,message.getMessage());
                        }
                    });

                    dialog.show();

                    return false;
                }
            });

        }

    }



    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class SentViewHolder extends RecyclerView.ViewHolder {

        ItemSendBinding binding;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            binding=ItemSendBinding.bind(itemView);
        }
    }

    public class ReceiverViewHolder extends RecyclerView.ViewHolder{

        ItemReceiveBinding binding;

        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            binding=ItemReceiveBinding.bind(itemView);
        }
    }
}

package com.example.dell.chatsapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.example.dell.chatsapp.R;
import com.example.dell.chatsapp.databinding.ActivityVideoCallBinding;

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.URL;

public class Video_Call extends AppCompatActivity {
ActivityVideoCallBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityVideoCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.imageView4.setImageResource(R.drawable.meeting);
        getSupportActionBar().setTitle("Video Call Conferencing");

        URL serverURL;

        try{
            serverURL=new URL("https://meet.jit.si");
            JitsiMeetConferenceOptions defaultOptions=
                    new JitsiMeetConferenceOptions.Builder().setServerURL(serverURL)
                            .setWelcomePageEnabled(false)
                            .build();
            JitsiMeet.setDefaultConferenceOptions(defaultOptions);


        } catch (Exception e) {
            e.printStackTrace();
        }


        binding.join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JitsiMeetConferenceOptions options= new JitsiMeetConferenceOptions.Builder().
                        setRoom(binding.secretCode.toString())
                        .setWelcomePageEnabled(false)
                        .build();

                JitsiMeetActivity.launch(Video_Call.this,options);
            }
        });
    }
}
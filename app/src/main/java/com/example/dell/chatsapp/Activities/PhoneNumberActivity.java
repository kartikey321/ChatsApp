package com.example.dell.chatsapp.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.dell.chatsapp.databinding.ActivityPhoneNumberBinding;
import com.google.firebase.auth.FirebaseAuth;

public class PhoneNumberActivity extends AppCompatActivity {
    ActivityPhoneNumberBinding binding;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        binding=ActivityPhoneNumberBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth=FirebaseAuth.getInstance();
        if(auth.getCurrentUser()!=null){
            Intent intent=new Intent(PhoneNumberActivity.this,MainActivity.class);
            startActivity(intent);
            finish();
        }

        getSupportActionBar().hide();

        binding.phonebox.requestFocus();

        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String mobile =binding.phonebox.getText().toString().trim();

                if(mobile.isEmpty() || mobile.length() < 10){
                    binding.phonebox.setError("Enter a valid mobile");
                    binding.phonebox.requestFocus();
                    return;
                }
                Intent intent=new Intent(PhoneNumberActivity.this,OTPActivity.class);
                intent.putExtra("phoneNumber",binding.phonebox.getText().toString());
                startActivity(intent);
            }
        });
    }
}
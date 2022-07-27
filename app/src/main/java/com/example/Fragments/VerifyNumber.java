package com.example.Fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.chaos.view.PinView;
import com.example.Model.UserModel;
import com.example.project3.R;
import com.example.project3.databinding.FragmentVerifyNumberBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class VerifyNumber extends Fragment {
    //private FragmentVerifyNumberBinding binding;
    private FragmentVerifyNumberBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private String OTPcode, phoneNumber;
    private static PinView otpTextViewPinView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_verify_number, container, false); //inflate fragment_verify_number.xml
        View view = binding.getRoot();
        otpTextViewPinView = view.findViewById(R.id.otp_text_view);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar); //set toolbar properties
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbar.setBackgroundColor(Color.parseColor("#212529"));
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.back_arrow));
        //((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        //((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //toolbar.setNavigationOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View v) {
        //        getActivity().getSupportFragmentManager().popBackStack();
        //    }
        //});
        firebaseAuth = FirebaseAuth.getInstance(); //receive an instance to fire base
        databaseReference = FirebaseDatabase.getInstance().getReference("Users"); //get reference to users in fire base
        checkPermissions(Manifest.permission.RECEIVE_SMS);
        //get bundle with the phone number and verification code from GetNumber fragment
        Bundle bundle = getArguments();
        if (bundle != null) {
            OTPcode = bundle.getString("VERIFICATION_CODE");
            phoneNumber = bundle.getString("phoneNumber");
        }
        view.findViewById(R.id.verifyButton).setOnClickListener(view1 -> { //set listener for click on the verify button
            if(checkOTPcode(binding.otpTextView.getText().toString().trim())) { //check if the opt code is valid
                binding.otpTextView.setVisibility(View.GONE);
                binding.verifyButton.setVisibility(View.GONE);
                binding.spinKit2.setVisibility(View.VISIBLE);
                verifyPhoneNumberWithCode(binding.otpTextView.getText().toString().trim());
            }
            else
                Toast.makeText(getContext(), "Invalid OTP code", Toast.LENGTH_SHORT).show();
        });

        binding.resendOTPcode.setOnClickListener(view2 -> { //set listener for click on the resend opt option
            PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBack = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                }
                //if sending new opt failed
                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    binding.otpTextView.setVisibility(View.VISIBLE);
                    binding.verifyButton.setVisibility(View.VISIBLE);
                    binding.spinKit2.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Sending OTP code failed", Toast.LENGTH_SHORT).show();
                }
                //send new opt code
                @Override
                public void onCodeSent(@NonNull String verificationID, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                    Toast.makeText(getContext(), "Sending new OTP code", Toast.LENGTH_SHORT).show();
                }
            };
            PhoneAuthOptions options =
                    PhoneAuthOptions.newBuilder(firebaseAuth)
                            .setPhoneNumber(phoneNumber)       // Phone number to verify
                            .setTimeout(30L, TimeUnit.SECONDS) // Timeout and unit
                            .setActivity(getActivity())                 // Activity (for callback binding)
                            .setCallbacks(mCallBack)          // OnVerificationStateChangedCallbacks
                            .build();
            PhoneAuthProvider.verifyPhoneNumber(options);

        });
        //check if opt code length is valid (==6) and calls the OnClickListener of verify button
//        binding.otpTextView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(binding.otpTextView.getText().toString().length() == 6)
//                    binding.verifyButton.performClick();
//            }
//        });
        binding.otpTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void afterTextChanged(Editable editable) {
                if(binding.otpTextView.getText().toString().length() == 6)
                    binding.verifyButton.performClick();
            }
        });

        return view;
    }

    private boolean checkOTPcode(String s) { //return true if otp code length is 6
        return s.length() == 6;
    }

    private void verifyPhoneNumberWithCode(String code) { //compares the user opt code with the sent code and allow user to continue if its valid
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(OTPcode, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) { //if signInWithCredential task completed successfully
                        UserModel userModel = new UserModel("", "", "", firebaseAuth.getCurrentUser().getPhoneNumber(),
                                firebaseAuth.getUid(), "online", "false", task.getResult().toString(),""); //crate new model for user according UserModel POJO and initialize fields

                        //after initialize complete,replace the fragment with UserData fragment (That fragment allows user to fill his profile information: first and last name & image)
                        databaseReference.child(firebaseAuth.getUid()).setValue(userModel).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    FragmentManager fm = getActivity().getSupportFragmentManager();
                                    FragmentTransaction ft = fm.beginTransaction();
                                    Fragment f = new UserData();
                                    ft.replace(R.id.LoginContainer, f).addToBackStack("UserDataNumberFragment").commit();
                                } else
                                    Toast.makeText(getContext(), "" + task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // Sign in failed, display a message and update the UI
                        Toast.makeText(getContext(), "" + task.getResult(), Toast.LENGTH_SHORT).show();
                        Log.w("TAG", "signInWithCredential: failure", task.getException());
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(getContext(), "Invalid entered OTP code" + task.getException(), Toast.LENGTH_SHORT).show(); // The verification code entered was invalid
                        }
                    }
                });
    }

    private void checkPermissions(String permission) {
        if (ContextCompat.checkSelfPermission(getContext(), permission) ==PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission)) {
            Toast.makeText(getContext(), "Grant SMS permission?", Toast.LENGTH_LONG).show();
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(permission);
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                } else
                    Toast.makeText(getContext(), "Grant SMS permission?", Toast.LENGTH_LONG).show();
            });


    public static class SMSbroadcast extends BroadcastReceiver {
        private String SMS = "android.provider.Telephony.SMS_RECEIVED";
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                SmsMessage smsMessage = msgs[0];
                if (smsMessage != null) {
                    String sender = smsMessage.getDisplayOriginatingAddress();
                    String smsChunk = smsMessage.getDisplayMessageBody();
                    Toast.makeText(context, smsChunk, Toast.LENGTH_SHORT).show();
                    String code = null;
                    if (smsChunk.contains("textme")) {
                        Toast.makeText(context, "hh", Toast.LENGTH_SHORT).show();
                        code = smsChunk.substring(0, 6);
                        otpTextViewPinView.setText(code);
                    }
                    Toast.makeText(context, code, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
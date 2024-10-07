package com.connectify.com.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.connectify.com.databinding.ActivityConnectingBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Objects;

public class ConnectingActivity extends AppCompatActivity {

    private ActivityConnectingBinding binding;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private boolean isOkay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        String profile = getIntent().getStringExtra("profile");
        Glide.with(this)
                .load(profile)
                .into(binding.profile);

        String username = auth.getUid();
        if (username == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findAvailableRoom(username);
    }

    private void findAvailableRoom(String username) {
        database.getReference().child("users")
                .orderByChild("status")
                .equalTo(0).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        if (snapshot.getChildrenCount() > 0) {
                            isOkay = true;
                            for (DataSnapshot childSnap : snapshot.getChildren()) {
                                handleAvailableRoom(childSnap, username);
                            }
                        } else {
                            createNewRoom(username);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {
                        Toast.makeText(ConnectingActivity.this, "Error finding room: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleAvailableRoom(DataSnapshot childSnap, String username) {
        String childKey = Objects.requireNonNull(childSnap.getKey());
        database.getReference()
                .child("users")
                .child(childKey)
                .child("incoming")
                .setValue(username);
        database.getReference()
                .child("users")
                .child(childKey)
                .child("status")
                .setValue(1);

        Intent intent = new Intent(ConnectingActivity.this, CallActivity.class);
        String incoming = childSnap.child("incoming").getValue(String.class);
        String createdBy = childSnap.child("createdBy").getValue(String.class);
        boolean isAvailable = Boolean.TRUE.equals(childSnap.child("isAvailable").getValue(Boolean.class));

        intent.putExtra("username", username);
        intent.putExtra("incoming", incoming);
        intent.putExtra("createdBy", createdBy);
        intent.putExtra("isAvailable", isAvailable);

        startActivity(intent);
        finish();
    }

    private void createNewRoom(String username) {
        HashMap<String, Object> room = new HashMap<>();
        room.put("incoming", username);
        room.put("createdBy", username);
        room.put("isAvailable", true);
        room.put("status", 0);

        database.getReference()
                .child("users")
                .child(username)
                .setValue(room)
                .addOnSuccessListener(unused -> monitorRoomStatus(username))
                .addOnFailureListener(e -> Toast.makeText(ConnectingActivity.this, "Error creating room: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void monitorRoomStatus(String username) {
        database.getReference()
                .child("users")
                .child(username)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        if (snapshot.child("status").exists() && snapshot.child("status").getValue(Integer.class) == 1) {
                            if (isOkay) return;

                            isOkay = true;
                            Intent intent = new Intent(ConnectingActivity.this, CallActivity.class);
                            String incoming = snapshot.child("incoming").getValue(String.class);
                            String createdBy = snapshot.child("createdBy").getValue(String.class);
                            boolean isAvailable = Boolean.TRUE.equals(snapshot.child("isAvailable").getValue(Boolean.class));

                            intent.putExtra("username", username);
                            intent.putExtra("incoming", incoming);
                            intent.putExtra("createdBy", createdBy);
                            intent.putExtra("isAvailable", isAvailable);

                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {
                        Toast.makeText(ConnectingActivity.this, "Error monitoring room: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

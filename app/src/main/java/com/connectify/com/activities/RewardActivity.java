package com.connectify.com.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.connectify.com.R;
import com.connectify.com.databinding.ActivityRewardBinding;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RewardActivity extends AppCompatActivity {

    private ActivityRewardBinding binding;
    private RewardedAd mRewardedAd;
    private FirebaseDatabase database;
    private String currentUid;
    private int coins = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRewardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid();

        if (currentUid == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadAd();
        fetchUserCoins();

        binding.video1.setOnClickListener(view -> showRewardedAd());
    }

    private void loadAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(this, "ca-app-pub-3422414103621909/2335080063", adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mRewardedAd = null;
                Toast.makeText(RewardActivity.this, "Failed to load ad", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                mRewardedAd = rewardedAd;
            }
        });
    }

    private void fetchUserCoins() {
        database.getReference().child("profiles")
                .child(currentUid)
                .child("coins")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Long coinsValue = snapshot.getValue(Long.class);
                            if (coinsValue != null) {
                                coins = coinsValue.intValue();
                            } else {
                                coins = 0;
                            }
                        } else {
                            coins = 0;
                        }
                        binding.coins.setText(String.valueOf(coins));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(RewardActivity.this, "Failed to load user coins", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRewardedAd() {
        if (mRewardedAd != null) {
            Activity activityContext = RewardActivity.this;
            mRewardedAd.show(activityContext, rewardItem -> {
                loadAd();
                updateCoins(20);
                binding.video1Icon.setImageResource(R.drawable.check);
            });
        } else {
            Toast.makeText(this, "Ad is not loaded yet", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCoins(int additionalCoins) {
        coins += additionalCoins;
        database.getReference().child("profiles")
                .child(currentUid)
                .child("coins")
                .setValue(coins)
                .addOnSuccessListener(aVoid -> binding.coins.setText(String.valueOf(coins)))
                .addOnFailureListener(e -> Toast.makeText(RewardActivity.this, "Failed to update coins", Toast.LENGTH_SHORT).show());
    }
}

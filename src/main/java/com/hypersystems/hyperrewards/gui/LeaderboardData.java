package com.hypersystems.hyperrewards.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class LeaderboardData {
    public static final BuilderCodec<LeaderboardData> CODEC = BuilderCodec
            .builder(LeaderboardData.class, LeaderboardData::new)
            .addField(
                    new KeyedCodec<>("Action", Codec.STRING),
                    (data, value) -> data.action = value,
                    data -> data.action
            )
            .build();

    public String action;

    public LeaderboardData() {}
}

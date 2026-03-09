package com.hypersystems.hyperrewards.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class AdminActionData {
    public static final BuilderCodec<AdminActionData> CODEC = BuilderCodec
            .builder(AdminActionData.class, AdminActionData::new)
            .addField(
                    new KeyedCodec<>("Action", Codec.STRING),
                    (data, value) -> data.action = value,
                    data -> data.action
            )
            .addField(
                    new KeyedCodec<>("TargetId", Codec.STRING),
                    (data, value) -> data.targetId = value,
                    data -> data.targetId
            )
            .build();

    public String action;
    public String targetId;

    public AdminActionData() {}
}

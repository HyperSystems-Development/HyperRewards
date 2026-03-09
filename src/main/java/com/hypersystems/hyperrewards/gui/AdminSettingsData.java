package com.hypersystems.hyperrewards.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class AdminSettingsData {
    public static final BuilderCodec<AdminSettingsData> CODEC = BuilderCodec
            .builder(AdminSettingsData.class, AdminSettingsData::new)
            .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .addField(new KeyedCodec<>("@RestEnabled", Codec.STRING), (d, v) -> d.restEnabled = v, d -> d.restEnabled)
            .addField(new KeyedCodec<>("@RestInterval", Codec.STRING), (d, v) -> d.restInterval = v, d -> d.restInterval)
            .addField(new KeyedCodec<>("@RestMessage", Codec.STRING), (d, v) -> d.restMessage = v, d -> d.restMessage)
            .build();

    public String action;
    public String restEnabled;
    public String restInterval;
    public String restMessage;

    public AdminSettingsData() {}
}

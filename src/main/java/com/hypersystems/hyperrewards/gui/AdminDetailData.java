package com.hypersystems.hyperrewards.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class AdminDetailData {
    public static final BuilderCodec<AdminDetailData> CODEC = BuilderCodec
            .builder(AdminDetailData.class, AdminDetailData::new)
            .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .addField(new KeyedCodec<>("TargetId", Codec.STRING), (d, v) -> d.targetId = v, d -> d.targetId)
            .addField(new KeyedCodec<>("@Id", Codec.STRING), (d, v) -> d.id = v, d -> d.id)
            .addField(new KeyedCodec<>("@Period", Codec.STRING), (d, v) -> d.period = v, d -> d.period)
            .addField(new KeyedCodec<>("@Time", Codec.STRING), (d, v) -> d.timeMinutes = v, d -> d.timeMinutes)
            .addField(new KeyedCodec<>("@Broadcast", Codec.STRING), (d, v) -> d.broadcastMessage = v, d -> d.broadcastMessage)
            .addField(new KeyedCodec<>("@Private", Codec.STRING), (d, v) -> d.privateMessage = v, d -> d.privateMessage)
            .addField(new KeyedCodec<>("@Group", Codec.STRING), (d, v) -> d.addToGroup = v, d -> d.addToGroup)
            .addField(new KeyedCodec<>("@Repeatable", Codec.STRING), (d, v) -> d.repeatable = v, d -> d.repeatable)
            .addField(new KeyedCodec<>("@Command", Codec.STRING), (d, v) -> d.commandText = v, d -> d.commandText)
            .addField(new KeyedCodec<>("@Perm", Codec.STRING), (d, v) -> d.permText = v, d -> d.permText)
            .addField(new KeyedCodec<>("RemoveIndex", Codec.STRING), (d, v) -> d.removeIndex = v, d -> d.removeIndex)
            .build();

    public String action;
    public String targetId;
    public String id;
    public String period;
    public String timeMinutes;
    public String broadcastMessage;
    public String privateMessage;
    public String addToGroup;
    public String repeatable;
    public String commandText;
    public String permText;
    public String removeIndex;

    public AdminDetailData() {}
}

package com.hypersystems.hyperrewards.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class PageActionData {
    public static final BuilderCodec<PageActionData> CODEC = BuilderCodec
            .builder(PageActionData.class, PageActionData::new)
            .addField(
                    new KeyedCodec<>("Action", Codec.STRING),
                    (data, value) -> data.action = value,
                    data -> data.action
            )
            .build();

    public String action;

    public PageActionData() {}
}

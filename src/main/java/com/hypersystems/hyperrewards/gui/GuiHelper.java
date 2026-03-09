package com.hypersystems.hyperrewards.gui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GuiHelper {

    public static void navigate(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                InteractiveCustomUIPage<?> page) {
        player.getPageManager().openCustomPage(ref, store, page);
    }
}

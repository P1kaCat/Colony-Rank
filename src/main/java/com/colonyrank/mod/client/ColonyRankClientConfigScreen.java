package com.colonyrank.mod.client;

import com.colonyrank.mod.config.ColonyRankGameConfig;
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.IConfigScreenFactory;
import net.minecraftforge.fml.ModLoadingContext;

@OnlyIn(Dist.CLIENT)
public final class ColonyRankClientConfigScreen {
    private ColonyRankClientConfigScreen() {
    }

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
            () -> (container, modListScreen) -> {
                var configScreen = ClientConfigRegistry.INSTANCE.provideScreen$fzzy_config(ColonyRankGameConfig.CONFIG_SCREEN_SCOPE);
                return configScreen != null ? configScreen : modListScreen;
            }
        );
    }
}

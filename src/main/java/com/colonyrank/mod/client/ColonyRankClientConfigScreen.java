package com.colonyrank.mod.client;

import com.colonyrank.mod.config.ColonyRankGameConfig;
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@OnlyIn(Dist.CLIENT)
public final class ColonyRankClientConfigScreen {
    private ColonyRankClientConfigScreen() {
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
            IConfigScreenFactory.class,
            (IConfigScreenFactory) (container, modListScreen) -> {
                var configScreen = ClientConfigRegistry.INSTANCE.provideScreen$fzzy_config(ColonyRankGameConfig.CONFIG_SCREEN_SCOPE);
                return configScreen != null ? configScreen : modListScreen;
            }
        );
    }
}

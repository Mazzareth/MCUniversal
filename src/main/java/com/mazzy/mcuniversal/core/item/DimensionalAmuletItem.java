package com.mazzy.mcuniversal.core.item;

import com.mazzy.mcuniversal.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DimensionalAmuletItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(DimensionalAmuletItem.class);

    public DimensionalAmuletItem(Properties properties) {
        super(properties);
    }

    public DimensionalAmuletItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        LOGGER.info("DimensionalAmuletItem right-clicked.");

        // Check if we’re on the server side
        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
                LOGGER.info("Opening Amulet Screen for player {} with up-to-date dimension data.",
                        serverPlayer.getName().getString());

                // Updated fix: send the full dimension list every time
                NetworkHandler.openAmuletScreenForPlayer(serverPlayer);
            }
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
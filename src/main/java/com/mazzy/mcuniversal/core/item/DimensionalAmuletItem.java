package com.mazzy.mcuniversal.core.item;

import com.mazzy.mcuniversal.network.NetworkHandler;
import com.mazzy.mcuniversal.network.OpenDimensionalAmuletPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        // Log to confirm it’s being used
        LOGGER.info("DimensionalAmuletItem right-clicked.");

        // Check if we’re on the server side
        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
                // Log to confirm packet is about to be sent
                LOGGER.info("Sending OpenDimensionalAmuletPacket to player {}.", serverPlayer.getName().getString());

                // Send the packet to the player
                NetworkHandler.sendToPlayer(new OpenDimensionalAmuletPacket(), serverPlayer);
            }
        }

        // Return success to allow further processing (like animations)
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}

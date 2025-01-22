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

/**
 * Special item that opens dimension selection interface when right-clicked
 * Handles server-side activation and client-screen synchronization
 */
public class DimensionalAmuletItem extends Item {
    // Logger for tracking device usage diagnostics
    private static final Logger LOGGER = LoggerFactory.getLogger(DimensionalAmuletItem.class);

    /** Default constructor with single-item stack size */
    public DimensionalAmuletItem() {
        super(new Item.Properties().stacksTo(1));
    }

    /** Alternative constructor for custom properties */
    public DimensionalAmuletItem(Properties properties) {
        super(properties);
    }

    /**
     * Handles right-click interaction with the amulet
     * @return Success result maintaining item in hand
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        LOGGER.info("DimensionalAmuletItem right-clicked.");

        // Server-side handling only - screen opening is client-bound
        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
                LOGGER.info("Opening Amulet Screen for player {} with up-to-date dimension data.",
                        serverPlayer.getName().getString());

                // Network sync to trigger client screen display
                NetworkHandler.openAmuletScreenForPlayer(serverPlayer);
            }
        }

        // Maintain item in hand on both client and server
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
package com.mazzy.mcuniversal.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Creates dramatic consequences for attempting to sleep in beds within the Overworld.
 * When players try to sleep in Overworld beds, triggers an explosion and destroys the bed.
 */
@Mod.EventBusSubscriber
public class OverworldBedExplosionHandler {

    /**
     * Handles bed sleep attempts with explosive results in the Overworld
     * @param event PlayerSleepInBedEvent containing sleep attempt details
     */
    @SubscribeEvent
    public static void onPlayerTryingToSleep(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        // Only process server-side events for real players
        if (player == null || player.level().isClientSide()) {
            return;
        }

        // Restrict explosive beds to Overworld dimension only
        if (player.level().dimension() == Level.OVERWORLD) {
            BlockPos bedPos = event.getPos();
            if (bedPos == null) {
                return;
            }

            // Prevent normal bed functionality
            event.setResult(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);

            ServerLevel serverLevel = (ServerLevel) player.level();
            // Remove bed block without item drops if present
            if (serverLevel.getBlockState(bedPos).getBlock() instanceof BedBlock) {
                serverLevel.destroyBlock(bedPos, false, player);
            }

            // Calculate precise explosion center at bed coordinates
            Vec3 explosionPos = new Vec3(
                    bedPos.getX() + 0.5D,
                    bedPos.getY() + 0.5D,
                    bedPos.getZ() + 0.5D
            );

            // Create customized explosion with:
            // - 5 block radius
            // - Fire generation
            // - Block interaction damage
            // - Special damage source type
            serverLevel.explode(
                    /* entity                */ null,
                    /* damageSource          */ serverLevel.damageSources().badRespawnPointExplosion(explosionPos),
                    /* explosionCalculator   */ null,
                    /* x                     */ explosionPos.x,
                    /* y                     */ explosionPos.y,
                    /* z                     */ explosionPos.z,
                    /* explosionRadius       */ 5.0F,
                    /* causesFire            */ true,
                    /* explosionInteraction  */ Level.ExplosionInteraction.BLOCK
            );
        }
    }
}
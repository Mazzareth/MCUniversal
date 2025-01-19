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

@Mod.EventBusSubscriber
public class OverworldBedExplosionHandler {

    @SubscribeEvent
    public static void onPlayerTryingToSleep(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide()) {
            return;
        }

        // Only explode beds in the Overworld
        if (player.level().dimension() == Level.OVERWORLD) {
            BlockPos bedPos = event.getPos();
            if (bedPos == null) {
                return;
            }

            // Cancel normal bed behavior (no sleeping, no respawn point set)
            event.setResult(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);

            // Destroy the bed if it's actually a bed block
            ServerLevel serverLevel = (ServerLevel) player.level();
            if (serverLevel.getBlockState(bedPos).getBlock() instanceof BedBlock) {
                serverLevel.destroyBlock(bedPos, false, player);
            }

            // Create an explosion (similar to the Nether bed explosion)
            Vec3 explosionPos = new Vec3(
                    bedPos.getX() + 0.5D,
                    bedPos.getY() + 0.5D,
                    bedPos.getZ() + 0.5D
            );

            // In 1.20.1, the final parameter is Level.ExplosionInteraction,
            // with valid values like NONE, BLOCK, MOB, or TNT.
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
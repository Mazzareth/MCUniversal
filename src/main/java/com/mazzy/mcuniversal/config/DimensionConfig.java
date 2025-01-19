/*******************************************************************************
 * DimensionConfig.java
 ******************************************************************************/
package com.mazzy.mcuniversal.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber
public class DimensionConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ServerConfig SERVER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SERVER = new ServerConfig(builder);
        SERVER_SPEC = builder.build();
    }

    public static class ServerConfig {
        // A list of dimension IDs that can be randomly teleported to
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionWhitelist;

        public ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.push("randomTeleport");
            dimensionWhitelist = builder
                    .comment("List of dimension IDs that are whitelisted for random teleportation.")
                    // Default to only Overworld. Everything else is effectively locked.
                    .defineList(
                            "dimension_whitelist",
                            Collections.singletonList("minecraft:overworld"),
                            obj -> obj instanceof String
                    );
            builder.pop();
        }
    }
}
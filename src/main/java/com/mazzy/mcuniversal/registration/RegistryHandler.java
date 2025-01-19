/*******************************************************************************
 * RegistryHandler.java
 ******************************************************************************/
package com.mazzy.mcuniversal.registration;

import com.mazzy.mcuniversal.McUniversal;
import com.mazzy.mcuniversal.core.item.DimensionalAmuletItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = McUniversal.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegistryHandler {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, McUniversal.MODID);

    // The Dimensional Amulet item
    public static final RegistryObject<Item> DIMENSIONAL_AMULET =
            ITEMS.register("dimensional_amulet", DimensionalAmuletItem::new);

    // No additional changes needed here unless you want to register more items
}
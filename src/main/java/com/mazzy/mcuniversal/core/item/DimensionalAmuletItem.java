package com.mazzy.mcuniversal.core.item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class DimensionalAmuletItem extends Item {

    public DimensionalAmuletItem(Properties properties) {
		super(properties);
    }
public DimensionalAmuletItem(){
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
        );
}
}

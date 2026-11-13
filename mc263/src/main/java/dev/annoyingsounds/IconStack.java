package dev.annoyingsounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
final class IconStack {
    static ItemStack of(Item item) {
        var connection=Minecraft.getInstance().getConnection();
        // In 26.3 item defaults are supplied by the connected world's registry.
        if (connection==null) return ItemStack.EMPTY;
        return connection.registryAccess().lookupOrThrow(Registries.ITEM)
            .get(ResourceKey.create(Registries.ITEM,BuiltInRegistries.ITEM.getKey(item)))
            .map(ItemStack::new).orElse(ItemStack.EMPTY);
    }
}


package org.bukkit.craftbukkit.inventory;

import net.minecraft.inventory.InventoryMerchant;

import org.bukkit.inventory.MerchantInventory;

public class CraftInventoryMerchant extends CraftInventory implements MerchantInventory {
    public CraftInventoryMerchant(InventoryMerchant merchant) {
        super(merchant);
    }
}

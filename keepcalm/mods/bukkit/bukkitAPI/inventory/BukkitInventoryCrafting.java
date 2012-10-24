package keepcalm.mods.bukkit.bukkitAPI.inventory;

import keepcalm.mods.bukkit.bukkitAPI.item.BukkitItemStack;
import net.minecraft.src.CraftingManager;
import net.minecraft.src.IInventory;
import net.minecraft.src.InventoryCrafting;
import net.minecraft.src.ShapedRecipes;

import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.util.Java15Compat;

public class BukkitInventoryCrafting extends BukkitInventory implements CraftingInventory {
    private final IInventory resultInventory;

    public BukkitInventoryCrafting(InventoryCrafting inventory, IInventory resultInventory) {
        super(inventory);
        this.resultInventory = resultInventory;
    }

	public IInventory getResultInventory() {
        return resultInventory;
    }

    public IInventory getMatrixInventory() {
        return inventory;
    }

    @Override
    public int getSize() {
        return getResultInventory().getSizeInventory() + getMatrixInventory().getSizeInventory();
    }

    @Override
    public void setContents(ItemStack[] items) {
        int resultLen = getMatrixInventory().getSizeInventory();
        int len = getMatrixContents().length + resultLen; //TODO
        if (len > items.length) {
            throw new IllegalArgumentException("Invalid inventory size; expected " + len + " or less");
        }
        setContents(items[0], Java15Compat.Arrays_copyOfRange(items, 1, items.length));
    }

    @Override
    public ItemStack[] getContents() {
        ItemStack[] items = new ItemStack[getSize()];
        net.minecraft.src.ItemStack[] mcResultItems = null;

        int i = 0;
        for (i = 0; i < mcResultItems.length; i++ ) {
            items[i] = new BukkitItemStack(mcResultItems[i]);
        }

        net.minecraft.src.ItemStack[] mcItems = getMatrixContents();

        for (int j = 0; j < mcItems.length; j++) {
            items[i + j] = new BukkitItemStack(mcItems[j]);
        }

        return items;
    }

    public void setContents(ItemStack result, ItemStack[] contents) {
        setResult(result);
        setMatrix(contents);
    }

    @Override
    public BukkitItemStack getItem(int index) {
        if (index < getResultInventory().getSizeInventory()) {
            net.minecraft.src.ItemStack item = getResultInventory().getStackInSlot(index);
            return item == null ? null : new BukkitItemStack(item);
        } else {
            net.minecraft.src.ItemStack item = getMatrixInventory().getStackInSlot(index - getResultInventory().getSizeInventory());
            return item == null ? null : new BukkitItemStack(item);
        }
    }

    @Override
    public void setItem(int index, ItemStack item) {
        if (index < getResultInventory().getSizeInventory()) {
            getResultInventory().setInventorySlotContents(index, (item == null ? null : BukkitItemStack.createNMSItemStack(item)));
        } else {
            getMatrixInventory().setInventorySlotContents((index - getResultInventory().getSizeInventory()), (item == null ? null : BukkitItemStack.createNMSItemStack(item)));
        }
    }

    public ItemStack[] getMatrix() {
        ItemStack[] items = new ItemStack[getSize()];
        net.minecraft.src.ItemStack[] matrix = getMatrixContents();

        for (int i = 0; i < matrix.length; i++ ) {
            items[i] = new BukkitItemStack(matrix[i]);
        }

        return items;
    }

    public ItemStack getResult() {
        net.minecraft.src.ItemStack item = getResultInventory().getStackInSlot(0);
        if(item != null) return new BukkitItemStack(item);
        return null;
    }

    public void setMatrix(ItemStack[] contents) {
        if (getMatrixContents().length > contents.length) {
            throw new IllegalArgumentException("Invalid inventory size; expected " + getMatrixContents().length + " or less");
        }

        net.minecraft.src.ItemStack[] mcItems = getMatrixContents();

        for (int i = 0; i < mcItems.length; i++ ) {
            if (i < contents.length) {
                ItemStack item = contents[i];
                if (item == null || item.getTypeId() <= 0) {
                    mcItems[i] = null;
                } else {
                    mcItems[i] = BukkitItemStack.createNMSItemStack(item);
                }
            } else {
                mcItems[i] = null;
            }
        }
    }
    protected net.minecraft.src.ItemStack[] getResultInventoryContents() {
    	IInventory inv = getResultInventory();
    	net.minecraft.src.ItemStack[] items = new net.minecraft.src.ItemStack[inv.getSizeInventory()];
    	for (int i = 0; i < inv.getSizeInventory(); i++) {
    		items[i] = inv.getStackInSlot(i);
    	}
    	return items;
    }
    protected net.minecraft.src.ItemStack[] getMatrixContents() {
    	IInventory inv = getMatrixInventory();
    	net.minecraft.src.ItemStack[] items = new net.minecraft.src.ItemStack[inv.getSizeInventory()];
    	for (int i = 0; i < inv.getSizeInventory(); i++) {
    		items[i] = inv.getStackInSlot(0);
    	}
    	return items;
    }
    public void setResult(ItemStack item) {
        net.minecraft.src.ItemStack[] contents = getResultInventoryContents();
        if (item == null || item.getTypeId() <= 0) {
            contents[0] = null;
        } else {
            contents[0] = BukkitItemStack.createNMSItemStack(item);
        }
    }

    public Recipe getRecipe() {
    	net.minecraft.src.ItemStack[] stacks = new net.minecraft.src.ItemStack[9];
    	int height = 0;
    	int width = 0;
    	for (int i = 0; i < 9; i++ ) {
    		stacks[i] = ((InventoryCrafting) getInventory()).getStackInSlot(i);
    		switch(i) {
    		case 0:
    		case 3:
    		case 6:
    			height++;
    		case 2:
    		case 5:
    		case 8:
    			width++;
    		}
    	}
    	net.minecraft.src.ItemStack recipe = CraftingManager.getInstance().findMatchingRecipe((InventoryCrafting) getInventory());
    	if (recipe == null) {
    		return null;
    	}
    	ShapedRecipes j = new ShapedRecipes(width, height, stacks, recipe);
    	return new BukkitRecipe(j);
        
        //return recipe == null ? null : new BukkitRecipe();
    }
}
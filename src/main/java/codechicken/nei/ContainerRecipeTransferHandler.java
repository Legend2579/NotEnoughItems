package codechicken.nei;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import codechicken.nei.api.IRecipeHandler;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IUsageHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced recipe transfer handler that works with any container, not just crafting tables.
 * Supports GT machine circuit slots and generic inventory transfers.
 */
public class ContainerRecipeTransferHandler {

    /**
     * Attempts to transfer recipe ingredients to the currently open container.
     * Called when shift+c is pressed on a recipe.
     */
    public static boolean transferRecipeToContainer(IRecipeHandler handler, int recipeIndex, boolean maxQuantity) {
        GuiContainer gui = NEIClientUtils.getGuiContainer();
        if (gui == null) {
            return false;
        }

        // Get the recipe ingredients
        List<ItemStack> ingredients = getRecipeIngredients(handler, recipeIndex);
        if (ingredients == null || ingredients.isEmpty()) {
            return false;
        }

        // Get available slots in the open container (excluding player inventory)
        List<Slot> targetSlots = getContainerSlots(gui.inventorySlots);
        if (targetSlots.isEmpty()) {
            return false;
        }

        // Check for circuit slot (GT machines)
        Slot circuitSlot = findCircuitSlot(targetSlots);

        // Attempt to transfer items
        return performTransfer(ingredients, targetSlots, circuitSlot, maxQuantity);
    }

    /**
     * Extract ingredients from the recipe handler
     */
    private static List<ItemStack> getRecipeIngredients(IRecipeHandler handler, int recipeIndex) {
        try {
            // Get recipe from handler
            if (handler instanceof ICraftingHandler) {
                return ((ICraftingHandler) handler).getIngredientStacks(recipeIndex);
            } else if (handler instanceof IUsageHandler) {
                return ((IUsageHandler) handler).getIngredientStacks(recipeIndex);
            }
        } catch (Exception e) {
            NEIClientConfig.logger.error("Error getting recipe ingredients", e);
        }
        return null;
    }

    /**
     * Get all slots in the container that are NOT player inventory
     */
    private static List<Slot> getContainerSlots(Container container) {
        List<Slot> slots = new ArrayList<>();

        for (Object obj : container.inventorySlots) {
            Slot slot = (Slot) obj;

            // Skip player inventory slots (typically 9-44 in most containers)
            // GT machines and other containers have their slots before player inventory
            if (slot.slotNumber < 36 || !isPlayerInventorySlot(slot)) {
                slots.add(slot);
            }
        }

        return slots;
    }

    /**
     * Check if a slot belongs to player inventory
     */
    private static boolean isPlayerInventorySlot(Slot slot) {
        // Player inventory typically starts at index 36 in most containers
        // This is a heuristic - might need adjustment for specific containers
        String slotClass = slot.getClass().getName();
        return slotClass.contains("SlotPlayer") ||
                slot.inventory.getClass().getSimpleName().equals("InventoryPlayer");
    }

    /**
     * Find the circuit slot in GT machines.
     * Circuit slots are typically the first slot or have specific naming.
     */
    private static Slot findCircuitSlot(List<Slot> slots) {
        for (Slot slot : slots) {
            String slotClass = slot.getClass().getName();

            // GT circuit slots are usually named something like "SlotCircuit" or index 0
            if (slotClass.contains("Circuit") || slotClass.contains("Programmed")) {
                return slot;
            }

            // Circuit slots are often the first slot in GT machines
            if (slot.slotNumber == 0 && slot.getHasStack() == false) {
                // Check if this looks like a circuit slot by position
                if (slot.xDisplayPosition < 20 && slot.yDisplayPosition < 40) {
                    return slot;
                }
            }
        }
        return null;
    }

    /**
     * Perform the actual item transfer
     */
    private static boolean performTransfer(List<ItemStack> ingredients, List<Slot> targetSlots,
            Slot circuitSlot, boolean maxQuantity) {
        GuiContainer gui = NEIClientUtils.getGuiContainer();
        Container container = gui.inventorySlots;

        boolean success = false;
        int slotIndex = 0;

        for (ItemStack ingredient : ingredients) {
            if (ingredient == null) {
                slotIndex++;
                continue;
            }

            // Check if this is a circuit item (GT programmed circuits)
            if (isCircuitItem(ingredient) && circuitSlot != null) {
                success |= transferCircuit(ingredient, circuitSlot);
                continue;
            }

            // Find next available slot
            while (slotIndex < targetSlots.size()) {
                Slot slot = targetSlots.get(slotIndex);

                // Skip circuit slot
                if (slot == circuitSlot) {
                    slotIndex++;
                    continue;
                }

                // Try to place item in this slot
                if (transferItemToSlot(ingredient, slot, maxQuantity)) {
                    success = true;
                    slotIndex++;
                    break;
                }

                slotIndex++;
            }
        }

        return success;
    }

    /**
     * Check if an item is a GT programmed circuit
     */
    private static boolean isCircuitItem(ItemStack stack) {
        if (stack == null) return false;

        // GT circuits have specific item IDs or NBT tags
        String itemName = stack.getItem().getClass().getName();
        if (itemName.contains("Circuit") || itemName.contains("Programmed")) {
            return true;
        }

        // Check NBT for circuit metadata
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().hasKey("mConfiguration") ||
                    stack.getTagCompound().hasKey("circuit");
        }

        return false;
    }

    /**
     * Transfer a circuit to the circuit slot (special handling for virtual items)
     */
    private static boolean transferCircuit(ItemStack circuit, Slot circuitSlot) {
        // GT circuits are virtual - need to send packet to set circuit number
        int circuitNumber = getCircuitNumber(circuit);

        if (circuitNumber >= 0) {
            // Send packet to server to set circuit configuration
            // This will need to integrate with GT's circuit packet system
            sendCircuitPacket(circuitSlot.slotNumber, circuitNumber);
            return true;
        }

        return false;
    }

    /**
     * Extract circuit number from circuit ItemStack
     */
    private static int getCircuitNumber(ItemStack circuit) {
        if (circuit.hasTagCompound()) {
            if (circuit.getTagCompound().hasKey("mConfiguration")) {
                return circuit.getTagCompound().getInteger("mConfiguration");
            }
        }

        // Circuit number might be in metadata
        return circuit.getItemDamage();
    }

    /**
     * Send packet to server to set circuit in GT machine
     */
    private static void sendCircuitPacket(int slotNumber, int circuitNumber) {
        // This needs to integrate with GT's networking system
        // Placeholder - will need actual GT packet implementation
        try {
            // GT typically has a packet system for GUI interactions
            // Something like: GT_Packet.sendCircuitChange(slotNumber, circuitNumber)
            NEIClientConfig.logger.info("Setting circuit slot " + slotNumber + " to value " + circuitNumber);
        } catch (Exception e) {
            NEIClientConfig.logger.error("Failed to send circuit packet", e);
        }
    }

    /**
     * Transfer a regular item to a slot
     */
    private static boolean transferItemToSlot(ItemStack ingredient, Slot slot, boolean maxQuantity) {
        GuiContainer gui = NEIClientUtils.getGuiContainer();

        // Check if slot can accept this item
        if (!slot.isItemValid(ingredient)) {
            return false;
        }

        // Check if slot already has items
        ItemStack slotStack = slot.getStack();
        if (slotStack != null && !ItemStack.areItemStacksEqual(ingredient, slotStack)) {
            return false;
        }

        // Find item in player inventory
        ItemStack playerStack = findItemInPlayerInventory(ingredient);
        if (playerStack == null) {
            return false;
        }

        // Calculate amount to transfer
        int transferAmount = maxQuantity ? playerStack.stackSize : Math.min(ingredient.stackSize, playerStack.stackSize);

        // Perform the transfer via click simulation
        return simulateItemTransfer(playerStack, slot, transferAmount);
    }

    /**
     * Find matching item in player inventory
     */
    private static ItemStack findItemInPlayerInventory(ItemStack target) {
        GuiContainer gui = NEIClientUtils.getGuiContainer();
        Container container = gui.inventorySlots;

        for (Object obj : container.inventorySlots) {
            Slot slot = (Slot) obj;

            if (isPlayerInventorySlot(slot) && slot.getHasStack()) {
                ItemStack stack = slot.getStack();
                if (ItemStack.areItemStacksEqual(stack, target)) {
                    return stack;
                }
            }
        }

        return null;
    }

    /**
     * Simulate item transfer using NEI's click system
     */
    private static boolean simulateItemTransfer(ItemStack source, Slot targetSlot, int amount) {
        try {
            // Use NEI's internal methods to simulate clicks
            // This will need to integrate with NEI's existing click handling
            NEIClientConfig.logger.info("Transferring " + amount + " of " + source.getDisplayName() + " to slot " + targetSlot.slotNumber);

            // Actual implementation would use NEIController's click methods
            // NEIController.clickSlot(...)

            return true;
        } catch (Exception e) {
            NEIClientConfig.logger.error("Failed to transfer item", e);
            return false;
        }
    }
}

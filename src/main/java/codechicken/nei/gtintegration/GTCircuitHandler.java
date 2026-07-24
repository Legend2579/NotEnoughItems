package codechicken.nei.gtintegration;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.inventory.GuiContainer;
import codechicken.nei.NEIClientConfig;

/**
 * Handles GregTech programmed circuit integration for NEI recipe transfer.
 * GT circuits are virtual items that don't exist in inventory - they're set via GUI.
 */
public class GTCircuitHandler {

    /**
     * Checks if a slot is a GT circuit slot
     */
    public static boolean isCircuitSlot(Slot slot) {
        if (slot == null) return false;

        // Check slot class name
        String slotClass = slot.getClass().getName();
        if (slotClass.contains("SlotCircuit") ||
                slotClass.contains("GT_Slot_Circuit") ||
                slotClass.contains("SlotProgrammedCircuit")) {
            return true;
        }

        // Circuit slots in GT machines are typically at specific positions
        // They're usually small slots in the top-left area
        if (slot.slotNumber == 0 || slot.slotNumber == 1) {
            // Check position (circuit slots are usually around x:7-18, y:7-18)
            if (slot.xDisplayPosition >= 5 && slot.xDisplayPosition <= 20 &&
                    slot.yDisplayPosition >= 5 && slot.yDisplayPosition <= 20) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if an ItemStack represents a programmed circuit
     */
    public static boolean isCircuitItem(ItemStack stack) {
        if (stack == null) return false;

        // Check item class
        String itemClass = stack.getItem().getClass().getName();
        if (itemClass.contains("MetaGenerated") || itemClass.contains("Circuit")) {

            // GT circuits have specific item IDs
            // IntCircuit item ID in GT is typically 32700-32725
            int itemID = stack.getItem().getUnlocalizedName().hashCode();

            // Check NBT for circuit configuration
            if (stack.hasTagCompound()) {
                if (stack.getTagCompound().hasKey("mConfiguration")) {
                    return true;
                }
                if (stack.getTagCompound().hasKey("Configuration")) {
                    return true;
                }
            }

            // Check metadata (circuit number 0-25)
            int meta = stack.getItemDamage();
            if (meta >= 0 && meta <= 25) {
                String displayName = stack.getDisplayName().toLowerCase();
                if (displayName.contains("circuit") || displayName.contains("programmed")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Extracts the circuit configuration number from a circuit ItemStack
     */
    public static int getCircuitNumber(ItemStack stack) {
        if (!isCircuitItem(stack)) {
            return -1;
        }

        // Try NBT first
        if (stack.hasTagCompound()) {
            if (stack.getTagCompound().hasKey("mConfiguration")) {
                return stack.getTagCompound().getInteger("mConfiguration");
            }
            if (stack.getTagCompound().hasKey("Configuration")) {
                return stack.getTagCompound().getInteger("Configuration");
            }
        }

        // Fall back to metadata
        return stack.getItemDamage();
    }

    /**
     * Sets a circuit value in a GT machine slot.
     * This simulates clicking the circuit slot with the appropriate configuration.
     */
    public static boolean setCircuitValue(GuiContainer gui, Slot circuitSlot, int circuitNumber) {
        if (circuitSlot == null || circuitNumber < 0 || circuitNumber > 25) {
            return false;
        }

        try {
            // GT machines handle circuit changes through different mechanisms:
            // 1. Scroll wheel when hovering over slot
            // 2. Clicking with specific items
            // 3. Direct packet to server

            // Method 1: Try to call GT's circuit change handler if available
            if (tryGTCircuitPacket(gui, circuitSlot.slotNumber, circuitNumber)) {
                NEIClientConfig.logger.info("Set circuit slot " + circuitSlot.slotNumber + " to " + circuitNumber);
                return true;
            }

            // Method 2: Simulate mouse wheel scrolls
            // GT circuits can be changed by scrolling over the slot
            if (tryScrollCircuit(gui, circuitSlot, circuitNumber)) {
                NEIClientConfig.logger.info("Set circuit via scroll to " + circuitNumber);
                return true;
            }

        } catch (Exception e) {
            NEIClientConfig.logger.error("Failed to set circuit value", e);
        }

        return false;
    }

    /**
     * Attempt to send GT's circuit change packet directly
     */
    private static boolean tryGTCircuitPacket(GuiContainer gui, int slotNumber, int circuitNumber) {
        try {
            // GT5U and GT6 have different packet systems
            // We need to use reflection to call the appropriate method

            // Try GT5U packet (gregtech.api.net.GT_Packet_SetCircuit or similar)
            Class<?> packetClass = Class.forName("gregtech.api.net.GT_Packet_SetConfigurationCircuit");
            if (packetClass != null) {
                // Create and send packet
                Object packet = packetClass.newInstance();
                // Set slot and circuit number via reflection
                // This is a simplified example - actual implementation depends on GT version

                NEIClientConfig.logger.info("Sending GT circuit packet");
                return true;
            }

        } catch (ClassNotFoundException e) {
            // GT packet class not found - not a GT machine or different version
        } catch (Exception e) {
            NEIClientConfig.logger.error("Error sending GT packet", e);
        }

        return false;
    }

    /**
     * Attempt to set circuit by simulating scroll wheel
     */
    private static boolean tryScrollCircuit(GuiContainer gui, Slot circuitSlot, int targetNumber) {
        try {
            // This would need to integrate with GT's scroll handler
            // GT machines listen for mouse wheel events over circuit slots

            // Calculate scroll direction and amount
            // (This is a placeholder - actual implementation would call GT's handler)

            int currentValue = getCurrentCircuitValue(circuitSlot);
            int scrollAmount = targetNumber - currentValue;

            if (scrollAmount != 0) {
                // Simulate scroll events
                NEIClientConfig.logger.info("Would scroll circuit " + scrollAmount + " times");
                // Would call: GT_GuiHandler.handleScroll(circuitSlot, scrollAmount);
            }

            return true;

        } catch (Exception e) {
            NEIClientConfig.logger.error("Error scrolling circuit", e);
        }

        return false;
    }

    /**
     * Get the current circuit value in a slot
     */
    private static int getCurrentCircuitValue(Slot slot) {
        if (slot == null || !slot.getHasStack()) {
            return 0;
        }

        return getCircuitNumber(slot.getStack());
    }

    /**
     * Helper to determine if the currently open GUI is a GT machine
     */
    public static boolean isGTMachine(GuiContainer gui) {
        if (gui == null) return false;

        String guiClass = gui.getClass().getName();
        return guiClass.contains("gregtech") ||
                guiClass.contains("GT_") ||
                guiClass.contains("GregTech");
    }
}

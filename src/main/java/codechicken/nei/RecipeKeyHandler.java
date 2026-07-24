package codechicken.nei;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.input.Keyboard;

/**
 * Handles recipe-related keybinds, particularly shift+c for recipe transfer.
 * This should be integrated into NEI's existing keybind system.
 */
public class RecipeKeyHandler {

    /**
     * Called when a key is pressed while viewing a recipe.
     * This should be hooked into NEI's GuiRecipe or similar class.
     *
     * @param keyCode The key that was pressed
     * @param recipe The currently viewed recipe handler
     * @param recipeIndex The index of the recipe being viewed
     * @return true if the key was handled
     */
    public static boolean handleRecipeKeyPress(int keyCode, Object recipe, int recipeIndex) {
        // Check for shift+c
        if (keyCode == Keyboard.KEY_C && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            return handleShiftC(recipe, recipeIndex);
        }

        return false;
    }

    /**
     * Handle shift+c press - transfer recipe to open container
     */
    private static boolean handleShiftC(Object recipe, int recipeIndex) {
        GuiContainer gui = NEIClientUtils.getGuiContainer();

        if (gui == null) {
            return false;
        }

        // Check what type of container is open
        String guiClass = gui.getClass().getName();

        // Previously, shift+c only worked with crafting tables
        // Now we support any container

        if (recipe instanceof codechicken.nei.api.IRecipeHandler) {
            codechicken.nei.api.IRecipeHandler handler = (codechicken.nei.api.IRecipeHandler) recipe;

            // Use our new universal transfer handler
            boolean maxQuantity = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ||
                    Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);

            return ContainerRecipeTransferHandler.transferRecipeToContainer(
                    handler,
                    recipeIndex,
                    maxQuantity
            );
        }

        return false;
    }

    /**
     * Helper to check if a GUI is a valid transfer target.
     * Can be used to filter out certain GUIs if needed.
     */
    public static boolean isValidTransferTarget(GuiContainer gui) {
        if (gui == null) {
            return false;
        }

        String guiClass = gui.getClass().getSimpleName();

        // Blacklist certain GUIs that shouldn't support recipe transfer
        if (guiClass.contains("GuiRecipe") ||
                guiClass.contains("GuiNEI") ||
                guiClass.contains("GuiInventory")) {
            return false;
        }

        // Allow all other containers
        return true;
    }
}

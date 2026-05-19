package org.wodichka.copyblockid;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class ClientCopyHandler {
    private static final ClientCopyHandler INSTANCE = new ClientCopyHandler();

    private ClientCopyHandler() {
    }

    public static void registerGameEvents() {
        NeoForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!isCopyShortcut(event.getKeyCode(), event.getModifiers())) {
            return;
        }

        if (copyHoveredItemId(Minecraft.getInstance(), event.getScreen())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS || !isCopyShortcut(event.getKey(), event.getModifiers())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            copyTargetedBlockId(minecraft);
        }
    }

    private static boolean isCopyShortcut(int keyCode, int modifiers) {
        Minecraft minecraft = Minecraft.getInstance();
        return keyCode == GLFW.GLFW_KEY_C && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 || isControlDown(minecraft));
    }

    private static boolean isControlDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static boolean copyHoveredItemId(Minecraft minecraft, Screen screen) {
        if (minecraft.player == null || !(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }

        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return false;
        }

        ItemStack itemStack = hoveredSlot.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        copyToClipboardAndNotify(minecraft, itemId.toString());
        return true;
    }

    private static void copyTargetedBlockId(Minecraft minecraft) {
        if (minecraft.screen != null || minecraft.level == null || minecraft.player == null) {
            return;
        }

        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        Block block = minecraft.level.getBlockState(pos).getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        copyToClipboardAndNotify(minecraft, blockId.toString());
    }

    private static void copyToClipboardAndNotify(Minecraft minecraft, String copiedId) {
        minecraft.keyboardHandler.setClipboard(copiedId);
        minecraft.player.displayClientMessage(Component.literal("Copied ID: " + copiedId), false);
    }
}

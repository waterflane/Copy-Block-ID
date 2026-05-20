package org.wodichka.copyblockid;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class ClientCopyHandler {
    private static final ClientCopyHandler INSTANCE = new ClientCopyHandler();
    private static final long SCREEN_NOTIFICATION_DURATION_MS = 1800L;
    private static final int NOTIFICATION_TEXT_COLOR = 0xFFE9C46A;
    private static final int NOTIFICATION_BORDER_COLOR = 0xCCF4A261;
    private static final int NOTIFICATION_BACKGROUND_COLOR = 0xD91F1A17;
    private ItemStack lastTooltipItemStack = ItemStack.EMPTY;
    private Screen lastTooltipScreen;
    private Component activeScreenNotification;
    private long activeScreenNotificationUntil;

    private ClientCopyHandler() {
    }

    public static void registerGameEvents() {
        NeoForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!isCopyShortcut(event.getKeyCode(), event.getModifiers()) || isTextInputFocused(event.getScreen())) {
            return;
        }

        if (copyHoveredScreenValue(Minecraft.getInstance(), event.getScreen())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Pre event) {
        lastTooltipScreen = event.getScreen();
        lastTooltipItemStack = ItemStack.EMPTY;
    }

    @SubscribeEvent
    public void onScreenRenderPost(ScreenEvent.Render.Post event) {
        renderScreenNotification(Minecraft.getInstance(), event.getScreen(), event.getGuiGraphics());
    }

    @SubscribeEvent
    public void onScreenClosing(ScreenEvent.Closing event) {
        clearLastTooltipItem();
    }

    @SubscribeEvent
    public void onTooltipRender(RenderTooltipEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null || event.getItemStack().isEmpty()) {
            return;
        }

        lastTooltipScreen = minecraft.screen;
        lastTooltipItemStack = event.getItemStack().copy();
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

    private static boolean isTextInputFocused(Screen screen) {
        return screen.getFocused() instanceof EditBox;
    }

    private static boolean isControlDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private boolean copyHoveredScreenValue(Minecraft minecraft, Screen screen) {
        return copyHoveredItemId(minecraft, screen) || copyHoveredTooltipItemName(minecraft, screen);
    }

    private static boolean copyHoveredItemId(Minecraft minecraft, Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }

        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return false;
        }

        ItemStack itemStack = hoveredSlot.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        INSTANCE.copyToClipboardAndNotify(
                minecraft,
                itemId.toString(),
                Component.translatable("message.copyblockid.item_id_copied", itemId.toString())
        );
        return true;
    }

    private boolean copyHoveredTooltipItemName(Minecraft minecraft, Screen screen) {
        if (screen != lastTooltipScreen || lastTooltipItemStack.isEmpty()) {
            return false;
        }

        Component itemName = lastTooltipItemStack.getHoverName();
        String copiedName = itemName.getString();
        if (copiedName.isBlank()) {
            return false;
        }

        INSTANCE.copyToClipboardAndNotify(
                minecraft,
                copiedName,
                Component.translatable("message.copyblockid.item_name_copied", itemName)
        );
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
        INSTANCE.copyToClipboardAndNotify(
                minecraft,
                blockId.toString(),
                Component.translatable("message.copyblockid.block_id_copied", blockId.toString())
        );
    }

    private void clearLastTooltipItem() {
        lastTooltipScreen = null;
        lastTooltipItemStack = ItemStack.EMPTY;
    }

    private void showNotification(Minecraft minecraft, Component notification) {
        if (minecraft.screen == null) {
            minecraft.gui.setOverlayMessage(notification, false);
            return;
        }

        activeScreenNotification = notification;
        activeScreenNotificationUntil = System.currentTimeMillis() + SCREEN_NOTIFICATION_DURATION_MS;
    }

    private void renderScreenNotification(Minecraft minecraft, Screen screen, GuiGraphics guiGraphics) {
        if (activeScreenNotification == null || System.currentTimeMillis() > activeScreenNotificationUntil) {
            activeScreenNotification = null;
            return;
        }

        int textWidth = minecraft.font.width(activeScreenNotification);
        int centerX = getNotificationCenterX(screen);
        int x = centerX - textWidth / 2;
        int y = getNotificationY(screen);

        guiGraphics.fill(x - 5, y - 4, x + textWidth + 5, y + 12, NOTIFICATION_BORDER_COLOR);
        guiGraphics.fill(x - 4, y - 3, x + textWidth + 4, y + 11, NOTIFICATION_BACKGROUND_COLOR);
        guiGraphics.drawString(minecraft.font, activeScreenNotification, x, y, NOTIFICATION_TEXT_COLOR, false);
    }

    private static int getNotificationCenterX(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            int leftPos = getScreenField(containerScreen, "leftPos", (screen.width - 176) / 2);
            int imageWidth = getScreenField(containerScreen, "imageWidth", 176);
            return leftPos + imageWidth / 2;
        }

        return screen.width / 2;
    }

    private static int getNotificationY(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            int topPos = getScreenField(containerScreen, "topPos", (screen.height - 166) / 2);
            if (screen instanceof CreativeModeInventoryScreen || screen instanceof InventoryScreen) {
                return Math.max(8, topPos - 32);
            }
            return Math.max(8, topPos - 18);
        }

        return 12;
    }

    private static int getScreenField(AbstractContainerScreen<?> screen, String fieldName, int fallback) {
        try {
            Field field = AbstractContainerScreen.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getInt(screen);
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    private void copyToClipboardAndNotify(Minecraft minecraft, String copiedValue, Component notification) {
        minecraft.keyboardHandler.setClipboard(copiedValue);
        showNotification(minecraft, notification);
    }
}

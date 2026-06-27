package org.wodichka.copyblockid;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public final class CopyBlockIdClient {
    private static final long SCREEN_NOTIFICATION_DURATION_MS = 1800L;
    private static final int NOTIFICATION_TEXT_COLOR = 0xFFE9C46A;
    private static final int NOTIFICATION_BORDER_COLOR = 0xCCF4A261;
    private static final int NOTIFICATION_BACKGROUND_COLOR = 0xD91F1A17;
    private static final String ID_COPIED_MESSAGE_KEY = "message.copyblockid.id_copied";

    private static ItemStack lastTooltipItemStack = ItemStack.EMPTY;
    private static List<String> lastTooltipLines = List.of();
    private static Screen lastTooltipScreen;
    private static Component activeScreenNotification;
    private static long activeScreenNotificationUntil;

    private CopyBlockIdClient() {
    }

    public static boolean handleScreenKeyPressed(Screen screen, int keyCode, int modifiers) {
        if (!isCopyShortcut(keyCode, modifiers) || isTextInputFocused(screen)) {
            return false;
        }

        return copyHoveredScreenValue(Minecraft.getInstance(), screen);
    }

    public static void handleWorldKeyPressed(int keyCode, int action, int modifiers) {
        if (action != GLFW.GLFW_PRESS || !isCopyShortcut(keyCode, modifiers)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            copyTargetedWorldValue(minecraft);
        }
    }

    public static void beforeScreenRender(Screen screen) {
        lastTooltipScreen = screen;
        lastTooltipItemStack = ItemStack.EMPTY;
        lastTooltipLines = List.of();
    }

    public static void afterScreenRender(Screen screen, GuiGraphics guiGraphics) {
        renderScreenNotification(Minecraft.getInstance(), screen, guiGraphics);
    }

    public static void onScreenClosing() {
        clearLastTooltipItem();
    }

    public static void captureTooltipItem(ItemStack itemStack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null || itemStack.isEmpty()) {
            return;
        }

        lastTooltipScreen = minecraft.screen;
        lastTooltipItemStack = itemStack.copy();
    }

    public static void captureTooltipComponents(ItemStack itemStack, List<?> tooltipElements) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            return;
        }

        List<String> tooltipLines = new ArrayList<>();
        for (Object tooltipElement : tooltipElements) {
            if (!(tooltipElement instanceof Either<?, ?> either)) {
                continue;
            }

            either.left()
                    .filter(FormattedText.class::isInstance)
                    .map(FormattedText.class::cast)
                    .map(FormattedText::getString)
                    .filter(line -> !line.isBlank())
                    .ifPresent(tooltipLines::add);
        }

        lastTooltipScreen = minecraft.screen;
        lastTooltipItemStack = itemStack.copy();
        lastTooltipLines = List.copyOf(tooltipLines);
    }

    public static void captureTooltipLines(ItemStack itemStack, List<? extends Component> tooltipLines) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        for (Component tooltipLine : tooltipLines) {
            String line = tooltipLine.getString();
            if (!line.isBlank()) {
                lines.add(line);
            }
        }

        lastTooltipScreen = minecraft.screen;
        if (!itemStack.isEmpty()) {
            lastTooltipItemStack = itemStack.copy();
        }
        lastTooltipLines = List.copyOf(lines);
    }

    private static boolean isCopyShortcut(int keyCode, int modifiers) {
        Minecraft minecraft = Minecraft.getInstance();
        return keyCode == GLFW.GLFW_KEY_C && ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 || isControlDown(minecraft));
    }

    private static boolean isTextInputFocused(Screen screen) {
        return screen != null && screen.getFocused() instanceof EditBox;
    }

    private static boolean isControlDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static boolean copyHoveredScreenValue(Minecraft minecraft, Screen screen) {
        return copyHoveredSlotItemId(minecraft, screen)
                || copyHoveredRecipeViewerIngredientId(minecraft)
                || copyHoveredTooltipItemId(minecraft, screen)
                || copyHoveredTooltipFluidId(minecraft, screen);
    }

    private static boolean copyHoveredSlotItemId(Minecraft minecraft, Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }

        Slot hoveredSlot = getHoveredSlot(containerScreen);
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return false;
        }

        ItemStack itemStack = hoveredSlot.getItem();
        copyIdToClipboardAndNotify(minecraft, BuiltInRegistries.ITEM.getKey(itemStack.getItem()));
        return true;
    }

    private static Slot getHoveredSlot(AbstractContainerScreen<?> screen) {
        if (screen instanceof HoveredSlotProvider hoveredSlotProvider) {
            return hoveredSlotProvider.copyblockid$getHoveredSlot();
        }

        return null;
    }

    private static boolean copyHoveredRecipeViewerIngredientId(Minecraft minecraft) {
        Optional<ResourceLocation> ingredientId = findHoveredReiIngredientId()
                .or(CopyBlockIdClient::findHoveredJeiIngredientId);
        if (ingredientId.isEmpty()) {
            return false;
        }

        copyIdToClipboardAndNotify(minecraft, ingredientId.get());
        return true;
    }

    private static boolean copyHoveredTooltipItemId(Minecraft minecraft, Screen screen) {
        if (screen != lastTooltipScreen || lastTooltipItemStack.isEmpty()) {
            return false;
        }

        copyIdToClipboardAndNotify(minecraft, BuiltInRegistries.ITEM.getKey(lastTooltipItemStack.getItem()));
        return true;
    }

    private static boolean copyHoveredTooltipFluidId(Minecraft minecraft, Screen screen) {
        if (screen != lastTooltipScreen || lastTooltipLines.isEmpty()) {
            return false;
        }

        Optional<ResourceLocation> fluidId = findFluidIdInTooltipLines(lastTooltipLines)
                .or(() -> findFluidIdByTooltipTitle(lastTooltipLines.get(0)));
        if (fluidId.isEmpty()) {
            return false;
        }

        copyIdToClipboardAndNotify(minecraft, fluidId.get());
        return true;
    }

    private static boolean copyTargetedWorldValue(Minecraft minecraft) {
        return CopyBlockIdConfig.canCopyTargetedEntityInWorld() && copyTargetedEntityId(minecraft)
                || CopyBlockIdConfig.canCopyTargetedBlockInWorld() && copyTargetedBlockId(minecraft);
    }

    private static boolean copyTargetedEntityId(Minecraft minecraft) {
        if (minecraft.screen != null || minecraft.level == null || minecraft.player == null) {
            return false;
        }

        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof EntityHitResult entityHitResult) || hitResult.getType() != HitResult.Type.ENTITY) {
            return false;
        }

        Entity entity = entityHitResult.getEntity();
        copyIdToClipboardAndNotify(minecraft, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        return true;
    }

    private static boolean copyTargetedBlockId(Minecraft minecraft) {
        if (minecraft.screen != null || minecraft.level == null || minecraft.player == null) {
            return false;
        }

        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        Block block = minecraft.level.getBlockState(pos).getBlock();
        copyIdToClipboardAndNotify(minecraft, BuiltInRegistries.BLOCK.getKey(block));
        return true;
    }

    private static void clearLastTooltipItem() {
        lastTooltipScreen = null;
        lastTooltipItemStack = ItemStack.EMPTY;
        lastTooltipLines = List.of();
    }

    private static void showNotification(Minecraft minecraft, Component notification) {
        if (minecraft.screen == null) {
            minecraft.gui.setOverlayMessage(notification, false);
            return;
        }

        activeScreenNotification = notification;
        activeScreenNotificationUntil = System.currentTimeMillis() + SCREEN_NOTIFICATION_DURATION_MS;
    }

    private static void renderScreenNotification(Minecraft minecraft, Screen screen, GuiGraphics guiGraphics) {
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
        return screen.width / 2;
    }

    private static int getNotificationY(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> ? Math.max(8, screen.height - 28) : 12;
    }

    private static Optional<ResourceLocation> findHoveredReiIngredientId() {
        try {
            Class<?> runtimeClass = Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            Object runtime = runtimeClass.getMethod("getInstance").invoke(null);
            Optional<?> overlay = invokeOptional(runtime, "getOverlay");
            if (overlay.isEmpty()) {
                return Optional.empty();
            }

            Object screenOverlay = overlay.get();
            Optional<ResourceLocation> entryListId = findReiOverlayListIngredientId(invoke(screenOverlay, "getEntryList"));
            if (entryListId.isPresent()) {
                return entryListId;
            }

            return invokeOptional(screenOverlay, "getFavoritesList")
                    .flatMap(CopyBlockIdClient::findReiOverlayListIngredientId);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static Optional<ResourceLocation> findReiOverlayListIngredientId(Object overlayList) {
        try {
            Object entryStack = invoke(overlayList, "getFocusedStack");
            if (entryStack == null || isReflectedEmptyEntry(entryStack)) {
                return Optional.empty();
            }

            return invokeResourceLocation(entryStack, "getIdentifier");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static Optional<ResourceLocation> findHoveredJeiIngredientId() {
        try {
            Optional<?> runtime = findJeiRuntime();
            if (runtime.isEmpty()) {
                return Optional.empty();
            }

            Object jeiRuntime = runtime.get();
            Optional<ResourceLocation> overlayId = findJeiHoveredIngredientId(invoke(jeiRuntime, "getIngredientListOverlay"));
            if (overlayId.isPresent()) {
                return overlayId;
            }

            Optional<ResourceLocation> bookmarkId = findJeiHoveredIngredientId(invoke(jeiRuntime, "getBookmarkOverlay"));
            if (bookmarkId.isPresent()) {
                return bookmarkId;
            }

            Object recipesGui = invoke(jeiRuntime, "getRecipesGui");
            Object ingredientManager = invoke(jeiRuntime, "getIngredientManager");
            for (Object ingredientType : getJeiIngredientTypes(ingredientManager)) {
                Optional<?> ingredient = invokeOptional(recipesGui, "getIngredientUnderMouse", ingredientType);
                Optional<ResourceLocation> ingredientId = ingredient.flatMap(CopyBlockIdClient::getIdFromIngredientObject);
                if (ingredientId.isPresent()) {
                    return ingredientId;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    private static Optional<Object> findJeiRuntime() {
        List<String> runtimeProviderClasses = List.of(
                "mezz.jei.common.Internal",
                "mezz.jei.core.util.JeiRuntime"
        );
        List<String> runtimeProviderMethods = List.of(
                "getJeiRuntime",
                "getRuntime",
                "getInstance"
        );

        for (String className : runtimeProviderClasses) {
            try {
                Class<?> providerClass = Class.forName(className);
                for (String methodName : runtimeProviderMethods) {
                    try {
                        Object runtime = providerClass.getMethod(methodName).invoke(null);
                        if (runtime instanceof Optional<?> optionalRuntime) {
                            if (optionalRuntime.isPresent()) {
                                return Optional.of(optionalRuntime.get());
                            }
                        } else if (runtime != null) {
                            return Optional.of(runtime);
                        }
                    } catch (NoSuchMethodException ignored) {
                        // Try the next known JEI runtime accessor.
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // JEI is optional; absence or API drift should not affect vanilla copying.
            }
        }

        return Optional.empty();
    }

    private static Optional<ResourceLocation> findJeiHoveredIngredientId(Object ingredientSource) {
        try {
            Optional<?> typedIngredient = invokeOptional(ingredientSource, "getIngredientUnderMouse");
            return typedIngredient
                    .map(CopyBlockIdClient::unwrapJeiTypedIngredient)
                    .flatMap(CopyBlockIdClient::getIdFromIngredientObject);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static List<Object> getJeiIngredientTypes(Object ingredientManager) {
        try {
            Object ingredientTypes = invoke(ingredientManager, "getRegisteredIngredientTypes");
            if (ingredientTypes instanceof Iterable<?> iterable) {
                List<Object> result = new ArrayList<>();
                for (Object ingredientType : iterable) {
                    result.add(ingredientType);
                }
                return result;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return List.of();
        }

        return List.of();
    }

    private static Object unwrapJeiTypedIngredient(Object typedIngredient) {
        try {
            return invoke(typedIngredient, "getIngredient");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return typedIngredient;
        }
    }

    private static Optional<ResourceLocation> getIdFromIngredientObject(Object ingredient) {
        if (ingredient instanceof ItemStack itemStack && !itemStack.isEmpty()) {
            return Optional.of(BuiltInRegistries.ITEM.getKey(itemStack.getItem()));
        }

        if (ingredient instanceof Fluid fluid) {
            return Optional.of(BuiltInRegistries.FLUID.getKey(fluid));
        }

        Optional<ResourceLocation> reflectedFluidStackId = getFluidFromReflectedStack(ingredient)
                .map(BuiltInRegistries.FLUID::getKey);
        if (reflectedFluidStackId.isPresent()) {
            return reflectedFluidStackId;
        }

        return invokeResourceLocation(ingredient, "getIdentifier")
                .or(() -> invokeResourceLocation(ingredient, "getRegistryName"))
                .or(() -> invokeResourceLocation(ingredient, "getId"));
    }

    private static Optional<Fluid> getFluidFromReflectedStack(Object ingredient) {
        try {
            if (isReflectedEmptyEntry(ingredient)) {
                return Optional.empty();
            }

            Object fluid = invoke(ingredient, "getFluid");
            return fluid instanceof Fluid reflectedFluid ? Optional.of(reflectedFluid) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static Optional<ResourceLocation> findFluidIdInTooltipLines(List<String> tooltipLines) {
        for (String tooltipLine : tooltipLines) {
            ResourceLocation id = ResourceLocation.tryParse(tooltipLine.trim());
            if (id != null && BuiltInRegistries.FLUID.containsKey(id)) {
                return Optional.of(id);
            }
        }

        return Optional.empty();
    }

    private static Optional<ResourceLocation> findFluidIdByTooltipTitle(String tooltipTitle) {
        return findFluidIdByTooltipTitle(tooltipTitle, true)
                .or(() -> findFluidIdByTooltipTitle(tooltipTitle, false));
    }

    private static Optional<ResourceLocation> findFluidIdByTooltipTitle(String tooltipTitle, boolean sourceFluidsOnly) {
        String normalizedTitle = normalizeTooltipText(tooltipTitle);
        if (normalizedTitle.isBlank()) {
            return Optional.empty();
        }

        for (Fluid fluid : BuiltInRegistries.FLUID) {
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
            if (fluidId == null || "minecraft:empty".equals(fluidId.toString()) || sourceFluidsOnly != isSourceFluid(fluid)) {
                continue;
            }

            Optional<Component> hoverName = FluidNameProvider.getHoverName(fluid);
            if (hoverName.isPresent() && normalizedTitle.equals(normalizeTooltipText(hoverName.get().getString()))) {
                return Optional.of(fluidId);
            }
        }

        return Optional.empty();
    }

    private static boolean isSourceFluid(Fluid fluid) {
        return !(fluid instanceof FlowingFluid flowingFluid) || flowingFluid.getSource() == fluid;
    }

    private static String normalizeTooltipText(String text) {
        return text.strip();
    }

    private static Optional<?> invokeOptional(Object target, String methodName, Object... arguments)
            throws ReflectiveOperationException {
        Object result = invoke(target, methodName, arguments);
        if (result instanceof Optional<?> optional) {
            return optional;
        }

        return Optional.ofNullable(result);
    }

    private static Optional<ResourceLocation> invokeResourceLocation(Object target, String methodName) {
        try {
            Object result = invoke(target, methodName);
            if (result instanceof ResourceLocation id) {
                return Optional.of(id);
            }

            if (result instanceof String idText) {
                return Optional.ofNullable(ResourceLocation.tryParse(idText));
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    private static boolean isReflectedEmptyEntry(Object target) {
        try {
            Object result = invoke(target, "isEmpty");
            return result instanceof Boolean empty && empty;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static Object invoke(Object target, String methodName, Object... arguments) throws ReflectiveOperationException {
        Class<?>[] argumentTypes = new Class<?>[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argumentTypes[i] = arguments[i].getClass();
        }

        Method method = findPublicMethod(target.getClass(), methodName, argumentTypes);
        return method.invoke(target, arguments);
    }

    private static Method findPublicMethod(Class<?> type, String methodName, Class<?>[] argumentTypes) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != argumentTypes.length) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!parameterTypes[i].isAssignableFrom(argumentTypes[i])) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                return method;
            }
        }

        throw new NoSuchMethodException(type.getName() + "." + methodName);
    }

    private static void copyIdToClipboardAndNotify(Minecraft minecraft, ResourceLocation id) {
        copyToClipboardAndNotify(
                minecraft,
                id.toString(),
                Component.translatable(ID_COPIED_MESSAGE_KEY, id.toString())
        );
    }

    private static void copyToClipboardAndNotify(Minecraft minecraft, String copiedValue, Component notification) {
        minecraft.keyboardHandler.setClipboard(copiedValue);
        showNotification(minecraft, notification);
    }
}

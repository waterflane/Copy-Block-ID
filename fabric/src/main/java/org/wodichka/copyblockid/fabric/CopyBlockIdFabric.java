package org.wodichka.copyblockid.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import org.lwjgl.glfw.GLFW;
import org.wodichka.copyblockid.CopyBlockId;
import org.wodichka.copyblockid.CopyBlockIdClient;
import org.wodichka.copyblockid.CopyBlockIdConfig;
import org.wodichka.copyblockid.FluidNameProvider;

public final class CopyBlockIdFabric implements ClientModInitializer {
    private static boolean copyKeyWasDown;
    private boolean copyTargetedBlockInWorld;
    private boolean copyTargetedEntityInWorld;

    @Override
    public void onInitializeClient() {
        loadConfig();
        CopyBlockIdConfig.setPlatform(new CopyBlockIdConfig.Platform() {
            @Override
            public boolean canCopyTargetedBlockInWorld() {
                return copyTargetedBlockInWorld;
            }

            @Override
            public boolean canCopyTargetedEntityInWorld() {
                return copyTargetedEntityInWorld;
            }
        });
        FluidNameProvider.setProvider(CopyBlockIdFabric::getFluidHoverName);

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenKeyboardEvents.allowKeyPress(screen).register((screen1, key, scancode, modifiers) ->
                    !CopyBlockIdClient.handleScreenKeyPressed(screen1, key, modifiers));
            ScreenEvents.beforeRender(screen).register((screen1, guiGraphics, mouseX, mouseY, tickDelta) ->
                    CopyBlockIdClient.beforeScreenRender(screen1));
            ScreenEvents.afterRender(screen).register((screen1, guiGraphics, mouseX, mouseY, tickDelta) ->
                    CopyBlockIdClient.afterScreenRender(screen1, guiGraphics));
            ScreenEvents.remove(screen).register(screen1 -> CopyBlockIdClient.onScreenClosing());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.screen != null || client.getWindow() == null) {
                copyKeyWasDown = false;
                return;
            }

            long window = client.getWindow().getWindow();
            boolean copyKeyDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_C);
            if (copyKeyDown && !copyKeyWasDown) {
                CopyBlockIdClient.handleWorldKeyPressed(GLFW.GLFW_KEY_C, GLFW.GLFW_PRESS, 0);
            }
            copyKeyWasDown = copyKeyDown;
        });
    }

    private void loadConfig() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CopyBlockId.MOD_ID + "-client.properties");
        Properties properties = new Properties();

        if (Files.isRegularFile(configPath)) {
            try (InputStream inputStream = Files.newInputStream(configPath)) {
                properties.load(inputStream);
            } catch (IOException ignored) {
                // Keep defaults if the config cannot be read.
            }
        }

        copyTargetedBlockInWorld = Boolean.parseBoolean(properties.getProperty("copyTargetedBlockInWorld", "false"));
        copyTargetedEntityInWorld = Boolean.parseBoolean(properties.getProperty("copyTargetedEntityInWorld", "false"));

        properties.setProperty("copyTargetedBlockInWorld", Boolean.toString(copyTargetedBlockInWorld));
        properties.setProperty("copyTargetedEntityInWorld", Boolean.toString(copyTargetedEntityInWorld));

        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream outputStream = Files.newOutputStream(configPath)) {
                properties.store(outputStream, "CopyBlockId client config");
            }
        } catch (IOException ignored) {
            // The mod still works with defaults when config saving fails.
        }
    }

    private static Optional<Component> getFluidHoverName(Fluid fluid) {
        if (fluid.getBucket().getDescriptionId().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(fluid.getBucket().getDescription());
    }
}

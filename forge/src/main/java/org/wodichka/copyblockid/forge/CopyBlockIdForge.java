package org.wodichka.copyblockid.forge;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.wodichka.copyblockid.CopyBlockId;
import org.wodichka.copyblockid.CopyBlockIdClient;
import org.wodichka.copyblockid.CopyBlockIdConfig;
import org.wodichka.copyblockid.FluidNameProvider;

@Mod(CopyBlockId.MOD_ID)
public final class CopyBlockIdForge {
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue COPY_TARGETED_BLOCK_IN_WORLD;
    private static final ForgeConfigSpec.BooleanValue COPY_TARGETED_ENTITY_IN_WORLD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        COPY_TARGETED_BLOCK_IN_WORLD = builder
                .comment("When true, Ctrl+C in the world copies the registry ID of the block you are looking at.")
                .define("copyTargetedBlockInWorld", false);

        COPY_TARGETED_ENTITY_IN_WORLD = builder
                .comment("When true, Ctrl+C in the world copies the registry ID of the entity you are looking at.")
                .define("copyTargetedEntityInWorld", false);

        SPEC = builder.build();
    }

    public CopyBlockIdForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
        CopyBlockIdConfig.setPlatform(new CopyBlockIdConfig.Platform() {
            @Override
            public boolean canCopyTargetedBlockInWorld() {
                return SPEC.isLoaded() && COPY_TARGETED_BLOCK_IN_WORLD.get();
            }

            @Override
            public boolean canCopyTargetedEntityInWorld() {
                return SPEC.isLoaded() && COPY_TARGETED_ENTITY_IN_WORLD.get();
            }
        });
        FluidNameProvider.setProvider(CopyBlockIdForge::getFluidHoverName);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (CopyBlockIdClient.handleScreenKeyPressed(event.getScreen(), event.getKeyCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Pre event) {
        CopyBlockIdClient.beforeScreenRender(event.getScreen());
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onScreenRenderPost(ScreenEvent.Render.Post event) {
        CopyBlockIdClient.afterScreenRender(event.getScreen(), event.getGuiGraphics());
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onScreenClosing(ScreenEvent.Closing event) {
        CopyBlockIdClient.onScreenClosing();
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onTooltipRender(RenderTooltipEvent.Pre event) {
        CopyBlockIdClient.captureTooltipItem(event.getItemStack());
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onTooltipComponentsGather(RenderTooltipEvent.GatherComponents event) {
        CopyBlockIdClient.captureTooltipComponents(event.getItemStack(), event.getTooltipElements());
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        CopyBlockIdClient.handleWorldKeyPressed(event.getKey(), event.getAction(), event.getModifiers());
    }

    private static Optional<Component> getFluidHoverName(Fluid fluid) {
        FluidStack fluidStack = new FluidStack(fluid, 1000);
        if (fluidStack.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(fluidStack.getDisplayName());
    }
}

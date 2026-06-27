package org.wodichka.copyblockid.fabric.mixin;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wodichka.copyblockid.CopyBlockIdClient;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsTooltipMixin {
    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
    private void copyblockid$captureItemTooltip(Font font, ItemStack itemStack, int mouseX, int mouseY, CallbackInfo callbackInfo) {
        CopyBlockIdClient.captureTooltipItem(itemStack);
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"))
    private void copyblockid$captureTooltipLines(
            Font font,
            List<Component> tooltipLines,
            Optional<TooltipComponent> visualTooltipComponent,
            int mouseX,
            int mouseY,
            CallbackInfo callbackInfo
    ) {
        CopyBlockIdClient.captureTooltipLines(ItemStack.EMPTY, tooltipLines);
    }
}

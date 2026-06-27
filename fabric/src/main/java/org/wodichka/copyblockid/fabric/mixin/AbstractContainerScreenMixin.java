package org.wodichka.copyblockid.fabric.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.wodichka.copyblockid.HoveredSlotProvider;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements HoveredSlotProvider {
    @Shadow
    protected Slot hoveredSlot;

    @Override
    public Slot copyblockid$getHoveredSlot() {
        return hoveredSlot;
    }
}

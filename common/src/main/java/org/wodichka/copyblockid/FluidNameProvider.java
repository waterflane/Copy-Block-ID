package org.wodichka.copyblockid;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;

public final class FluidNameProvider {
    private static Provider provider = Provider.DEFAULT;

    private FluidNameProvider() {
    }

    public static void setProvider(Provider provider) {
        FluidNameProvider.provider = provider;
    }

    public static Optional<Component> getHoverName(Fluid fluid) {
        return provider.getHoverName(fluid);
    }

    public interface Provider {
        Provider DEFAULT = fluid -> Optional.empty();

        Optional<Component> getHoverName(Fluid fluid);
    }
}

package org.wodichka.copyblockid;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<Boolean> COPY_TARGETED_BLOCK_IN_WORLD;
    private static final ModConfigSpec.ConfigValue<Boolean> COPY_TARGETED_ENTITY_IN_WORLD;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        COPY_TARGETED_BLOCK_IN_WORLD = builder
                .comment("When true, Ctrl+C in the world copies the registry ID of the block you are looking at.")
                .define("copyTargetedBlockInWorld", false);

        COPY_TARGETED_ENTITY_IN_WORLD = builder
                .comment("When true, Ctrl+C in the world copies the registry ID of the entity you are looking at.")
                .define("copyTargetedEntityInWorld", false);

        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    public static boolean canCopyTargetedBlockInWorld() {
        return SPEC.isLoaded() && COPY_TARGETED_BLOCK_IN_WORLD.get();
    }

    public static boolean canCopyTargetedEntityInWorld() {
        return SPEC.isLoaded() && COPY_TARGETED_ENTITY_IN_WORLD.get();
    }
}

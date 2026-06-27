package org.wodichka.copyblockid;

public final class CopyBlockIdConfig {
    private static Platform platform = Platform.DEFAULT;

    private CopyBlockIdConfig() {
    }

    public static void setPlatform(Platform platform) {
        CopyBlockIdConfig.platform = platform;
    }

    public static boolean canCopyTargetedBlockInWorld() {
        return platform.canCopyTargetedBlockInWorld();
    }

    public static boolean canCopyTargetedEntityInWorld() {
        return platform.canCopyTargetedEntityInWorld();
    }

    public interface Platform {
        Platform DEFAULT = new Platform() {
            @Override
            public boolean canCopyTargetedBlockInWorld() {
                return false;
            }

            @Override
            public boolean canCopyTargetedEntityInWorld() {
                return false;
            }
        };

        boolean canCopyTargetedBlockInWorld();

        boolean canCopyTargetedEntityInWorld();
    }
}

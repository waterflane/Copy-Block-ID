package org.wodichka.copyblockid;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Copyblockid.MODID)
public final class Copyblockid {
    public static final String MODID = "copyblockid";

    public Copyblockid() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientCopyHandler.registerGameEvents();
        }
    }
}

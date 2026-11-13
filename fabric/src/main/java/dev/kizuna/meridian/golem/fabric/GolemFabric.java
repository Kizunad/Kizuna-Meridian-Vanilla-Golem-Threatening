package dev.kizuna.meridian.golem.fabric;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

/** 傀儡仓 26.2 Fabric 入口：傀儡工作台、傀儡师职业与工作站点、创造栏。住处结构是随包数据，不用注册。 */
public final class GolemFabric implements ModInitializer {
    public static final String MOD_ID = "meridian_golem";

    @Override
    public void onInitialize() {
        // 方块先于工作站点和职业注册：工作站点要列出傀儡工作台的全部方块状态。
        GolemBlocks.register();
        GolemancerProfession.register();
        GolemCreativeTab.register();
    }

    /** 本仓命名空间下的身份。 */
    public static Identifier id(String path) {
        // 方块、职业、结构和创造栏都在 meridian_golem 命名空间下。
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

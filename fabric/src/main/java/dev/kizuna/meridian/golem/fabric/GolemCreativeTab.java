package dev.kizuna.meridian.golem.fabric;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** 傀儡独占一页创造栏，现在只有傀儡工作台。 */
public final class GolemCreativeTab {
    public static final Identifier ID = GolemFabric.id("golem");

    private GolemCreativeTab() {
        // 创造栏只在模组初始化时注册一次。
    }

    static void register() {
        // 图标和条目都是傀儡工作台；以后的傀儡物品接着往这一页加。
        CreativeModeTab tab = FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.meridian_golem.golem"))
                .icon(() -> new ItemStack(GolemBlocks.golemBench()))
                .displayItems((context, output) -> output.accept(GolemBlocks.golemBench()))
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ID, tab);
    }
}

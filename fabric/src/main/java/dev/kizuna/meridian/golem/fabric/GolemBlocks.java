package dev.kizuna.meridian.golem.fabric;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/** 傀儡工作台方块与方块物品：材质属性照原版锻造台，木身铁面，斧子挖得快，徒手也能挖下来。 */
public final class GolemBlocks {
    public static final Identifier GOLEM_BENCH_ID = GolemFabric.id("golem_bench");
    // 硬度与原版锻造台相同。
    private static final float GOLEM_BENCH_STRENGTH = 2.5F;
    private static Block golemBench;

    private GolemBlocks() {
        // 方块和方块物品只在模组初始化时注册一次。
    }

    static void register() {
        // 方块物品取方块的翻译键，语言文件只写 block.meridian_golem.golem_bench 一份名字。
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, GOLEM_BENCH_ID);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .instrument(NoteBlockInstrument.BASS)
                .strength(GOLEM_BENCH_STRENGTH)
                .sound(SoundType.WOOD)
                .ignitedByLava()
                .setId(blockKey);
        golemBench = Registry.register(BuiltInRegistries.BLOCK, GOLEM_BENCH_ID, new GolemBenchBlock(properties));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, GOLEM_BENCH_ID);
        Item.Properties itemProperties = new Item.Properties()
                .setId(itemKey)
                .useBlockDescriptionPrefix();
        Registry.register(BuiltInRegistries.ITEM, GOLEM_BENCH_ID, new GolemBenchItem(golemBench, itemProperties));
    }

    /** 已注册的傀儡工作台方块。 */
    public static Block golemBench() {
        // 注册前取用说明初始化顺序错了，直接失败。
        if (golemBench == null) {
            throw new IllegalStateException("傀儡工作台还没注册");
        }
        return golemBench;
    }
}

package dev.kizuna.meridian.golem.fabric;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 傀儡工作台：放置朝向照铁砧、结构转向与镜像、斧挖掉自身、配方与配方书解锁、无业村民认领后成傀儡师。 */
public final class GolemBenchGameTest {
    private static final String FLOOR = "meridian_golem_test:bench_floor";
    /** 未单独声明时限的用例按 100 tick；认领要等村民的找工作扫描和走路，留一分钟。 */
    private static final int DEFAULT_TIMEOUT = 100;
    private static final int CLAIM_TIMEOUT = 1200;
    private static final ResourceKey<Recipe<?>> RECIPE =
            ResourceKey.create(Registries.RECIPE, GolemBlocks.GOLEM_BENCH_ID);
    private static final List<Direction> LOOKS =
            List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final BlockPos BENCH = new BlockPos(2, 1, 2);
    private static final BlockPos VILLAGER = new BlockPos(5, 1, 5);
    private static final int BENCH_ROW_Z = 1;
    private static final int ANVIL_ROW_Z = 3;

    @GameTest(structure = FLOOR, maxTicks = DEFAULT_TIMEOUT)
    public void placementFacesThePlacerLikeAnAnvil(GameTestHelper context) {
        // 四个视线方向各放一张工作台和一块铁砧：facing 两者相同，所以互换时照抄；工作台正面朝放置的人。
        Player player = context.makeMockPlayer(GameType.CREATIVE);
        for (int column = 0; column < LOOKS.size(); column++) {
            Direction look = LOOKS.get(column);
            player.setYRot(look.toYRot());
            BlockState bench = place(context, player, GolemBlocks.golemBench().asItem(),
                    new BlockPos(1 + column, 1, BENCH_ROW_Z));
            BlockState anvil = place(context, player, Items.ANVIL, new BlockPos(1 + column, 1, ANVIL_ROW_Z));
            context.assertTrue(bench.is(GolemBlocks.golemBench()), "视线朝 " + look + " 没放下工作台：" + bench);
            context.assertTrue(anvil.getValue(AnvilBlock.FACING) == bench.getValue(GolemBenchBlock.FACING),
                    "视线朝 " + look + "：铁砧 " + anvil + "，工作台 " + bench);
            context.assertTrue(GolemBenchBlock.front(bench) == look.getOpposite(),
                    "视线朝 " + look + "：工作台正面朝 " + GolemBenchBlock.front(bench) + "，不是朝放置的人");
        }
        context.succeed();
    }

    @GameTest(structure = FLOOR, maxTicks = DEFAULT_TIMEOUT)
    public void rotationAndMirrorCarryTheFrontLikeALoom(GameTestHelper context) {
        // 织布机的 facing 就是正面：同一个正面的工作台和织布机，转任意角度、做任意镜像后正面仍然一致。
        for (Direction facing : LOOKS) {
            BlockState bench = GolemBlocks.golemBench().defaultBlockState().setValue(GolemBenchBlock.FACING, facing);
            BlockState loom = Blocks.LOOM.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, GolemBenchBlock.front(bench));
            for (Rotation rotation : Rotation.values()) {
                assertSameFront(context, loom.rotate(rotation), bench.rotate(rotation), facing + " 转 " + rotation);
            }
            for (Mirror mirror : Mirror.values()) {
                assertSameFront(context, loom.mirror(mirror), bench.mirror(mirror), facing + " 镜像 " + mirror);
            }
        }
        context.succeed();
    }

    @GameTest(structure = FLOOR, maxTicks = DEFAULT_TIMEOUT)
    public void benchIsAxeMineableAndDropsItself(GameTestHelper context) {
        // 斧子挖得快；挖掉掉回一张工作台。
        context.setBlock(BENCH, GolemBlocks.golemBench());
        BlockState state = context.getBlockState(BENCH);
        context.assertTrue(state.is(BlockTags.MINEABLE_WITH_AXE), "傀儡工作台不在斧子能挖的标签里");
        List<ItemStack> drops = Block.getDrops(state, context.getLevel(), context.absolutePos(BENCH), null);
        boolean dropsItself = drops.size() == 1 && drops.getFirst().is(GolemBlocks.golemBench().asItem())
                && drops.getFirst().getCount() == 1;
        context.assertTrue(dropsItself, "挖掉傀儡工作台的掉落不对：" + drops);
        context.succeed();
    }

    @GameTest(structure = FLOOR, maxTicks = DEFAULT_TIMEOUT)
    public void recipeCraftsTheBenchAndUnlocksWithAnIronBlock(GameTestHelper context) {
        // 合成格照占位配方摆出一张工作台，命中的是本仓配方；玩家身上第一次有铁块时配方书解锁这条配方。
        // 解锁走进度触发器，玩家要像真玩家一样进了服务器才有进度监听，所以不用 Fabric 的假玩家。
        ServerLevel level = context.getLevel();
        CraftingInput input = CraftingInput.of(3, 3, List.of(
                stack(Items.IRON_BLOCK), stack(Items.IRON_BLOCK), stack(Items.IRON_BLOCK),
                ItemStack.EMPTY, stack(Items.CRAFTING_TABLE), ItemStack.EMPTY,
                stack(Items.IRON_INGOT), stack(Items.IRON_INGOT), stack(Items.IRON_INGOT)));
        var match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        context.assertTrue(match.isPresent() && match.get().id().equals(RECIPE), "占位配方没有命中本仓配方：" + match);
        ItemStack result = match.get().value().assemble(input);
        context.assertTrue(result.is(GolemBlocks.golemBench().asItem()) && result.getCount() == 1, "合成结果不对：" + result);

        ServerPlayer player = context.makeMockServerPlayerInLevel();
        context.assertFalse(player.getRecipeBook().contains(RECIPE), "还没拿到铁块配方书就有了傀儡工作台");
        ItemStack ironBlock = stack(Items.IRON_BLOCK);
        player.getInventory().add(ironBlock.copy());
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), ironBlock);
        context.assertTrue(player.getRecipeBook().contains(RECIPE), "拿到铁块后配方书没有解锁傀儡工作台");
        context.succeed();
    }

    @GameTest(structure = FLOOR, maxTicks = CLAIM_TIMEOUT)
    public void unemployedVillagerClaimsTheBenchAndBecomesGolemancer(GameTestHelper context) {
        // 工作台登记为傀儡师的工作站点并可认领；无业村民走过去认领，职业变成傀儡师，工作站点记的是这张台子。
        context.setBlock(BENCH, GolemBlocks.golemBench().defaultBlockState()
                .setValue(GolemBenchBlock.FACING, Direction.WEST));
        BlockPos bench = context.absolutePos(BENCH);
        var site = context.getLevel().getPoiManager().getType(bench);
        context.assertTrue(site.isPresent() && site.get().is(GolemancerProfession.JOB_SITE)
                && site.get().is(PoiTypeTags.ACQUIRABLE_JOB_SITE), "工作台没有登记成可认领的傀儡师工作站点：" + site);
        Villager villager = context.spawn(EntityTypes.VILLAGER, VILLAGER);
        context.assertTrue(villager.getVillagerData().profession().is(VillagerProfession.NONE),
                "刚生成的村民不是无业：" + villager.getVillagerData());
        context.succeedWhen(() -> {
            context.assertTrue(GolemancerProfession.is(villager), "村民还没成傀儡师：" + villager.getVillagerData());
            var claimed = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
            context.assertTrue(claimed.isPresent() && claimed.get().pos().equals(bench), "认领的工作站点不对：" + claimed);
        });
    }

    private static BlockState place(GameTestHelper context, Player player, Item item, BlockPos relative) {
        // 对着下面那格地面的顶面右键，和玩家放方块一样经过方块物品与它的放置规则。
        BlockPos floor = context.absolutePos(relative.below());
        ItemStack stack = stack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        var result = ((BlockItem) item).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
        context.assertTrue(result.consumesAction(), "没放下 " + item + "：" + result);
        return context.getBlockState(relative);
    }

    private static void assertSameFront(GameTestHelper context, BlockState loom, BlockState bench, String step) {
        // 织布机正面就是它的 facing。
        Direction expected = loom.getValue(BlockStateProperties.HORIZONTAL_FACING);
        context.assertTrue(GolemBenchBlock.front(bench) == expected,
                step + "：工作台正面朝 " + GolemBenchBlock.front(bench) + "，织布机朝 " + expected);
    }

    private static ItemStack stack(Item item) {
        // 一个物品一格。
        return new ItemStack(item);
    }
}

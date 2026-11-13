package dev.kizuna.meridian.golem.fabric;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 傀儡师住处：服务器结构列表里有它，原版 /place template 摆出来铁砧那格是工作台；转着摆时工作台跟着转。 */
public final class GolemStationGameTest {
    private static final String PLOT = "meridian_golem_test:station_plot";
    private static final int TIMEOUT = 100;
    private static final Identifier STATION = GolemFabric.id("golem_station");
    /** 住处 14×9×16；场地 16×11×18，地面上留一圈空。 */
    private static final BlockPos SIZE = new BlockPos(14, 9, 16);
    private static final BlockPos ORIGIN = new BlockPos(1, 1, 1);
    /** 顺时针转 90° 时 NBT 的 (x, z) 落到原点的 (-z, x)，原点放在场地东边才落得进场地。 */
    private static final BlockPos ROTATED_ORIGIN = new BlockPos(15, 1, 1);
    /** 用户 NBT 里铁砧那一格。 */
    private static final BlockPos ANVIL_CELL = new BlockPos(6, 1, 12);

    @GameTest(structure = PLOT, maxTicks = TIMEOUT)
    public void stationPlacesWithTheBenchWhereTheAnvilWas(GameTestHelper context) {
        // 结构蓝图读的也是服务器这份结构列表；摆出来铁砧那格是朝西的工作台，正面朝着屋里那格空地，整座没有铁砧。
        MinecraftServer server = context.getLevel().getServer();
        boolean listed = server.getStructureManager().listTemplates().anyMatch(STATION::equals);
        context.assertTrue(listed, "服务器的结构列表里没有 " + STATION);
        place(context, ORIGIN, "");
        BlockPos cell = ORIGIN.offset(ANVIL_CELL);
        assertBench(context, cell, Direction.WEST);
        assertOnlyBench(context, ORIGIN, cell);
        context.succeed();
    }

    @GameTest(structure = PLOT, maxTicks = TIMEOUT)
    public void rotatedStationTurnsTheBenchWithIt(GameTestHelper context) {
        // 顺时针转 90° 摆：铁砧那格落到 (-z, x)，facing 由西转成北，正面仍朝着屋里那格空地。
        place(context, ROTATED_ORIGIN, " clockwise_90");
        BlockPos cell = ROTATED_ORIGIN.offset(-ANVIL_CELL.getZ(), ANVIL_CELL.getY(), ANVIL_CELL.getX());
        assertBench(context, cell, Direction.NORTH);
        context.succeed();
    }

    private static void place(GameTestHelper context, BlockPos origin, String rotation) {
        // 和玩家输入的命令一样经原版 /place template，NBT 原点落在这一格。
        BlockPos at = context.absolutePos(origin);
        String command = "place template " + STATION + " " + at.getX() + " " + at.getY() + " " + at.getZ() + rotation;
        MinecraftServer server = context.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    private static void assertBench(GameTestHelper context, BlockPos cell, Direction facing) {
        // 那一格是工作台、facing 对，正面前面那格是空气（屋里的过道），不是被墙挡住。
        BlockState state = context.getBlockState(cell);
        context.assertTrue(state.is(GolemBlocks.golemBench()), "住处铁砧那格不是傀儡工作台：" + state);
        context.assertTrue(state.getValue(GolemBenchBlock.FACING) == facing, "工作台朝向不对：" + state);
        BlockState inFront = context.getBlockState(cell.relative(GolemBenchBlock.front(state)));
        context.assertTrue(inFront.isAir(), "工作台正面被挡住了：" + inFront);
    }

    private static void assertOnlyBench(GameTestHelper context, BlockPos origin, BlockPos bench) {
        // 整座住处里没有铁砧，工作台只有那一张。
        BlockPos last = origin.offset(SIZE).offset(-1, -1, -1);
        for (BlockPos pos : BlockPos.betweenClosed(origin, last)) {
            BlockState state = context.getBlockState(pos);
            context.assertFalse(state.is(Blocks.ANVIL), "住处里还有铁砧：" + pos);
            context.assertFalse(state.is(GolemBlocks.golemBench()) && !pos.equals(bench), "住处里多了一张工作台：" + pos);
        }
    }
}

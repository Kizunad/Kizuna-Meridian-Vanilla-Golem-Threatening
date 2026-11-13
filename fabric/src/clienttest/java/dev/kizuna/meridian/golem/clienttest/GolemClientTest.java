package dev.kizuna.meridian.golem.clienttest;

import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import dev.kizuna.meridian.golem.fabric.GolemBenchBlock;
import dev.kizuna.meridian.golem.fabric.GolemBlocks;
import dev.kizuna.meridian.golem.fabric.GolemancerProfession;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * 傀儡工作台和傀儡师住处的真实客户端截图：草地上一张工作台；一个无业村民自己走去认领、成了傀儡师，站在台边；
 * 物品栏里指着工作台看提示；随包住处用原版 {@code /place template} 摆出，从西南、东北两边斜 45° 各拍全景，再进屋拍
 * 换上工作台的那一格。截图只摆场景，不比对像素；村民认领、住处里那一格是不是朝西的工作台在服务端核对。
 */
public final class GolemClientTest implements FabricClientGameTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 960;
    private static final String BENCH = "meridian_golem:golem_bench";
    private static final String STATION = "meridian_golem:golem_station";
    /** 工作台放在场地原点，facing=north，正面朝东；镜头从东南看，正面、南侧面和顶面都在画里。 */
    private static final BlockPos BENCH_CELL = BlockPos.ZERO;
    private static final Vec3 BENCH_CAMERA = new Vec3(1.7, 1.2, 2.1);
    /** 村民先放在台子东边两格外，认领后挪到台子正面那格偏北，面朝镜头；镜头在东南，台子在村民左手边不被挡。 */
    private static final BlockPos VILLAGER_SPAWN = new BlockPos(3, 0, 0);
    private static final Vec3 VILLAGER_POSE = new Vec3(1.5, 0.0, -0.3);
    private static final float VILLAGER_YAW = -30.0F;
    private static final Vec3 VILLAGER_FOCUS = new Vec3(1.0, 0.9, 0.1);
    private static final Vec3 VILLAGER_CAMERA = new Vec3(2.8, 1.0, 3.5);
    private static final int TICKS_PER_SECOND = 20;
    private static final int CLAIM_TIMEOUT_TICKS = 60 * TICKS_PER_SECOND;
    private static final int VILLAGER_SEARCH = 16;
    /** 原版物品栏：底板 176×166 居中，快捷栏第一格左上角在 (8, 142)，一格 16 像素。 */
    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_HEIGHT = 166;
    private static final int HOTBAR_FIRST_X = 8 + 8;
    private static final int HOTBAR_FIRST_Y = 142 + 8;
    /** 住处 NBT 原点落在场地 (-7, 0, -8)，整座房子的中心大致在场地原点上方。 */
    private static final BlockPos STATION_ORIGIN = new BlockPos(-7, 0, -8);
    private static final Vec3 STATION_FOCUS = new Vec3(0.0, 2.5, 0.0);
    private static final List<Overview> OVERVIEWS = List.of(
            new Overview("golem_04_station_southwest", new Vec3(-12.0, 7.0, 12.0)),
            new Overview("golem_05_station_northeast", new Vec3(12.0, 7.0, -12.0)));
    /** 原来的铁砧在 NBT (6,1,12)，正面朝北对着 (6,1,11) 那格过道；人站在过道北头看过去。 */
    private static final BlockPos STATION_BENCH = STATION_ORIGIN.offset(6, 1, 12);
    private static final Vec3 INTERIOR_EYE = new Vec3(-0.5, 2.4, 0.4);

    /** 一张住处全景：截图名、镜头在 {@link #STATION_FOCUS} 的哪个方向多远。 */
    private record Overview(String shot, Vec3 camera) {
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        // 一个世界里依次拍工作台、傀儡师、物品提示和住处。
        context.getInput().resizeWindow(WINDOW_WIDTH, WINDOW_HEIGHT);
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            Stage stage = Stage.prepare(context, world);
            bench(stage);
            golemancer(context, stage);
            tooltip(context, stage);
            station(stage);
        }
    }

    private static void bench(Stage stage) {
        // 草地上单放一张工作台，看三个面的贴图。
        stage.clear();
        stage.spectate();
        stage.place(BENCH_CELL, BENCH + "[facing=north]");
        Vec3 focus = stage.center(BENCH_CELL);
        stage.look(focus.add(BENCH_CAMERA), focus);
        stage.shoot("golem_01_bench");
    }

    private static void golemancer(ClientGameTestContext context, Stage stage) {
        // 台子还在：旁边召一个无业村民，等它自己认领成傀儡师，冻住挪到台子东南角面朝镜头，看职业外袍。
        stage.summon("villager", VILLAGER_SPAWN);
        for (int waited = 0; !claimed(stage); waited += TICKS_PER_SECOND) {
            if (waited >= CLAIM_TIMEOUT_TICKS) {
                throw new AssertionError("无业村民 " + CLAIM_TIMEOUT_TICKS / TICKS_PER_SECOND + " 秒内没认领傀儡工作台");
            }
            context.waitTicks(TICKS_PER_SECOND);
        }
        Vec3 pose = stage.at(VILLAGER_POSE);
        stage.computeOnServer(server -> {
            Villager villager = villager(stage, server);
            villager.setNoAi(true);
            villager.snapTo(pose.x, pose.y, pose.z, VILLAGER_YAW, 0.0F);
            villager.setYHeadRot(VILLAGER_YAW);
            villager.setYBodyRot(VILLAGER_YAW);
            return villager;
        });
        Vec3 focus = stage.at(VILLAGER_FOCUS);
        stage.look(focus.add(VILLAGER_CAMERA), focus);
        stage.shoot("golem_02_golemancer");
        String profession = stage.computeOnServer(
                server -> villager(stage, server).getVillagerData().profession().getRegisteredName());
        LOGGER.info("[傀儡截图] golem_02_golemancer：村民职业 {}", profession);
    }

    private static boolean claimed(Stage stage) {
        // 场地里那个村民已经是傀儡师。
        return stage.computeOnServer(server -> GolemancerProfession.is(villager(stage, server)));
    }

    private static Villager villager(Stage stage, MinecraftServer server) {
        // 场地里只召了一个村民。
        AABB area = new AABB(stage.block(BlockPos.ZERO)).inflate(VILLAGER_SEARCH);
        List<Villager> villagers = server.overworld().getEntitiesOfClass(Villager.class, area);
        if (villagers.size() != 1) {
            throw new AssertionError("场地里应该正好一个村民：" + villagers);
        }
        return villagers.getFirst();
    }

    private static void tooltip(ClientGameTestContext context, Stage stage) {
        // 清场后生存模式快捷栏第一格放一张工作台，按物品栏键打开原版物品栏，光标指着它看名字下那句用途。
        stage.clear();
        stage.survive();
        stage.command("clear @a");
        stage.command("item replace entity @a hotbar.0 with " + BENCH);
        stage.settle();
        context.getInput().pressKey(options -> options.keyInventory);
        context.waitForScreen(InventoryScreen.class);
        Vec2 slot = context.computeOnClient(client -> {
            Window window = client.getWindow();
            float scale = (float) window.getScreenWidth() / window.getGuiScaledWidth();
            int left = (window.getGuiScaledWidth() - INVENTORY_WIDTH) / 2;
            int top = (window.getGuiScaledHeight() - INVENTORY_HEIGHT) / 2;
            return new Vec2((left + HOTBAR_FIRST_X) * scale, (top + HOTBAR_FIRST_Y) * scale);
        });
        context.getInput().setCursorPos(slot.x, slot.y);
        stage.settle();
        stage.shoot("golem_03_tooltip");
        context.runOnClient(client -> client.player.closeContainer());
        stage.command("clear @a");
    }

    private static void station(Stage stage) {
        // 清场摆随包住处，先核对原来铁砧那格是朝西的工作台，再拍两张全景和屋里那一格。
        stage.clear();
        stage.spectate();
        stage.template(STATION_ORIGIN, STATION);
        stage.settle();
        BlockState state = stage.computeOnServer(
                server -> server.overworld().getBlockState(stage.block(STATION_BENCH)));
        if (!state.is(GolemBlocks.golemBench()) || state.getValue(GolemBenchBlock.FACING) != Direction.WEST) {
            throw new AssertionError("住处原来铁砧那格不是朝西的傀儡工作台：" + state);
        }
        Vec3 focus = stage.at(STATION_FOCUS);
        for (Overview overview : OVERVIEWS) {
            stage.look(focus.add(overview.camera()), focus);
            stage.shoot(overview.shot());
        }
        stage.look(stage.at(INTERIOR_EYE), stage.center(STATION_BENCH));
        stage.shoot("golem_06_station_bench");
    }
}

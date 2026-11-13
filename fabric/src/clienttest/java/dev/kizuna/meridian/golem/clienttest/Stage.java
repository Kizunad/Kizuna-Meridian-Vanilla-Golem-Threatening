package dev.kizuna.meridian.golem.clienttest;

import java.util.Locale;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 截图场地：世界原点地表上的一片空地。摆方块、召唤实体、挪镜头都走服务端命令，和玩家在游戏里输入的一样。 */
final class Stage {
    private static final double EYE_HEIGHT = 1.62;
    private static final int SETTLE_TICKS = 10;
    private static final int CLEAR_RADIUS = 16;
    // 住处 9 格高，清到 12 格。
    private static final int CLEAR_HEIGHT = 12;
    private static final boolean CLEAR_SENT_HISTORY = false;
    private final ClientGameTestContext context;
    private final TestSingleplayerContext world;
    private final BlockPos origin;

    private Stage(ClientGameTestContext context, TestSingleplayerContext world, BlockPos origin) {
        // 场地只由 prepare 建。
        this.context = context;
        this.world = world;
        this.origin = origin;
    }

    static Stage prepare(ClientGameTestContext context, TestSingleplayerContext world) {
        // 场地原点取 (0, 0) 处的地表；正午、晴天、不刷怪，旁观模式不画手和方块描边。
        world.getConnection().waitForChunksRender();
        int surface = world.getServer().computeOnServer(
                server -> server.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
        Stage stage = new Stage(context, world, new BlockPos(0, surface, 0));
        stage.command("time set noon");
        stage.command("gamerule advance_time false");
        stage.command("weather clear");
        stage.spectate();
        return stage;
    }

    void spectate() {
        // 拍场景：旁观模式，界面藏起来。
        command("gamemode spectator @a");
        context.runOnClient(client -> {
            if (!client.gui.hud.isHidden()) {
                client.gui.hud.toggle();
            }
        });
    }

    void survive() {
        // 拍物品栏：生存模式才开原版物品栏（创造模式开的是创造物品栏），界面显示出来。
        command("gamemode survival @a");
        context.runOnClient(client -> {
            if (client.gui.hud.isHidden()) {
                client.gui.hud.toggle();
            }
        });
    }

    void clear() {
        // 换场前清掉上一场的方块和玩家以外的实体；被压过的草会变成泥土，地面一并铺回草。
        // 实体直接移除：/kill 会放死亡动画和消失的烟，烟会留在下一场的画面里。
        BlockPos from = origin.offset(-CLEAR_RADIUS, 0, -CLEAR_RADIUS);
        BlockPos to = origin.offset(CLEAR_RADIUS, CLEAR_HEIGHT, CLEAR_RADIUS);
        command("fill " + coordinates(from) + " " + coordinates(to) + " air");
        command("fill " + coordinates(from.below()) + " " + coordinates(to.atY(origin.getY() - 1)) + " grass_block");
        AABB area = new AABB(Vec3.atLowerCornerOf(from), Vec3.atLowerCornerOf(to.offset(1, 1, 1)));
        world.getServer().runOnServer(server -> server.overworld()
                .getEntities((Entity) null, area, entity -> !(entity instanceof Player))
                .forEach(Entity::discard));
    }

    void place(BlockPos offset, String state) {
        // 原版 /setblock，方块状态照原版写法。
        command("setblock " + coordinates(origin.offset(offset)) + " " + state);
    }

    void template(BlockPos offset, String id) {
        // 原版 /place template：NBT 原点落在这一格，不转不镜像。
        command("place template " + id + " " + coordinates(origin.offset(offset)));
    }

    void summon(String entity, BlockPos offset) {
        // 召唤在这一格底面正中。
        Vec3 feet = Vec3.atBottomCenterOf(origin.offset(offset));
        command(String.format(Locale.ROOT, "summon %s %.3f %.3f %.3f", entity, feet.x, feet.y, feet.z));
    }

    Vec3 at(Vec3 offset) {
        // 场地坐标换成世界坐标。
        return Vec3.atLowerCornerOf(origin).add(offset);
    }

    Vec3 center(BlockPos offset) {
        // 场地里这一格的正中。
        return Vec3.atCenterOf(origin.offset(offset));
    }

    BlockPos block(BlockPos offset) {
        // 场地里这一格的世界坐标。
        return origin.offset(offset);
    }

    void look(Vec3 eye, Vec3 target) {
        // 原版朝向：偏航 0 朝 +Z，俯仰为正时低头；tp 给的是脚底坐标，眼睛在其上方 1.62 格。
        Vec3 direction = target.subtract(eye);
        double yaw = Math.toDegrees(Math.atan2(-direction.x, direction.z));
        double pitch = Math.toDegrees(Math.atan2(-direction.y, direction.horizontalDistance()));
        command(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f",
                eye.x, eye.y - EYE_HEIGHT, eye.z, yaw, pitch));
        settle();
    }

    <T> T computeOnServer(Function<MinecraftServer, T> action) {
        // 在服务端线程上读世界。
        return world.getServer().computeOnServer(action::apply);
    }

    void command(String command) {
        // 以服务端身份执行，和管理员在聊天栏里输入相同。
        world.getServer().runCommand(command);
    }

    void settle() {
        // 等服务端结果同步到客户端，再等区块网格重建。
        context.waitTicks(SETTLE_TICKS);
        world.getConnection().waitForChunksRender();
    }

    void shoot(String name) {
        // 命令回显和右上角提示会挡画面，拍之前清掉；玩家发过的消息历史不清。
        context.runOnClient(client -> {
            client.gui.hud.getChat().clearMessages(CLEAR_SENT_HISTORY);
            client.gui.toastManager().clear();
        });
        context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
    }

    private static String coordinates(BlockPos pos) {
        // 命令里的整数坐标。
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}

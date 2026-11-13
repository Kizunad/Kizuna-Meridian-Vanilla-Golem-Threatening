package dev.kizuna.meridian.golem.fabric;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * 傀儡工作台：整格方块，傀儡师的工作站点。朝向沿用原版铁砧的约定：facing 是放置者视线方向顺时针转 90°，
 * 正面（挂傀儡面板那面）朝着放置的人，也就是 facing 再顺时针转 90° 那一面。这样和铁砧互换时 facing 原样照抄，
 * 工作面还在铁砧工作面那一侧。结构旋转、镜像时正面跟着转。
 */
public final class GolemBenchBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public GolemBenchBlock(BlockBehaviour.Properties properties) {
        // 默认朝北，放置时按铁砧的规则改。
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** 这个方块状态的正面朝哪：facing 顺时针转 90°。 */
    public static Direction front(BlockState state) {
        // 和铁砧一样，facing 与视线垂直，正面朝放置者。
        return state.getValue(FACING).getClockWise();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 与原版铁砧相同：视线方向顺时针转 90°。
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        // 结构旋转时朝向跟着转，正面随之转同样角度。
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        // 镜像要让正面落到镜像后的那一侧，再按正面倒推 facing；直接镜像 facing 会把正面翻到背面。
        Direction mirroredFront = mirror.mirror(front(state));
        return state.setValue(FACING, mirroredFront.getCounterClockWise());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // 唯一的方块状态是水平朝向。
        builder.add(FACING);
    }
}

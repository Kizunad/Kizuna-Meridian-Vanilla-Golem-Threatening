package dev.kizuna.meridian.golem.fabric;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** 傀儡工作台的方块物品：名字下面多一行灰字，说它是谁的工作台。 */
public final class GolemBenchItem extends BlockItem {
    public static final String DESCRIPTION_KEY = "block.meridian_golem.golem_bench.desc";

    public GolemBenchItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        // 原版的附加行之后补一句用途，灰色与原版物品说明一致。
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable(DESCRIPTION_KEY).withStyle(ChatFormatting.GRAY));
    }
}

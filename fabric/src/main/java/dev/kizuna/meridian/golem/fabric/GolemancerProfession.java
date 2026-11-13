package dev.kizuna.meridian.golem.fabric;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

/**
 * 傀儡师职业与傀儡工作台工作站点。无业村民认领傀儡工作台就成了傀儡师，工作声与原版工具匠相同。
 * 交易还没定：每级都没有交易表，原版遇到没有交易表的等级就不加交易，傀儡师右键只会摇头。
 */
public final class GolemancerProfession {
    public static final Identifier ID = GolemFabric.id("golemancer");
    public static final ResourceKey<VillagerProfession> KEY = ResourceKey.create(Registries.VILLAGER_PROFESSION, ID);
    public static final ResourceKey<PoiType> JOB_SITE = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, ID);
    // 与原版职业工作站点相同：一张工作台只给一个村民，站点范围 1 格。
    private static final int MAX_TICKETS = 1;
    private static final int VALID_RANGE = 1;

    private GolemancerProfession() {
        // 工作站点和职业只在模组初始化时注册一次。
    }

    static void register() {
        // 工作站点按傀儡工作台的全部朝向登记；职业的持有和认领都只认这个站点。
        PoiHelper.register(ID, MAX_TICKETS, VALID_RANGE, GolemBlocks.golemBench());
        Predicate<Holder<PoiType>> jobSite = holder -> holder.is(JOB_SITE);
        Component name = Component.translatable("entity." + ID.getNamespace() + ".villager." + ID.getPath());
        VillagerProfession profession = new VillagerProfession(name, jobSite, jobSite,
                ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_TOOLSMITH, Int2ObjectMaps.emptyMap());
        Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, ID, profession);
    }

    /** 村民现在是不是傀儡师。 */
    public static boolean is(Villager villager) {
        // 只看职业，不看等级。
        return villager.getVillagerData().profession().is(KEY);
    }
}

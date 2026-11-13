package dev.kizuna.meridian.golem.verification;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 傀儡仓数据包与资源包：JSON 都能解析，名字、配方、标签、模型贴图互相对上，随包结构与 wiki 记的 SHA-256 一致。 */
public final class GolemResourcesTest {
    private static final String NAMESPACE = "meridian_golem";
    private static final String PREFIX = NAMESPACE + ":";
    private static final String BENCH = PREFIX + "golem_bench";
    private static final String DATA = "data/" + NAMESPACE + "/";
    private static final String ASSETS = "assets/" + NAMESPACE + "/";
    private static final List<String> LANGUAGES = List.of("zh_cn", "en_us");
    private static final Set<String> NAME_KEYS = Set.of("block.meridian_golem.golem_bench",
            "block.meridian_golem.golem_bench.desc", "entity.meridian_golem.villager.golemancer",
            "itemGroup.meridian_golem.golem");
    private static final Set<String> FACINGS = Set.of("facing=north", "facing=east", "facing=south", "facing=west");
    private static final String STATION = DATA + "structure/golem_station.nbt";

    @Test
    public void everyJsonFileParses() throws IOException {
        // 数据包和资源包里每个 JSON 都是对象，打包前就把手误挡住。
        List<Path> files;
        try (Stream<Path> walk = Files.walk(resources())) {
            files = walk.filter(path -> path.toString().endsWith(".json")).toList();
        }
        assertFalse(files.isEmpty());
        for (Path file : files) {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            assertTrue(file.toString(), parsed.isJsonObject());
        }
    }

    @Test
    public void bothLanguagesNameTheBenchProfessionTabAndTooltip() throws IOException {
        // 方块名、用途一句、职业名、创造栏名在中英语言包里都有，两份语言包的键完全一致。
        Set<String> chinese = json(ASSETS + "lang/zh_cn.json").keySet();
        assertEquals(NAME_KEYS, chinese);
        for (String language : LANGUAGES) {
            assertEquals(language, chinese, json(ASSETS + "lang/" + language + ".json").keySet());
        }
    }

    @Test
    public void placeholderRecipeAndItsRecipeBookUnlock() throws IOException {
        // 占位配方：上排 3 铁块、中间工作台、下排 3 铁锭；配方书在拿到铁块时解锁这条配方。
        JsonObject recipe = json(DATA + "recipe/golem_bench.json");
        assertEquals(List.of("BBB", " C ", "III"), strings(recipe.getAsJsonArray("pattern").asList()));
        JsonObject key = recipe.getAsJsonObject("key");
        assertEquals("minecraft:iron_block", key.get("B").getAsString());
        assertEquals("minecraft:crafting_table", key.get("C").getAsString());
        assertEquals("minecraft:iron_ingot", key.get("I").getAsString());
        assertEquals(BENCH, recipe.getAsJsonObject("result").get("id").getAsString());

        JsonObject unlock = json(DATA + "advancement/recipes/decorations/golem_bench.json");
        assertEquals(List.of(BENCH), strings(unlock.getAsJsonObject("rewards").getAsJsonArray("recipes").asList()));
        JsonObject criteria = unlock.getAsJsonObject("criteria");
        assertEquals(BENCH, criteria.getAsJsonObject("has_the_recipe").getAsJsonObject("conditions")
                .get("recipe").getAsString());
        assertTrue(criteria.getAsJsonObject("has_iron_block").toString().contains("minecraft:iron_block"));
    }

    @Test
    public void tagsAndLootListTheBench() throws IOException {
        // 工作站点进原版「可认领」标签，方块进斧子标签，挖掉掉回自己；都只追加不替换。
        assertEquals(Set.of(PREFIX + "golemancer"),
                tagValues("data/minecraft/tags/point_of_interest_type/acquirable_job_site.json"));
        assertEquals(Set.of(BENCH), tagValues("data/minecraft/tags/block/mineable/axe.json"));
        String loot = json(DATA + "loot_table/blocks/golem_bench.json").toString();
        assertTrue(loot, loot.contains("\"name\":\"" + BENCH + "\""));
    }

    @Test
    public void blockstateCoversEveryFacingAndModelTexturesExist() throws IOException {
        // 四个朝向都有变体；模型引用的本仓贴图都在，职业贴图村民和僵尸村民各一张。
        assertEquals(FACINGS, json(ASSETS + "blockstates/golem_bench.json").getAsJsonObject("variants").keySet());
        JsonObject textures = json(ASSETS + "models/block/golem_bench.json").getAsJsonObject("textures");
        for (String face : textures.keySet()) {
            String texture = textures.get(face).getAsString();
            if (texture.startsWith(PREFIX)) {
                String path = ASSETS + "textures/" + texture.substring(PREFIX.length()) + ".png";
                assertTrue(path, Files.isRegularFile(resource(path)));
            }
        }
        for (String entity : List.of("villager", "zombie_villager")) {
            Path texture = resource(ASSETS + "textures/entity/" + entity + "/profession/golemancer.png");
            assertTrue(texture.toString(), Files.isRegularFile(texture));
        }
    }

    @Test
    public void shippedStationMatchesTheHashInTheWiki() throws IOException, NoSuchAlgorithmException {
        // 随包结构改过就得同时更新 Structures.md 表里「随包 NBT」那一列；用户原件那一列也是 SHA，不能只看页面里有没有。
        byte[] station = Files.readAllBytes(resource(STATION));
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(station));
        assertEquals("`" + hash + "`", shippedHashCell());
    }

    private static String shippedHashCell() throws IOException {
        // Structures.md 表里随包文件那一行的最后一格是随包 NBT 的 SHA-256。
        Path wiki = Path.of(System.getProperty("golem.wiki"), "Structures.md");
        List<String> rows = Files.readAllLines(wiki, StandardCharsets.UTF_8).stream()
                .filter(line -> line.startsWith("|") && line.contains("`" + STATION + "`"))
                .toList();
        assertEquals(rows.toString(), 1, rows.size());
        String[] cells = rows.getFirst().split("\\|");
        return cells[cells.length - 1].trim();
    }

    private static Set<String> tagValues(String path) throws IOException {
        // 标签条目是字符串数组，本仓只追加不替换。
        JsonObject tag = json(path);
        assertFalse(path, tag.get("replace").getAsBoolean());
        return new TreeSet<>(strings(tag.getAsJsonArray("values").asList()));
    }

    private static List<String> strings(List<JsonElement> elements) {
        // JSON 字符串数组转成 Java 字符串。
        return elements.stream().map(JsonElement::getAsString).toList();
    }

    private static JsonObject json(String path) throws IOException {
        // 读 fabric 资源目录下的一个 JSON 对象。
        return JsonParser.parseString(Files.readString(resource(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static Path resource(String path) {
        // 资源目录由 Gradle 传入，与正式包打进去的是同一份。
        return resources().resolve(path);
    }

    private static Path resources() {
        // 验证读源文件，不经打包。
        return Path.of(System.getProperty("golem.fabricResources"));
    }
}

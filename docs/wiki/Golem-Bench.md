# 傀儡工作台与傀儡师

## 傀儡工作台 `meridian_golem:golem_bench`

- 整格方块，木身铁面：顶面一块铆钉铁板，正面一层搁板放凿子和锤子、下面挂一块傀儡脸的铁牌，侧面铁箍和铁链。
  底面用原版深色橡木木板。
- 材质属性照原版锻造台：硬度 2.5，木头的声音，斧子挖得快，徒手也能挖下来，挖掉掉回自己。
- 物品名下面一行灰字：「没活计的村民守着它，就成了傀儡师」。
- 在创造栏「傀儡」一页。

### 朝向

`facing` 沿用原版铁砧的约定：值是放置者视线方向顺时针转 90°，正面朝着放置的人，也就是 `facing` 再顺时针转 90°
那一面。

| facing | 正面朝 |
|---|---|
| north | 东 |
| east | 南 |
| south | 西 |
| west | 北 |

这样傀儡工作台和铁砧互换时 `facing` 原样照抄，工作面还在铁砧工作面那一侧。结构旋转、镜像时正面跟着转
（和正面就是 `facing` 的织布机转出来一样）。

### 配方（占位）

有序合成：上排 3 个铁块，中间一个工作台，下排 3 个铁锭。身上第一次有铁块时配方书解锁。配方是占位，等用户定。

（`fabric/.../GolemBlocks.java`、`GolemBenchBlock.java`、`GolemBenchItem.java`，
`data/meridian_golem/recipe/golem_bench.json`、`advancement/recipes/decorations/golem_bench.json`）

## 傀儡师 `meridian_golem:golemancer`

- 傀儡工作台是傀儡师的工作站点（原版的 POI），在原版「可认领工作站点」标签里：无业村民找到它、走过去认领，
  就成了傀儡师。一张台子只给一个村民。
- 工作时的声音与原版工具匠相同。
- 外袍上一条皮围裙、铁扣腰带，腰间别一把小锤；僵尸村民用同一张。
- **还没有交易**：每级都没有交易表，右键傀儡师只会摇头。工作台右键也没有界面。这两件等用户定。

（`fabric/.../GolemancerProfession.java`，`data/minecraft/tags/point_of_interest_type/acquirable_job_site.json`）

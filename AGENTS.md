# 仓库协作约定

- 简体中文沟通、文档和函数内注释；遵守 `~/.codex/AGENTS.md` 的代码形状规则。
- `fabric/` 是 Java 模组：傀儡工作台方块、傀儡师职业与工作站点、创造栏，以及数据包、语言、模型、贴图和随包结构；
  `src/gametest` 是服务端 GameTest，`src/clienttest` 是真实客户端截图。`verification/` 只读资源源文件做单元验证。
- 依赖的附属都是硬依赖，不写「没装就跳过」的分支或测试。现在只依赖 Minecraft 与 Fabric API。
- 随包结构只改用户 NBT 里点名的格子，改法和两边的 SHA-256 记在 `docs/wiki/Structures.md`。
- 文档来源为 `docs/wiki/`。
- 截图在 `fabric/build/clienttest/screenshots/`（WSL 下默认驱动退出时 JVM 崩，所以用软件渲染）。
- 未经授权不提交、不推送、不创建远端。

## 构建和测试

构建、测试一律走工作区的 `kz`（`~/Code/Kizuna-remote-session/scripts/kz`），不直接运行 `./gradlew`。细则以 `~/Code/Kizuna-remote-session/handoff/codex-worker-rules.md` 为准，动手前先读。

- 单测、构建和服务端 GameTest：`kz test ~/Code/<本仓的 worktree>`
- 要带上别的仓的 worktree 一起测：每个加一次 `--with ~/Code/<那个仓的 worktree>`
- 客户端测试：`kz client ~/Code/<本仓的 worktree>` 列出有哪些项，`kz client ~/Code/<本仓的 worktree> <项>` 跑其中一项
- 要自己写 Gradle 参数时：`kz gradle ~/Code/<本仓的 worktree> -- <参数>`
- 不提交、不推送；在自己的 worktree（`~/Code/wt-*`）里改，不改主目录。

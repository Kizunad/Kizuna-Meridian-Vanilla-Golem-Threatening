# 仓库协作约定

- 简体中文沟通、文档和函数内注释；遵守 `~/.codex/AGENTS.md` 的代码形状规则。
- `fabric/` 是 Java 模组：傀儡工作台方块、傀儡师职业与工作站点、创造栏，以及数据包、语言、模型、贴图和随包结构；
  `src/gametest` 是服务端 GameTest，`src/clienttest` 是真实客户端截图。`verification/` 只读资源源文件做单元验证。
- 依赖的附属都是硬依赖，不写「没装就跳过」的分支或测试。现在只依赖 Minecraft 与 Fabric API。
- 随包结构只改用户 NBT 里点名的格子，改法和两边的 SHA-256 记在 `docs/wiki/Structures.md`。
- 文档来源为 `docs/wiki/`。Gradle 一律经 `~/Code/Kizuna-remote-session/scripts/kz-gradle-lock` 并带 `--no-daemon`。
- 验证：`./gradlew --no-daemon :verification:test :fabric:build :fabric:runGametest`。截图：
  `env -u DISPLAY -u WAYLAND_DISPLAY LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s "-screen 0 1280x960x24" ./gradlew --no-daemon
  :fabric:runClientGametest`，图在 `fabric/build/clienttest/screenshots/`（WSL 下默认驱动退出时 JVM 崩，所以用软件渲染）。
- 未经授权不提交、不推送、不创建远端。

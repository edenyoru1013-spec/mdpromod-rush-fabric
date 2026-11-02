# MDProMod — 项目状态与交接文档

> 文档更新时间：2026-11-01 21:55 +08:00
> 本文档是**交接用**的项目状态说明。所有数字均来自实际文件/构建产物实测；
> 凡未实机验证的内容都显式标注为「未验证」，不要把未验证项当作已完成功能对待。
> 上一版快照：2026-10-31 21:35。
>
> 本轮修复卡组编辑器仍显示/进入大师规则、Rush 卡被反向判非法、卡池仍可切到大师的问题；
> 新卡组和旧卡组现在都固定迁移到 Rush。构建、依赖和临时缓存全部落在 D 盘。

---

## 1. 项目背景、目标与开发环境

### 1.1 起源

用户最初提供了一个 **Forge 1.20.1** 模组：

```text
D:\下载\mdpromod-0.1.0.jar
```

它把《游戏王》的 `ocgcore` 规则引擎、WindBot 人机、卡组编辑器、决斗盘、
Master Duel 卡图/演出/音效整合进 Minecraft。目标是移植到 **Fabric**，并最终加入 **Rush Duel**。

### 1.2 当前目标

1. 保留原模组核心功能：实体决斗盘、卡盒与卡组编辑、ocgcore 本地决斗、WindBot 人机、
   选卡/连锁/落点/表示形式等完整决策界面、召唤动画、Cut-in、特写、音频与 BGM
2. 在 Fabric 1.20.1 上稳定运行
3. 增加 Rush Duel 模式
4. 生成可独立安装的完整 JAR

### 1.3 当前存在的两个工程

| 工程 | 路径 | 说明 | 状态 |
|---|---|---|---|
| **原版（双规则）** | `work\mdpro-fabric` | 106 个 java，同时支持大师规则与 Rush | 冻结，不再开发 |
| **RD 独立版** | `work\mdpromod-rush-fabric` | 104 个 java，**只支持 Rush Duel** | **当前主线** |

RD 独立版于 2026-10-31 19:00 从原版 fork。两者 **modid 都是 `mdpromod`**——刻意设计，见 §10.1。

### 1.4 开发环境

- OS：Windows（PowerShell 5.1 / Git Bash 混用）
- JDK：`C:\Program Files\Java\jdk-21.0.11`（编译目标 Java 17）
- Gradle：8.12.1，wrapper 在
  `D:\gradle-home\wrapper\dists\gradle-8.12.1-bin\830jys8xkckz69cgvf9t0ocan\gradle-8.12.1\bin\gradle.bat`
- **Gradle 缓存只在 D 盘**：原有用户级缓存是 `D:\gradle-home`；2026-11-01 本轮又使用
  `work\gradle-home` 作为可写依赖缓存，并把 `TEMP`/`TMP` 指到其 `.tmp`。项目构建缓存仍在
  `work\mdpromod-rush-fabric\build` 与 `.gradle`。旧的 `C:\Users\30634\.gradle` 是历史副本。
- .NET SDK：`work\dotnet-sdk`（运行 `duel_host` 用）
- 游戏实例：`D:\QQ\聊天数据\pcl\.minecraft\versions\1.20.1-Fabric 0.16.14`

---

## 2. 版本信息（实测自 `gradle.properties` / `fabric.mod.json`）

| 项 | 值 |
|---|---|
| Minecraft | 1.20.1 |
| Fabric Loader | 0.16.10 |
| Fabric API | 0.92.12+1.20.1 |
| Java（编译目标） | 17（`options.release = 17`） |
| Fabric Loom | 1.9.1 |
| mod id / 命名空间 | `mdpromod` |
| mod version | 0.1.0-fabric |
| maven group | `net.mdpromod` |
| 包名 | `net.mdpromod`（**未迁移，见 §10.1**） |
| 入口 | `main: net.mdpromod.MDProMod`，`client: net.mdpromod.MDProModClient` |
| mixin 配置 | `mdpromod.mixins.json` |

`build.gradle` 额外 shade 两个本地 jar：`libs\gdx-1.12.1.jar`、`libs\spine-libgdx-4.2.12.jar`
（排除 gdx 的 Pixmap/Texture 类）。**没有** Rush 专属 task 或依赖配置。

---

## 3. 原始 MDPro 模组的结构与主要功能

> 本节基于仓库内代码与 `work\mdpro-repo` 归纳。MDPro3 上游完整结构**未做全面勘察**，
> 属「不确定」信息，后续若要对接上游请重新核实。

- **对局内核**：`ocgcore.dll` + `cards.cdb`（SQLite 卡库）
- **卡牌脚本**：Lua，打包在 `script.zip`
- **人机**：WindBot，由 .NET 宿主 `duel_host` 驱动
  （源码在 `work\mdpro-repo\tools\duel_host`，入口 `Program.cs`，AI 封装在 `WindBotAi.cs`）
- **宿主协议**：Minecraft 侧 `DuelHostProcess` 启动 `duel_host` 子进程，
  通过 **stdin/stdout 的 JSON 行**通信（不是网络 socket）

模组侧功能（代码归纳）：

1. 实体决斗盘 `DuelFieldEntity` + 卡盒 `CardCaseEntity` + 卡牌实体 `DuelCardEntity`
2. 3D 场地（32 个 zone，定义在 `data\mdpromod\duel_field_layout.json`）
3. 卡牌渲染与动画（召唤特效、cut-in 立绘、BGM/音效）
4. 卡组编辑器（类 MDPro3 卡盒界面）
5. 命令体系（`/mdpromod ...`）

---

## 4. 已完成的移植工作

- Forge → Fabric 1.20.1 移植
- 物理场地、卡牌实体、动画、音频、网络同步
- 卡库资源：**大师 15,019 条** + **Rush 3,481 条**（两者卡号交集为 **0**）
- 资源分发：大体积资源（卡图、内核、Lua、宿主）打包进 `data\mdpromod\bundle\`，
  首次启动由 `ModBundle` 解包到 `<实例>\mdpromod\`
- Rush Duel 全流程接入（独立内核 + 独立卡库 + 独立运行时目录）

### 4.1 当前交付产物（实测）

| 产物 | 大小 | SHA-256 |
|---|---:|---|
| **当前安装**（RD 独立版，实例 `mods\`） | 284,636,089 | `29BEF6574F187BFE17A6504222C07DBC1E303258B6F73D3C43927E4CDF213396` |
| RD 独立版发行副本（`outputs\mdpromod-rush-fabric-0.1.0\`） | 同上 | 同上（三处 sha256 已核对一致） |
| 上一版已安装 RD 独立版（本轮初始手牌/界面修复前，回滚用） | 284,635,325 | `D1C62251AD4AA4EC1564E0CC031809EC297F19A62ABFACD07A7140C00237202B`（`work\backups\mdpromod-rush-before-hand4-ui-20261101-215312.jar`） |
| 原版双规则版（`outputs\mdpromod-fabric-0.1.0\`） | 602,711,150 | `E7497274C2BF05A16DDD0B2E790D38916F5587AFB214E04353C51BAA94EB4E38` |
| 原版备份（回滚用） | 602,711,150 | `work\backups\mdpromod-master-full-bak-20261031-193020.jar` |
| 更早基线（落点映射有 bug） | 602,710,934 | `work\backups\mdpromod-0.1.0-fabric-full.jar.bak-20261031-175510` |
| **旧宿主 dll**（换了 `humanSeat1` 之前那份） | 1,309,184 | `work\backups\duel_host.dll.bak-20261031-2100` |

RD 独立版 dev jar：`work\mdpromod-rush-fabric\build\libs\mdpromod-0.1.0-fabric.jar`
（约 3.9 MB，改源码后重新生成，大小会小幅变动）。

本轮相对上一版的实质变化：
1. `DeckData` 新增规则字段，新卡组默认 Rush；旧 NBT 缺字段或写着 master 时自动迁移 Rush
2. 卡池过滤器固定为 Rush，不再出现/循环到「大师规则」；Rush 卡不再被反向判成大师卡组卡
3. Rush 融合怪兽统一按额外卡组排序、提示和校验
4. `gradle test` 增至 45 条断言，含真实 Rush 卡组、双席位卡面 owner、全部按钮命中、解包内容校验和提示卡号可见性

---

## 5. 已实现的 Rush Duel 玩法及具体规则

### 5.1 规则模式

`DuelRuleMode` 枚举。**RD 独立版里 `byOrdinal()` 恒返回 `RUSH`**，
即无论存档里存的是什么值，跑起来的规则永远是 Rush。

### 5.2 实际生效的 Rush 规则（无头对局中观察到）

| 规则 | 实测结果 |
|---|---|
| 场地 | 5 个怪兽槽位，由 `RDRule.lua` 禁用最外侧的 0 / 4，实际可用 **1 / 2 / 3 三列**；两个额外怪兽区遮暗 |
| 通常召唤 | **不限次数**（单回合最多观察到 3 次，受手牌/怪兽数量限制） |
| 准备阶段 | 无 |
| 主要阶段 2 | 无 |
| 开局手牌 | 双方各 4 张；之后每次通常抽卡抽 1 张 |
| 抽卡 | 之后每回合抽 1 张 |
| 构筑 | 主卡组 40–60；同名卡（含 alias 分组）最多 3 张 |
| 传说卡 | 怪兽 / 魔法 / 陷阱**各最多 1 种**、每种 1 张（靠 desc 含 `<传说卡>` 识别，卡池中共 132 张） |
| 额外卡组 | Rush 融合怪兽放入额外卡组，当前上限 15 张 |

### 5.3 Rush 运行时（实测目录内容）

`<实例>\mdpromod\rush-runtime\`：
`cards.cdb`（1,462,272 B）、`cards.json`、`ocgcore.dll`（2,448,384 B）、
`sqlite3.dll`、`script.zip`（Lua 打包在内）、`Data\`

Rush 与大师**共用同一份 `duel_host.exe`**，仅靠 **`--root` 指向不同目录** +
JSON 里的 `mode` 字段（`"rush"` / `"master"`）切换。

### 5.4 卡图来源

Rush 卡图**不随包分发**，由 `RushCardAssets` 从 MDPro3 安装目录外读：

```text
D:\Game\MDPro3\Expansions\RD正式卡.ypk
D:\Game\MDPro3\Expansions\RD先行卡.ypk
```

（用户确认保留的方案。缺点：离开这台机器或 MDPro3 不在原位就显示不出卡图。）

---

## 6. 核心类、关键文件路径及作用

源码根：`work\mdpromod-rush-fabric\src\main\java\net\mdpromod`（104 个文件，18,844 行）

### 6.1 对局与规则

| 文件 | 行数 | 作用 |
|---|---:|---|
| `DuelSessionManager.java` | 1455 | **对局总控**：起停宿主、状态推送、落点提示、选卡/连锁/决策分发、手牌同步 |
| `DuelHostProcess.java` | 1079 | 启动 `duel_host` 子进程，JSON 行协议收发，暴露 `handOf(seat)`、`turnPlayer()` 等 |
| `DuelFieldEntity.java` | 1023 | 决斗盘实体：两侧卡组绑定、座位表、规则模式、点击交互 |
| `DuelRuleMode.java` | ~60 | 规则枚举；**RD 版 `byOrdinal` 恒返回 RUSH** |
| `DeckRules.java` | ~200 | 卡组合法性校验（张数 / 同名 3 张 / 传说卡 / 卡池隔离） |
| `DuelCommands.java` | 599 | `/mdpromod` 命令体系（含 `case give`） |
| `DuelCardEntity.java` | ~169 | 场上卡牌实体；`owner` 存 **UUID 字符串**，用于隐藏卡可见性判定 |

### 6.2 场地布局与渲染

| 文件 | 行数 | 作用 |
|---|---:|---|
| `DuelFieldLayout.java` | 224 | 引擎地址 ↔ 物理格子映射。**Rush 下是恒等映射**（见 §10.2） |
| `data\mdpromod\duel_field_layout.json` | 32 zones | 场地坐标定义，两规则共用同一份 |
| `DuelFieldRenderer.java` | 297 | 场地渲染；`RUSH_HIDDEN_ZONES` 遮暗外侧列与额外怪兽区 |
| `DuelAnimationScene.java` | 528 | 动画场景与手牌/卡堆快照 |

### 6.3 卡库与卡组

| 文件 | 作用 |
|---|---|
| `CardRegistry.java` | **RD 版只加载 `rush_cards.json`**（大师 `cards.json` 已删除） |
| `CardInfo.java` | 卡牌信息；`isExtraDeckInRush()` 识别融合 / 同调 / 超量 / 连接 |
| `RushCardAssets.java` | **唯一的 Rush 专属类**：从 MDPro3 `.ypk` 读 Rush 卡图 |
| `CardCaseScreen.java` / `CardCaseItem.java` | 卡盒 UI 与物品 |
| `CardFilter.java` | 卡池筛选（来源 / 种类 / 额外） |

### 6.4 资源与路径

| 文件 | 作用 |
|---|---|
| `ModPaths.java` | 运行时根目录、bundle 解包目录、玩家卡组目录 |
| `ModBundle.java` | 首次启动把 `data\mdpromod\bundle\` 解到 `<实例>\mdpromod\`，靠 `manifest.txt` 逐条校验 |
| `slim_bundle.py`（工程根） | **资源裁剪脚本**：剔除大师资源 + 换 class + 重写 manifest，一步产出成品 |

### 6.5 重要目录

```text
D:\codexdate\2026-10-30\zhe\
├── PROJECT_STATUS.md          ← 本文档
├── work\
│   ├── mdpro-fabric\          ← 原版双规则（冻结）
│   ├── mdpromod-rush-fabric\  ← RD 独立版（主线）
│   ├── mdpro-repo\            ← 上游工具/资源（含 duel_host 源码）
│   ├── rush-invest\           ← 卡组导入与校验脚本
│   ├── dotnet-sdk\            ← 运行 duel_host 用
│   ├── backups\               ← 各版本 JAR / dll 备份
│   ├── full\                  ← 打包中间产物
│   ├── probe_prompt_seat.py   ← 席位探测：宿主到底在问谁（本轮新增）
│   ├── probe_rd_mechanics.py  ← RD 机制脚本层探测（本轮新增）
│   └── probe_rd_prompts.py    ← 打印提示序列，诊断"自动应答为什么答不对"（本轮新增）
├── outputs\
│   ├── mdpromod-fabric-0.1.0\       ← 原版双规则发行目录（未改动）
│   └── mdpromod-rush-fabric-0.1.0\  ← RD 独立版发行目录
```

回归网位置（本轮新增）：
```text
work\mdpromod-rush-fabric\src\test\java\net\mdpromod\
├── DuelFieldLayoutRushTest.java    ← Rush 落点恒等映射
├── ExtraDeckTrackerTest.java       ← 额外卡组记账 + 按席位独立
├── DuelRuleModeTest.java           ← 规则固化（byOrdinal 恒 RUSH）
└── DuelLocationTest.java           ← 位置文案
```

玩家卡组目录（运行时读盘，改完**不需要重启游戏**）：
```text
<实例>\config\mdpromod\decks\
```

---

## 7. 已知 Bug 与未解决的问题

### 7.1 已修

| 问题 | 根因 | 修法 |
|---|---|---|
| **Rush 落点错位一格**（提示不绿、左侧点不动、卡右移一列） | 代码假设"引擎把列重编号成 0/1/2"并多做 ±1 偏移；实际核心仍用 5 槽位、由 RDRule 禁掉 0/4 | `DuelFieldLayout` 改恒等映射，删 `rushSequence()` |
| 位置文案错位（N1） | `DuelLocation.label()` 按 `sequence+1` 报列号 | RD 版直接用 `sequence` 本身 |
| **带融合的 Rush 卡组开不了局** | Rush 融合卡未纳入额外卡组判定 | `CardInfo.isExtraDeckInRush()` 纳入融合位 `0x40` |
| `SHA256SUMS.txt` 是 CRLF，POSIX 校验全失败 | 文件行尾不一致 | 已转 LF（`sha256sum -c` 32/32 OK） |
| **`answerSeat()` 会把提示发给错的人**（本轮修） | 先查 `field.promptSeat()`（只在阶段提示里设过），再退回 `host.turnPlayer()`。对方回合轮到我方应答时必然发错 | 改成**优先用宿主每条提示自带的 `player` 字段**（实测 546,569 条无一缺失，且确实取 0/1 两种值）。详见 §10.5 |
| **p2 真人的额外卡组记账缺失**（本轮修） | `ExtraDeckTracker` 里写死"只记 controller 0"，`allCodes` 里写死 `side == 0` / `key = "p1"` | 改成按席位两份记账（`ExtraDeckTracker(int seat)` + `extraTrackers[2]` / `extraBases[2]`） |
| **★ Rush 模式下宿主自动代答席位 1**（本轮修，**真人对真人的真正阻塞点**） | `Program.cs` 有硬编码分支：Rush 且被问的是席位 1 → `AutoAnswer` 代答（`strategy="rush-basic"`），**与 `ai:false` 无关**。所以之前那套"放开 p2 放卡组 / 去 aiKey / 手牌双通道"只把界面推给了 p2，宿主根本不给 p2 提问 | 新增 `humanSeat1` 开关：只有远半场确实是真人时才不发代答。详见 §10.6 |
| **`update_zip.ps1` 在本机跑不了**（本轮修） | 用了 .NET Core 才有的 `[System.IO.Path]::GetRelativePath`（PS 5.1 无），且**原地 `Open(Update)`，抛错前已把归档写坏** | 自己算相对路径 + 先写临时文件再原子替换 + 可选 `-KeepBackup`。实测通过，见 §8.2 |
| **仓库没有 `src/test`**（本轮修） | `gradle test` 是 `NO-SOURCE` | 新增 4 个测试类 30 条断言（JUnit 5），`gradle test` 现在真跑，见 §8.2 |
| **卡组仍显示大师规则 / Rush 卡无法放入**（2026-11-01 修） | `DeckRules.problems()` 的 `else if` 把“Rush 卡用于 Rush 卡组”反向判成“不能用于大师规则”；卡池按钮还能切到大师，默认也是“全部卡池” | 删掉反向分支；`DeckData` 新卡组默认 Rush并在旧档载入时迁移；卡池固定 Rush；融合卡按额外卡组处理 |
| **双人局 p2 看不到卡、席位被 AI 代打**（2026-11-01 修） | 两处独立的席位 0 写死：`DuelAnimationScene` 只给 controller 0 写 owner UUID；开局又允许 p2 人类卡组在 UUID 未解析时继续，`humanSeat1=false` 后宿主自动代答 | 动画场景改双 owner；放卡组时记录实际玩家 UUID；人类 p2 必须解析到在线玩家，否则拒绝开局；桌面 owner 固定成 p1 |
| **联机时所有场地按钮点不动**（2026-11-01 修） | 客户端在发请求前用本地 `playerSide` 做了一次重复归属过滤，座位同步晚一拍就吞掉 p2 全部点击；按钮命中又只检查极薄水平面，稍偏就落到世界交互 | 删除客户端重复拦截，统一交服务端权威校验；改有厚度的 AABB 命中并扩大容错；遍历测试两边全部阶段/确认键 |
| **好友主机换包后双真人仍被 AI 代打**（2026-11-01 修） | 宿主只凭 `humanSeat1=false` 就触发 `rush-basic`，上层任何席位识别失败仍会回到自动应答 | 宿主记录“是否真的配置了 Rush 人机”；只有存在席位 1 人机时才自动代答。双真人即使 `humanSeat1=false` 也会把提示交给 Java |
| **好友换完整 JAR 后仍运行旧宿主**（2026-11-01 修） | 新旧 `duel_host.dll` 都是 1,309,184 B，`ModBundle` 只比大小，内容变了也跳过覆盖 | 文件大小相同时继续逐字节比较；内容不同就原子替换。新增同大小异内容回归测试 |
| **效果选自己手牌时显示成对方手牌背面**（2026-11-01 修） | `visibleCode()`、卡堆扫描和位置文案把 `controller=0` 写死成“己方”。`player=1` 提示中自己的手牌是 controller 1，于是被隐藏，controller 0 反被泄露 | 卡号可见性、可发动卡堆、位置文案、额外卡组和卡堆标题全部改为相对当前提示/客户端席位 |
| **效果要求宣言卡名时卡死（function 142 AnnounceCard）** | 宿主未解析 `player + count + opcode[]`，Java 也没有对应界面，核心会永久等待 | 宿主实现 opcode 栈过滤并返回候选卡；新增可搜索的“宣言卡名”界面，选择后回传卡号 |
| **选卡窗口重问会清空页码/选择，主卡组余量不可见** | 核心 RETRY 重发时客户端每次都新建 `SelectCardScreen`，页码重置；手牌同步没有携带卡组余量 | 重发的同一候选列表原位更新，保留页码和已选；`HandSync` 增加双方主卡组剩余并显示在手牌 HUD |

### 7.2 未解决 / 未验证（**不要把下面这些当作已修**）

1. **RD 独立版从未实机启动过**——装完还没重启游戏验证。本轮也没做（需要人进游戏）。
2. **真人对真人完全未测。** 本轮只是把阻塞点（宿主代答）和路由（`answerSeat`）修对了，
   **一行都没在真人对真人的实机场景里跑过**。用户说这块晚点再测。
3. **Maximum / Rush Fusion / Rush Ritual 的"实际发动"未验证。**
   本轮能证明的是：这几副卡组在宿主里能跑完且 **0 Lua 错误**
   （`RDRushDemo` 2/2、`RDRushFusionDemo` 2/2、`RDMaximumDemo` 1/2 seed）。
   但探针跑下来 **`spsummon` 候选一直是 0**，也就是融合/最大召唤**一次都没真正发动过**。
   原因：无头自动应答不会去选这些动作。要真正验证需要会打牌的脚本或手动实机测试。
4. **Rush 高亮/遮暗没有像素级截图验证**：只有代码级 + 单元测试级证明
   （掩码位序 == `zones()` 索引 == 渲染循环下标，与遮暗清单零交集；
   "只剩 seq 3"时掩码非 0——旧代码此处为 0）。**无客户端截图。**
5. **新增的"落点映射失败" warn 从未在实机日志里出现过**：修复后理论上不该出现，
   属未观察项，不要写成"已验证不存在"。
6. **位置文案的小不一致（已知、未改）**：场地魔法所在的 `(location 8, sequence 5)`
   会落进魔陷区兜底文案，显示成「魔陷区(第 5 格)」而不是「场地」。
   已在 `DuelLocationTest` 里钉住现状，改文案时记得同步测试。
7. **宿主侧 `humanSeat1` 只处理了 Rush 的席位 1**。大师规则那边席位 1 本来就发提示，
   不受影响；但 Rush 下如果 p2 中途掉线，宿主会一直等应答——**没有超时兜底**。
   目前认为可接受（对局本来也就废了），但没有实测过掉线场景。

---

## 8. 已完成的测试、编译结果与未验证功能

### 8.1 编译

- RD 独立版 `gradle build`：**BUILD SUCCESSFUL**
- 完整 JAR `jar --validate`（JDK 21.0.11）：**通过**
- ⚠ **重要教训**：gradle 增量缓存会让已删除的资源仍留在 jar 里。
  改资源后**必须 `gradle clean build`**，否则会误判"删掉了"。
  （本轮第一次没 clean，jar 只小了 1.6KB，差点漏判。）

### 8.2 已执行的测试

| 测试 | 结果 |
|---|---|
| 落点映射离线断言（QA 独立 harness，**外部临时目录**） | 168 条，0 失败。跑的是从 602MB 成品包内抽出的**真实字节码** |
| 掩码反推（实机日志真实提示 `(0,4,1)(0,4,2)(0,4,3)`） | 掩码 **84 = bit2/4/6**，正好覆盖 `mzone_1/2/3`，与遮暗清单**零交集**；"只剩 (0,4,3)"时掩码 64（旧代码此处为 0） |
| 无头 Rush 对局（seed 12345 / 777 / 2024） | `passed: true`，luaErrors 空，Retry 0，单回合通常召唤 ≤3 |
| 部署级复核 | 从实例内 602MB JAR 抽 class 跑同一套 168 条断言，全 PASS |
| 资源裁剪校验 | 4 项删除清零；manifest 226 行与产物**双向完全自洽**（0 缺失 / 0 字节不符 / 0 未登记 / 0 幽灵） |
| MDPro3 卡组导入 | 320 个 ydk 中 **287 套**通过校验并导入，落盘后逐个复校（内容哈希与源一致） |
| **`gradle test`（2026-11-01）** | **45 条断言全过**：落点映射 11、额外卡组记账 10、规则枚举 4、位置文案 6、卡组迁移/卡池/真实 Rush 卡组 5、双席位卡面 owner 1、按钮命中 3、解包内容校验 2、提示可见性 3（见 §8.4） |
| **宿主 `player` 字段探测**（`work\probe_prompt_seat.py`） | 546,569 条提示 **0 条缺 `player`**；master 模式关 AI 时该字段取到过 **0 和 1** 两种值；Rush 模式下**席位 1 从不出现**（这就是 §7.1 那个代答 bug 的证据） |
| **宿主改动回归（本轮）** | 双真人无 AI、且故意设 `humanSeat1=false`：seed 12345/777 → 90/62 条提示，席位出现 `[0,1]`；真正人机 seed 12345 → 42 条提示，席位只有 `[0]`，人机仍正常 |
| **宿主时点自检（AnnounceCard 版本）** | `--timing-selftest` 29 条断言通过；AnnounceCard 原始日志格式已解析验证 |
| **Rush 人机对局（新宿主）** | seed 12345 → 双方初始 `Draw` 均为 **4 张**，0 Lua 错误，0 retry，无准备阶段、无主要阶段 2，单回合普召 ≤3 |
| **`update_zip.ps1`（本轮修）** | PS 5.1 下语法 0 错误；实跑：更新 2 条 + 新增 1 条，产物 **3 个条目无重复**、内容正确、生成了备份 |
| **成品包 manifest 自洽（重新打包后）** | 226 行，**0 缺失 / 0 字节不符 / 0 未登记 / 0 幽灵**；`duel_host.dll` 登记与实际同为 1,309,184 |
| **RD 机制脚本层探测**（`work\probe_rd_mechanics.py`） | `RDRushDemo` 2/2、`RDRushFusionDemo` 2/2 跑完且 0 Lua 错误；`RDMaximumDemo` 1/2（另一个 seed 被宿主判"连续应答非法"，是应答脚本太笨，不是卡组问题） |

> 一个易踩的坑（本轮刚踩过）：`work\rush-invest\RushDemo.ydk` 是**大师规则**卡组
> （卡号 32864 / 549481 这类），拿它在 Rush 运行时里跑会得到 80 条
> `initial_effect: attempt to call an error function`——**不是运行时坏了**。
> Rush 卡组要用带 `RD` 前缀的那几副（卡号 12xxxxxxx）。

### 8.3 未验证的功能

- **RD 独立版从未实机启动过**（装完还没重启游戏验证）
- **真人对真人**完全未测（本轮只修对了阻塞点和路由，没跑过）
- Maximum / Rush Fusion / Rush Ritual 的**实际发动**（详见 §7.2-3）
- Rush AI 连招（目前只会普召、特召、进战阶、攻击）
- 卡组编辑器 / YDK 导入导出 / 选卡排序界面的系统回归
- 多人联机（除身份绑定与席位路由改造外）

### 8.4 回归网（`src/test`，本轮新增）

| 测试类 | 断言数 | 钉住的东西 |
|---|---:|---|
| `DuelFieldLayoutRushTest` | 11 | **Rush 落点恒等映射**（这是坏过一次的那个 bug）。含"只剩 seq 3 时掩码非 0"——旧代码此处算出来是 0 |
| `ExtraDeckTrackerTest` | 10 | 额外卡组记账规则 + **按席位互不干扰**（本轮新改的） |
| `DuelRuleModeTest` | 4 | 任何 ordinal / hostKey 都折成 RUSH，且 `MASTER` 常量不能删（旧存档 NBT 写的是 0） |
| `DuelLocationTest` | 5 | 位置文案直接用引擎序号（N1 那个 bug）+ 已知的小不一致 |
| `DeckRushMigrationTest` | 5 | 新卡组默认 Rush、旧 NBT 迁移、Rush 卡池不再误判、真实 `RDRushDemo` 合法、融合卡进入额外卡组 |
| `DuelSceneOwnerTest` | 1 | p1 / p2 各自持有自己的卡面 owner，p2 观众侧必须识别为席位 1 |
| `DuelControlHitTest` | 3 | p2 END、p1 战斗键、两边全部阶段/确认键均能从各自半场命中 |
| `ModBundleContentTest` | 2 | 同字节数但内容不同的 DLL 不会因大小相同被跳过 |
| `PromptVisibilityTest` | 3 | p1/p2 自己的手牌可见、对手手牌隐藏、双方墓地保持公开 |

---

## 9. 当前开发进度与下一步任务

### 9.1 已完成（按时间）

1. Rush 落点映射错位修复 → 验证 → 打包安装
2. MDPro3 卡组导入（287 套）→ 实例可用
3. **RD 独立 mod 拆分**：源码去大师化 + 资源裁剪（601MB → 284MB）
4. **联机身份绑定**：座位表（`P1Uuid`/`P2Uuid`）、`playerSide` 查表优先
5. **真人对真人三步**：放开 p2 放卡组 / 去 aiKey 强制 / 手牌与提示双通道
6. **（本轮）清掉上一版挂着的未解决问题**：`answerSeat` 改用宿主 `player` 字段、
   额外卡组按席位记账、**宿主 Rush 席位 1 代答**、修 `update_zip.ps1`、建 `src/test` 回归网
7. 上一轮重新打包安装：284,624,700 B / `6B82EDE8…F8A4F221`
8. **（2026-11-01 本轮）修复卡组规则显示与 Rush 卡误判**：新卡组默认 Rush、旧卡组自动迁移；
   重新 `clean build` + `slim_bundle.py` 并安装，284,625,060 B / `E3EF9AE1…F57EFE11`
9. **（2026-11-01 双席位修复）**：p2 卡面 owner、实际放卡玩家 UUID 和 `humanSeat1` 串联修正；
   重新构建安装，284,626,628 B / `37AC359F…275445A`
10. **（2026-11-01 按钮修复）**：删除客户端重复归属过滤，场地按钮改体积命中；
    重新构建安装，284,627,676 B / `4C283A56…D814657C`
11. **（2026-11-01 AI 席位修复）**：宿主自动代答改为只受“确有 Rush 人机”控制，
    不再被错误的 `humanSeat1=false` 触发；重新构建安装，284,627,727 B / `5BB46460…79843C85`
12. **（2026-11-01 解包内容校验）**：同大小资源改为逐字节比较；最终构建安装，
    284,628,237 B / `51C7DD20…119C77CE`
13. **（2026-11-01 席位可见性修复）**：提示卡号、手牌归属、卡堆/额外卡组标题按当前席位解析；
    重新构建安装，284,629,768 B / `4707BF09…86C85EB6`
14. **（2026-11-01 AnnounceCard）**：新增声明卡名窗口、opcode 候选过滤和选择回传；
    重新构建安装，284,635,325 B / `D1C62251…0237202B`
15. **（2026-11-01 手牌与选卡体验）**：初始手牌改 4；RETRY 原位更新保留页码/已选；
    手牌 HUD 显示剩余主卡组；重新构建安装，284,636,089 B / `29BEF657…4CDF213396`

### 9.2 下一步（按优先级）

| 优先级 | 任务 |
|---|---|
| P0 | **重启游戏实机验证 RD 独立版**（至今未做过）：能进世界、能开局、三列场地、卡图能读 |
| P0 | **测真人对真人**（用户说晚点做）：两人各放卡组，观察 p2 是否收到自己的手牌与提示。本轮已把阻塞点修掉，但一行都没实机跑过 |
| P1 | Maximum / Fusion / Ritual 的**实际发动**：需要会打牌的无头脚本，或手动实机测（§7.2-3） |
| P1 | 给 `humanSeat1` 加掉线兜底（p2 中途退出时宿主会一直等应答，§7.2-7） |
| P2 | Rush AI 连招、卡组编辑器 / YDK 导入导出 / 选卡排序的系统回归 |
| P2 | 把 `DuelCommands` 的 `start` 重载收敛一下（本轮为了传 `humanSeat1` 加到 7 个参数，有点乱） |

---

## 10. 架构设计、修改原因与注意事项

### 10.1 ★ 为什么保持 `modid = mdpromod` 不变（最重要）

RD 独立版**没有**改 modid，也没有迁移包名和资源目录。原因：

1. **天然互斥**：Fabric 遇到重复 modid 会拒绝加载，正好实现"只装一个"
2. **零迁移成本**：`assets\mdpromod\`、`data\mdpromod\bundle\` 全部不用动，
   省掉 104 个文件的包名重写
3. **旧存档兼容**：方块/物品 ID 不变，`新的世界 (2)` 等存档可直接继续用

代价：两个 JAR 不能同时存在。要恢复大师玩法就把 JAR 换回备份。

### 10.2 ★ Rush 落点为什么是恒等映射

核心保留 **5 个怪兽槽位（0..4）**，由 `RDRule.lua` 禁用最外侧的 0 和 4，
所以合法落点是 **1/2/3**，引擎 sequence 与物理列号**一一对应**。

早期代码错误地假设"引擎重新编号成 0/1/2"并做了 ±1 偏移，导致提示、点击、渲染
三条链同时错位一格。`DuelFieldLayout.java` 顶部注释**明确写了不许再做偏移**，改之前先读。

物理 0/4 与两个额外怪兽区仍**遮暗**显示（视觉上就是中间三列），
但 `engineAddress` 对它们返回 null（点击不提交），`zoneIdFor` 保留 0..4（暗列上的卡仍渲染）。

### 10.3 ★ 规则固化的做法：改根节点而非改 21 个文件

RD 独立版没有逐个清理 21 个文件里的 `mode == RUSH` 分支，
而是把 `DuelRuleMode.byOrdinal()`（NBT → 枚举的**唯一入口**）改成恒返回 `RUSH`，
让下游所有分支自然恒真。`MASTER` 枚举值**刻意保留**，只为兼容旧存档里写的 0。

好处是改动面小、不易出错；代价是代码里还留着 `mode` 参数的壳。

卡组自身另有一份 `DeckData.ruleMode`。新对象初始化为 RUSH；旧 NBT 无论缺字段还是写着
`master`，`load()` 都会归一为 RUSH，并在卡盒下次保存时写回 `ruleMode=rush`。
YDK 导入同样一律从 Rush 开始，因此不存在“新建后还要手动选规则”的步骤。

### 10.4 真人对真人是怎么做到的

关键发现：.NET 宿主 `Program.cs:117` 是 **条件启用** AI：

```csharp
if (command.TryGetProperty("ai", out var aiProp) && aiProp.ValueKind == JsonValueKind.True)
```

所以 `DuelHostProcess` 改成 `command.addProperty("ai", aiName != null && !aiName.isBlank())`
即可让 seat 1 不被 AI 托管。**不需要改 C# 代码**。

双通道实现要点：

- `DuelSessionManager` 新增 `p2PlayerId` + `actorFor(seat)` + `answerSeat()`
- 用 sed 把 **16 处**写死的 `ownerId == null ? null : getPlayer(ownerId)` 换成 `actorFor(answerSeat())`
- `pushHand` 拆成 `pushHandSeat(0/1)`，席位 1 只在 `p2PlayerId != null` 时推
- 两边挑同一副卡组时导出会互相覆盖 → 给 p2 那份加 `_p2` 后缀

> ⚠ 上面这三步**只是把界面推给了 p2**，还不够。宿主在 Rush 模式下会替席位 1 自动应答，
> 所以 p2 其实一个提示都收不到。真正的开关是下一节的 `humanSeat1`。

### 10.5 `answerSeat()` 为什么会发错人、现在怎么定

原先的优先级是「`field.promptSeat()` → `host.turnPlayer()`」，错在两点：

- `field.promptSeat()` 只在阶段提示里被 `setPhasePrompt` 设过，选卡 / 连锁类提示压根没有它；
- 兜底的 `turnPlayer()` 是**回合玩家**，而连锁、效果确认这类窗口问的是**被问的那个人**。
  对方回合我方要应答时必然发错。

现在的优先级：

1. **宿主每条提示自带的 `player` 字段**（`DuelHostProcess.promptPlayer()`）——权威来源，
   实测 546,569 条提示 0 条缺失，master 模式关 AI 时取到过 0 和 1 两种值；
2. `field.promptSeat()`（兜底）；
3. `host.turnPlayer()`（再兜底）。

外加一条保险：**只有那个席位真坐着真人才认它**。席位 1 没人（人机局）就退回主人，
否则提示会发给一个不存在的玩家，整局卡死。

### 10.6 ★ 宿主在 Rush 下自动代答席位 1（真人对真人的真正开关）

`work\mdpro-repo\tools\duel_host\Program.cs` 里有这么一条：

```csharp
if (_rushMode && !_auto && ReadAskedPlayer(messageBuffer, messageLength) == 1)
{
    AutoAnswer(messageBuffer, messageLength);          // 席位 1 一律代答
    Emit(new { type = "ai", seat = 1, strategy = "rush-basic" });
    continue;
}
```

它在"找 WindBot 应答"之前就把席位 1 吃掉了，而且**跟 `ai=false` 无关**。
所以只要规则是 Rush，坐在 p2 的人永远不会被问到任何事——
他只能看着自己被对面打完。这也是 Rush 模式下 WindBot 根本不会被创建的原因
（构造函数里是 `if (aiOptions != null && !_rushMode)`）。

修法：加一个 `humanSeat1` 开关，只有远半场确实是真人才跳过这条代答。

```csharp
var humanSeat1 = command.TryGetProperty("humanSeat1", out var humanProp)
                 && humanProp.ValueKind == JsonValueKind.True;
// … 传给 DuelCore 构造函数 …
if (_rushMode && !_auto && _rushBasicAi && ReadAskedPlayer(...) == 1) { … }
```

`_rushBasicAi` 只在 `aiOptions` 真正包含席位 1 人机时为 true。因此双真人局即使上层
`humanSeat1` 识别失败，也不会落入 `rush-basic`；真正人机局仍正常代答。

Java 侧：`DuelHostProcess.setHumanSeat1(boolean)` → 开局命令带 `"humanSeat1"`；
`DuelSessionManager.startForField` 只在 `seat2Player != null`（远半场坐了真人）时传 `true`。

**验证（三条都跑了）**：

| 场景 | 结果 |
|---|---|
| 无 AI、`humanSeat1` 缺省 / false（最终 dll） | seed 12345/777 → **90 / 62** 条提示，席位出现 `[0, 1]`，`player` 字段 0 缺失 |
| 有 AI、`humanSeat1=false`（最终 dll） | seed 12345 → **42** 条提示，席位只有 `[0]`，证明人机局未被破坏 |
| 出厂 exe（对照组） | 84 / 67，席位只出现 `[0]` |

也就是说**默认行为一字节都没动**，只有显式要求"席位 1 是真人"时才改。

### 10.7 ★ 宿主不在 Gradle 里：改了必须重建 + 注入 + 同步 manifest

`duel_host.dll` 是 C# 产物，跟 Java 构建完全无关。改完 `Program.cs` 后：

1. `dotnet build -c Release`（用 `work\dotnet-sdk`）
2. 跑 `python slim_bundle.py`——它现在会把新 dll 写进 `data/mdpromod/bundle/duelhost/duel_host.dll`
   并**同步 manifest 里那一行的字节数**（`slim_bundle.py` 里的 `HOST_DLL` 常量）
3. 顺手把实例 `<实例>\mdpromod\duelhost\duel_host.dll` 也换掉，
   否则当前这次解包出来的仍是旧的（下次解包才会更新）

不改 manifest 会让 `ModBundle` 因为字节数对不上而解包失败——**这是最容易漏的一步**。

### 10.8 其他注意事项

- **`ModBundle` 靠 `manifest.txt` 逐条解包**，格式 `相对路径<TAB>字节数`。
  任何改动 bundle 内容的操作都必须同步重写 manifest，漏一行或多一行都会让解包失败。
- **bundle 由仓库外流水线注入**，不经过 Gradle；`build.gradle` 里没有任何相关配置。
- 资源侧改动后必须 `clean build`（见 §8.1）。
- **改宿主源码后必须重建 + 注入 + 同步 manifest**（见 §10.7）。
- **`DuelSessionManager.start` 现在有 7 个参数的重载**（末位是 `humanSeat1`）。
  别再加新的 6 参重载：`DuelCommands:196` 那个 `(…, false, null, ruleMode)` 调用会被
  `(AiProfile, boolean)` 之类的签名抢走，编译报"DuelRuleMode 无法转换为 boolean"。
- 实例 `<实例>\mdpromod\` 下残留旧解包的 `art`(236M)/`closeup`(103M)/`runtime`(43M)，
  共约 382MB，新版不会再读取。**未擅自删除**（属用户游戏目录）。

---

## 11. 构建、运行与测试命令

### 11.1 构建（工作目录 `work\mdpromod-rush-fabric`）

```bash
# 日常构建（依赖缓存、临时目录、项目构建缓存都在 D 盘）
$env:GRADLE_USER_HOME='D:\codexdate\2026-10-30\zhe\work\gradle-home'
$env:TEMP="$env:GRADLE_USER_HOME\.tmp"
$env:TMP=$env:TEMP
& 'D:\gradle-home\wrapper\dists\gradle-8.12.1-bin\830jys8xkckz69cgvf9t0ocan\gradle-8.12.1\bin\gradle.bat' build

# 改了资源（尤其是删除）后必须用这个
& '...\gradle.bat' clean build
```

产物：`build\libs\mdpromod-0.1.0-fabric.jar`

跑回归网（45 条断言）：

```bash
& '...\gradle.bat' test
```

### 11.1b 重建宿主（只在改了 `Program.cs` / `WindBotAi.cs` 时才需要）

```bash
cd work\mdpro-repo\tools\duel_host
DOTNET_ROOT=".../work/dotnet-sdk" ".../work/dotnet-sdk/dotnet.exe" build -c Release --no-restore
```

产物 `bin\Release\net8.0\duel_host.dll`。**之后必须重跑 `slim_bundle.py`**（见 §10.7）。

### 11.2 打包成品（裁剪 + 换 class + 注入宿主 + 重写 manifest 一步完成）

```bash
cd work\mdpromod-rush-fabric
python slim_bundle.py
```

产物：`work\mdpromod-rush-fabric\full\mdpromod-0.1.0-fabric-full.jar`

然后手动复制到 `outputs\mdpromod-rush-fabric-0.1.0\` 和实例 `mods\`，
并用 `sha256sum` 核对三处一致。

### 11.3 校验

```bash
'C:\Program Files\Java\jdk-21.0.11\bin\jar.exe' --validate --file <jar>   # 无输出即通过
sha256sum <jar>
```

### 11.4 无头 Rush 对局测试

```bash
cd work
MDPRO_DOTNET=".../work/dotnet-sdk/dotnet.exe" \
MDPRO_HOST_DLL=".../work/mdpro-repo/tools/duel_host/bin/Release/net8.0/duel_host.dll" \
MDPRO_RUSH_ROOT="<实例>/mdpromod/rush-runtime" \
MDPRO_RUSH_DECK=".../work/rush-invest/RDRushDemo.ydk" \
MDPRO_RUSH_SEED=12345 \
python test_rush_host.py
```

断言：`"passed": true`、`luaErrors` 为空、`retries` == 0、单回合通常召唤 ≤3。

**两个坑**：

- 卡组必须是 **Rush** 卡组（带 `RD` 前缀那几副，卡号 12xxxxxxx）。
  用 `RushDemo.ydk`（大师规则卡组）会得到 80 条 `initial_effect` 报错，那不是运行时坏了。
- 该脚本自动应答很有限，遇到 `AnnounceCard`、`SelectTribute` 等 prompt 会直接报错中断
  （**不是卡组的问题**）。效果卡 / Maximum 卡组跑到中盘经常触发，属脚本限制。

### 11.4b 席位探测（本轮新增，用来验证「谁在被问」）

```bash
cd work
# 出厂宿主 / Rush / 关 AI：席位 1 永不出现（= 代答 bug 的证据）
python probe_prompt_seat.py

# 打开 humanSeat1：席位 1 的提示开始外发
MDPRO_PROBE_HUMAN_SEAT1=1 python probe_prompt_seat.py

# master 模式关 AI：验证 player 字段确实会取 0 和 1
MDPRO_PROBE_MODE=master MDPRO_PROBE_AI=0 python probe_prompt_seat.py
```

环境变量：`MDPRO_HOST`（宿主 exe）、`MDPRO_DOTNET_ROOT`、`MDPRO_RUSH_DECK`、
`MDPRO_PROBE_SEEDS`、`MDPRO_PROBE_AI`、`MDPRO_PROBE_HUMAN_SEAT1`、`MDPRO_PROBE_AGGRESSIVE`。

### 11.4c RD 机制脚本层探测（本轮新增）

```bash
cd work
python probe_rd_mechanics.py          # auto 模式，验证卡组能跑完且 0 Lua 错误
MDPRO_DECK=RDMaximumDemo MAX=25 python probe_rd_prompts.py   # 打印提示序列，看它在问什么
```

### 11.5 游戏内使用

```text
/mdpromod case give <卡组名>     # 需要 OP / 开作弊
```

卡组目录 `<实例>\config\mdpromod\decks\`，**运行时读盘，放进去立刻生效，不用重启**。
文件名里的空格**必须换成 `_`**（`case give` 参数是 `StringArgumentType.word()`）。
清单见 `<实例>\config\mdpromod\decks\_卡组命令对照.txt`。

---

## 12. 需要保留的兼容性约束

1. **modid 必须是 `mdpromod`**——改了就要迁移 104 个文件的包名 + 整棵资源树，且旧存档失效
2. **`DuelRuleMode.MASTER` 枚举值不能删**——旧存档 NBT 里存的是 0，
   `byOrdinal` 会把它折算成 RUSH；删掉会让 `ordinal()` 漂移
3. **`data\mdpromod\bundle\` 的路径不能改**——`ModBundle.RESOURCE_ROOT` 硬编码，
   bundle 由仓库外流水线按这个路径注入
4. **Rush 落点必须保持恒等映射**——`DuelFieldLayout` 顶部注释写了禁止再引入 ±1 偏移
5. **`manifest.txt` 格式不能变**（`相对路径<TAB>字节数`），且必须与包内条目严格一一对应。
   换 `duel_host.dll` 时字节数必须跟着改，否则 `ModBundle` 解包失败（见 §10.7）
6. **卡池隔离**：大师卡与 Rush 卡卡号交集为 0，但混合卡组会被 `DeckRules` 拒绝；
   RD 版已删除大师卡库，不要再引入
7. **Rush 额外卡组**：融合 / 同调 / 超量 / 连接怪兽按 `isExtraDeckInRush()` 进入额外卡组，
   不要只看 `isExtraDeck()` 的大师规则语义
8. **`duel_host` 是两种规则共用一份 exe**，切换只靠 `--root` + `mode` 字段，不要拆成两份
9. **物品 / 方块 / 实体 ID 不能改**（`duel_field`、`card_case`、`duel_card` 等），改了旧存档失效
10. **Rush 卡图靠外部 MDPro3 `.ypk`**（用户确认保留），不要假设卡图在包内
11. **双真人是否代答最终以宿主是否真的存在 Rush 人机为准**（`_rushBasicAi`）。
    `humanSeat1` 仍保留为席位识别提示，但不能单独决定关闭/开启代答；只按它判断会再次出现
    “p2 被静默代答”。最终版本已用无 AI 且 `humanSeat1=false` 的场景钉住。
12. **额外卡组记账必须按席位各一份**——`ExtraDeckTracker` 的 `seat` 字段不能退回写死 0，
    `DuelSessionManager` 的 `extraTrackers` / `extraBases` 都是长度 2 的数组
13. **`answerSeat()` 的第一优先级必须是宿主的 `player` 字段**——退回
    `turnPlayer()` 会让对方回合的应答发错人（已修，见 §10.5）

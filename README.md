# Diamond HUD

> 在血量条和饥饿条之间画一个箭头，指向你选中的最近的矿石，并标出距离。

Minecraft **1.21.11** 客户端模组，**Fabric** 和 **NeoForge** 两个版本。MIT 许可。

---

## 三个功能

### 1. 矿石指示箭头

```
        ❤❤❤❤❤❤❤❤❤❤   🍗🍗🍗🍗🍗🍗🍗🍗🍗🍗
              ▲ 18m          ← 箭头画在两条状态条中间的空隙里
```

- **11 种矿石单选** —— 一次只找一种，要挖什么就选什么（见下表）
- **上方标出距离**（格）
- **十六个方向共用一张图** —— 通过旋转一帧「向上」的贴图得到，所以斜向和正向是同一套美术
- **目标在正上/正下方时** —— 箭头锁定该轴并循环播放动画，让「就在这儿往下挖」一眼可读
- 箭头下面还有一行字（`右转 100° 低头 29°` / `已对准`），因为箭头在背后时说不清该往哪转
- **找不到矿时朝正下方指**，深度按你勾的矿石里最深的那个算

支持：煤矿 · 铜矿 · 铁矿 · 金矿 · 红石 · 青金石 · 钻石 · 绿宝石 · 下界石英 · 下界金 · 远古残骸
（每种自动包含普通和深层变种，不用勾两次）

### 2. 设置界面

**按 `J`，或者手里拿着指南针右键** 打开：

```
┌────────────────────────────────────────────────┐
│ ◆ Diamond HUD   开关 H · 设置 J · 也可右键指南针   │
│                                                │
│            指示箭头：开 / 关                     │
│  [半径 -]    搜索半径 20 格     [半径 +]         │
│            选择矿石（当前：钻石）                 │
│                    [关闭]                      │
│  找：钻石（扫不到时朝下指 Y=-59）                 │
└────────────────────────────────────────────────┘
```

点「选择矿石」进第二级列表 —— 单独一层是为了 UI 不出错位：十一个按钮挤在主面板上，
GUI 缩放一小就会和底部的行叠在一起。

```
┌────────────────────────────────────────────────┐
│ ◆ 选择矿石   点一行选中 · 键位在 选项→控制 里设     │
│                                                │
│  ☐ 煤矿                            未设置        │
│  ☐ 铜矿                            未设置        │
│  ☐ 铁矿                            未设置        │
│  ● 钻石   ← 绿色横条                            │
│  ☐ 绿宝石                          未设置        │
│  … 共 11 行                                    │
│                                                │
│  当前：钻石                                     │
│  [重置为钻石] [返回]                            │
└────────────────────────────────────────────────┘
```

**单选，不是多选。** 多选时「最近的矿石」会变成一个说不清的东西：箭头会在几个目标之间
来回跳，为铁矿记下的坐标会被拿去判断钻石，朝下指的深度也得在几个矿层里挑一个。
一次只找一种，上面每一件事都变得没有歧义。

所有改动立刻写进 `config/diamondhud.txt`，没有确定/取消按钮。

### 3. 指南针面板

主手或副手拿着 **指南针** 时，屏幕角落浮出一个 Create 风格的面板（模仿工程师护目镜的样式），显示坐标和朝向。

判断只按物品 ID —— **附魔、改名、NBT 都不影响**，任何指南针都算。

---

## 快捷键

| 键 | 作用 | 默认 |
|---|---|---|
| `H` | 开关箭头 | 已绑 |
| `J` | 打开设置界面 | 已绑 |
| `指向·煤矿` … `指向·远古残骸` | 把目标切成那一种矿石 | **全部不绑** |

每种矿石一个独立的键，默认**故意不绑**：再塞 11 个默认键进去，在任何一个真实的模组包里
都会撞车，而开关和设置界面已经把常见需求覆盖了。想给某种矿石绑键就去
**选项 → 控制 → 按键绑定**，找「指向·铁矿」这类条目自己设，按一下目标就切过去。

判断按键用的是 `isDown()` 的边沿检测，不是 `consumeClick()`：后者是个计数器，
任何让 `KeyMapping.click()` 对一次物理按下被调用两次的东西，都会让开关「啪」地关掉又打开，
而消费循环分辨不出这和真的按了两次有什么区别。

---

## 安装

| | 需要 | 版本 |
|---|---|---|
| **Fabric** | Minecraft | 1.21.11 |
| | Fabric Loader | **0.19.3** 或更新 |
| | Fabric API | 必须装，选 `+1.21.11` 的那个构建 |
| **NeoForge** | Minecraft | 1.21.11 |
| | NeoForge | **21.11.42** 或更新 |
| 两者都要 | Java | 21 或更高 |

把 jar 放进 mods 文件夹：

```
%appdata%\.minecraft\mods\                 普通模式
<游戏目录>\versions\<版本名>\mods\            版本隔离模式（PCL2 / HMCL）
```

⚠️ **jar 不要解压**，直接放。两个加载器**只能装一个**。**纯客户端模组**，服务器不用装。

### 下载

到 [Releases](../../releases) 取最新的：

| 文件 | 加载器 |
|---|---|
| `diamondhud-0.6.1-Fabric-1.21.11.jar` | Fabric |
| `diamondhud-0.6.1-NeoForge-1.21.11.jar` | NeoForge |

---

## 配置

设置界面能改的都会写进 `config/diamondhud.txt`，纯文本，可以直接手改：

```properties
enabled=true
radius=20
ore=diamond
```

`ore=` 填 `coal` / `copper` / `iron` / `gold` / `redstone` / `lapis` / `diamond` /
`emerald` / `quartz` / `nether_gold` / `debris` 之一。

游戏里改设置会覆盖这个文件；手改文件后重启游戏生效。
0.6.0 写下的 `ores=a,b` 会被读成其中第一种并自动改写成 `ore=a`，不用手改。

其余用 JVM 启动参数覆盖：

```properties
-Ddiamondhud.target=minecraft:stone          临时指向任意方块，测试用（覆盖上面的勾选）
-Ddiamondhud.radius=32                       只想改半径、不动配置文件时用
-Ddiamondhud.arrow=absolute                  箭头改成绝对方位（默认 relative）
-Ddiamondhud.pixel=2                         箭头放大倍数，默认 1
-Ddiamondhud.hint=false                      找不到矿时不要朝下指，直接不画
-Ddiamondhud.debug=true                      每秒在日志打一行决策数据
```

`-Ddiamondhud.debug=true` 打出来长这样，方向不对时可以一眼看出错在哪一环：

```
arrow radius=20 targets=[minecraft:diamond_ore,minecraft:deepslate_diamond_ore]
      tgt=-33,-60,6 off=(-6.2,-1.4,-0.2) dist=6.3 at=(-26.3,-59.8,6.7)
      yaw=98.4 pitch=31.1 absolute=-88.4 view=278.4 relative=-366.8 drawn=-366.8 frame=0
```

> ⚠️ **关于默认半径 20**：站在地表时，这个立方体范围**够不到钻石真正生成的深层**。所以这个箭头是**挖矿或探洞时看的**，不是在地面上闲逛时用的。要在地表用就把半径调大（现在设置界面里就能调，代价是扫描变慢）。

### 关于「记忆」

模组只扫描「半径 N 的立方体」，但扫到的矿会**记进内存（最多 96 个）**，
箭头指的是**记忆里最近的那一个**，而不是这一帧扫到的。

所以：

- 走过一片矿区再走开，箭头会一直指着那边 —— 这是设计如此，它记得
- 换矿石时这份记忆会被清空，因为那些坐标是按旧的目标收集的

### 找不到矿时的提示深度

| 矿石 | 提示深度 | 矿石 | 提示深度 |
|---|---|---|---|
| 钻石 / 红石 | -59 | 铜矿 | 48 |
| 金矿 | -16 | 煤矿 | 96 |
| 铁矿 | 16 | 绿宝石 | 200（山地） |
| 青金石 | 0 | 下界石英 / 下界金 / 远古残骸 | 不提示 |

最后三种只在下面挖，「往下」没有意义，所以选它们时不画提示。

---

## 关于贴图来源

箭头贴图 **`resources/assets/diamondhud/textures/gui/placement_indicator.png`** 是 Create 模组

```
create-1.20.1-0.5.1.j.jar → assets/create/textures/gui/placement_indicator.png
```

的**逐字节副本**，没有重绘也没有调色。

它是 **16×256** —— 也就是 **16 帧 16×16 的箭头**，每帧一个旋转角度（Create 画放置指示器时就是循环这 16 帧）。所以这里的逻辑只是**挑出角度匹配的那一帧**：没有程序化旋转、没有像素表，美术保持原样。

帧的方向是**量出来的**，不是猜的：把第 0 帧旋转 90° 后与各帧逐像素比对，第 4 帧与「顺时针 90°」吻合度 0.71、与「逆时针 90°」0.63，第 8 帧与「转 180°」完全一致。所以：

```
帧号 0 = 上    4 = 右    8 = 下    12 = 左    每步顺时针 22.5°
```

Create 是 MIT 许可，所以可以这样复用，但**在此明确署名**。如果你要基于本项目再分发，请一并保留这段说明。

指南针面板的配色也是从 Create 的 `foundation/gui/Theme.class` 里读出来的：

```
面板底色 #F0100010   发光边 #DD8AB6D6   分隔线 #FF9ABBD3
正文    #FFEEEEEE   次要文字 #FFA3A3A3   强调色 #FF8AB6D6
状态    绿 #CC20CC20   红 #CCCC2020
```

那个面板的**布局是重构的** —— Create 完全用代码绘制它、不附带贴图，所以几何形状是照着观感做的，不是照搬的。

---

## 仓库结构

两个加载器共用大部分代码，只有入口、HUD 注册和按键注册方式不同：

```
common/     Arrows  Digits  GogglesPanel  OreTargets          ← 加载器无关
            DiamondHudConfig  DiamondHudKeys
            DiamondHudSettingsScreen  OreSelectScreen
fabric/     DiamondHudMod.java  DiamondHudLayer.java  fabric.mod.json
neoforge/   DiamondHudMod.java  DiamondHudLayer.java  META-INF/neoforge.mods.toml
resources/  logo.png + 贴图 + 按键名的语言文件
tools/      不用 Gradle 的构建链（见 BUILDING.md）
```

| | NeoForge | Fabric |
|---|---|---|
| 入口 | `@Mod(value=..., dist=Dist.CLIENT)` | `implements ClientModInitializer` |
| HUD 注册 | `RegisterGuiLayersEvent.registerAbove(...)` | `HudElementRegistry.addLast(...)` |
| 图层接口 | `implements GuiLayer` | `implements HudElement` |
| 按键注册 | `RegisterKeyMappingsEvent.register(...)` | `KeyBindingHelper.registerKeyBinding(...)` |
| 指南针右键 | `InputEvent.InteractionKeyMappingTriggered` + `setCanceled(true)` | 客户端 tick 轮询使用键 |

`render(GuiGraphics, DeltaTracker)` 的签名两边一样，所以图层逻辑一个字都没改；
按键对象和背后的逻辑都放在 `common/DiamondHudKeys`，只有「注册」这一步分叉。
NeoForge 那份 `DiamondHudLayer.java` 是由 Fabric 那份替换接口名生成的，
避免手抄两份之后跑偏。

---

## 从源码编译

**不用 Gradle，也不用 Loom / ForgeGradle** —— 直接对着原版 jar 用 `javac`，再自己重映射到 Fabric 的 intermediary 命名。完整做法和踩过的坑见 **[BUILDING.md](BUILDING.md)**。

一句话版：

```powershell
# Fabric：javac 编译（对着 Mojang 名的 jar）→ 两遍重映射 → 打包
tools\rebuild-fabric.ps1
tools\install-fabric.ps1
```

那条重映射链是必需的：Fabric 运行期加载的是 **intermediary** 名（`class_2960`），
而你写代码用的是 **Mojang** 名（`Identifier`）。NeoForge 跑的就是 Mojang 名，所以不需要。

---

## 已验证 / 未验证

**已实测通过**（1.21.11 Fabric，进了游戏逐项点过）：
箭头渲染、距离数字、方位提示、设置界面、矿石二级单选列表、逐行切换矿石、
旧配置文件 `ores=` 自动改写、配置写盘与重读、`J` 打开设置、`H` 开关箭头、
`-Ddiamondhud.debug` 的决策日志。

**未实测**：NeoForge 版能编译、能打包，但没在 NeoForge 里跑过（这台机器上的 NeoForge 实例没装这个模组）。

---

## 相关

- [dshmusic](https://github.com/Stone-Sprite/dshmusic) —— 同作者，手里拿木棍就能放歌的音乐播放器，同样支持 NeoForge 1.21.11 和 Fabric 1.21.11。

---

## 许可

MIT，见 [LICENSE](LICENSE)。

其中的箭头贴图来自 Create 模组（同样 MIT），已在上文署名。

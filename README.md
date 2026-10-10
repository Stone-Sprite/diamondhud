# Diamond HUD

> 在血量条和饥饿条之间画一个箭头，指向最近的钻石矿，并标出距离。

Minecraft **1.21.11** 客户端模组，**Fabric** 和 **NeoForge** 两个版本。MIT 许可。

---

## 两个功能

### 1. 钻石指示箭头

```
        ❤❤❤❤❤❤❤❤❤❤   🍗🍗🍗🍗🍗🍗🍗🍗🍗🍗
              ▲ 18m          ← 箭头画在两条状态条中间的空隙里
```

- **指向最近的钻石矿** —— 钻石矿和深层钻石矿都算
- **上方标出距离**（格）
- **十六个方向共用一张图** —— 通过旋转一帧「向上」的贴图得到，所以斜向和正向是同一套美术
- **目标在正上/正下方时** —— 箭头锁定该轴并循环播放动画，让「就在这儿往下挖」一眼可读
- 箭头下面还有一行字（`右转 100° 低头 29°` / `已对准`），因为箭头在背后时说不清该往哪转

### 2. 指南针面板

主手或副手拿着 **指南针** 时，屏幕角落浮出一个 Create 风格的面板（模仿工程师护目镜的样式），显示坐标和朝向。

判断只按物品 ID —— **附魔、改名、NBT 都不影响**，任何指南针都算。

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

⚠️ **jar 不要解压**，直接放。**纯客户端模组**，服务器和同房间的其他玩家都不需要装。

### 下载

到 [Releases](../../releases) 取最新的：

| 文件 | 加载器 |
|---|---|
| `diamondhud-0.5.0-Fabric-1.21.11.jar` | Fabric |
| `diamondhud-0.4.5-NeoForge-1.21.11.jar` | NeoForge |

---

## 配置

全部用 JVM 启动参数覆盖，不加就是默认值：

```properties
# 搜索半径（方块），默认 20
-Ddiamondhud.radius=32

# 指向什么方块，默认钻石矿和深层钻石矿，逗号分隔
-Ddiamondhud.target=minecraft:emerald_ore,minecraft:deepslate_emerald_ore

# 箭头方位：相对（默认，屏幕上方 = 你正前方）还是绝对（永远朝北，像指南针）
-Ddiamondhud.arrow=absolute

# 箭头放大倍数，GUI 缩放下嫌小就调它，默认 1
-Ddiamondhud.pixel=2

# 附近没矿时不要朝下指，直接什么都不画
-Ddiamondhud.hint=false

# Fabric 版专用：每秒在日志里打一行决策数据
-Ddiamondhud.debug=true
```

> ⚠️ **关于默认半径 20**：站在地表时，这个立方体范围**够不到钻石真正生成的深层**。所以这个箭头是**挖矿或探洞时看的**，不是在地面上闲逛时用的。要在地表用就把半径调大（代价是扫描变慢）。

**找不到矿时它不会什么都不画** —— 它会朝正下方指，提示「钻石在下面」。所以它的读法是两个：

```
箭头朝上/斜着  + 距离   →  附近（或记忆里）有矿，按箭头走
箭头朝正下      + 距离   →  没扫到矿，往下挖
```

如果你已经在 Y = -57 以下，它就不画了 —— 已经到钻石层，再提示「往下」没有意义。

### 关于「记忆」

模组只扫描「半径 20 的立方体」，但扫到的矿会**记进内存（最多 96 个）**，
箭头指的是**记忆里最近的那一个**，而不是这一帧扫到的。

所以：

- 走过一片矿区再走开，箭头会一直指着那边 —— 这是设计如此，它记得
- 反过来，用 `-Ddiamondhud.target` 指向石头这种随处可见的方块时，96 个名额会被
  石头瞬间占满，箭头就会去指旁边一块石头。**目标别选太常见的方块。**

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

两个加载器共用大部分代码，只有入口和 HUD 注册方式不同：

```
common/     Arrows.java  Digits.java  GogglesPanel.java     ← 加载器无关
fabric/     DiamondHudMod.java  DiamondHudLayer.java  fabric.mod.json
neoforge/   DiamondHudMod.java  DiamondHudLayer.java  META-INF/neoforge.mods.toml
resources/  logo.png + 贴图
tools/      不用 Gradle 的构建链（见 BUILDING.md）
builds/     已构建好的 jar
```

同一个文件在两个加载器下的差别只有一行接口：

| | NeoForge | Fabric |
|---|---|---|
| 入口 | `@Mod(value=..., dist=Dist.CLIENT)` | `implements ClientModInitializer` |
| HUD 注册 | `RegisterGuiLayersEvent.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, ...)` | `HudElementRegistry.addLast(...)` |
| 图层接口 | `implements GuiLayer` | `implements HudElement` |

`render(GuiGraphics, DeltaTracker)` 的签名两边一样，所以图层逻辑一个字都没改。

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
而你写代码用的是 **Mojang** 名（`Identifier`）。Loom 平时干的就是这件事。

---

## 相关

- [dshmusic](https://github.com/Stone-Sprite/dshmusic) —— 同作者，手里拿木棍就能放歌的音乐播放器，同样支持 NeoForge 1.21.11 和 Fabric。

---

## 许可

MIT，见 [LICENSE](LICENSE)。

其中的箭头贴图来自 Create 模组（同样 MIT），已在上文署名。

# Diamond HUD

> 在血量条和饥饿条之间画一个箭头，指向最近的钻石矿，并标出距离。

NeoForge 1.21.11 客户端模组。MIT 许可。

---

## 两个功能

### 1. 钻石指示箭头

```
        ❤❤❤❤❤❤❤❤❤❤   🍗🍗🍗🍗🍗🍗🍗🍗🍗🍗
              ▲ 18m          ← 箭头画在两条状态条中间的空隙里
```

- **指向最近的钻石矿** —— 钻石矿和深层钻石矿都算
- **上方标出距离**（米）
- **八个方向共用一张图** —— 通过旋转一帧「向上」的贴图得到，所以斜向和正向是同一套美术
- **目标在正上/正下方时** —— 箭头锁定该轴并循环播放动画，让「就在这儿往下挖」一眼可读

### 2. 指南针面板

主手或副手拿着 **指南针** 时，屏幕角落浮出一个 Create 风格的面板（模仿工程师护目镜的样式），显示坐标和朝向。

判断只按物品 ID —— **附魔、改名、NBT 都不影响**，任何指南针都算。

---

## 安装

| 需要 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| NeoForge | 21.11.42 或更新 |

把 jar 放进 `.minecraft\versions\1.21.11\mods\` 即可。纯客户端模组，服务器不需要装。

---

## 配置

两个都可以用启动参数覆盖：

```properties
# 搜索半径（方块），默认 20
-Ddiamondhud.radius=32

# 指向什么方块，默认钻石矿和深层钻石矿
-Ddiamondhud.target=minecraft:emerald_ore,minecraft:deepslate_emerald_ore
```

> ⚠️ **关于默认半径 20**：站在地表时，这个立方体范围**够不到钻石真正生成的深层**。所以这个箭头是**挖矿或探洞时看的**，不是在地面上闲逛时用的。要在地表用就把半径调大（代价是扫描变慢）。

`-Ddiamondhud.target` 也可以拿来**冒烟测试渲染** —— 指向石头之类随手可见的方块，不用真去找钻石。

---

## 关于贴图来源

箭头贴图 **`assets/diamondhud/textures/gui/placement_indicator.png`** 是 Create 模组

```
create-1.20.1-0.5.1.j.jar → assets/create/textures/gui/placement_indicator.png
```

的**逐字节副本**，没有重绘也没有调色。

它是 **16×256** —— 也就是 **16 帧 16×16 的箭头**，每帧一个旋转角度（Create 画放置指示器时就是循环这 16 帧）。所以这里的逻辑只是**挑出角度匹配的那一帧**：没有程序化旋转、没有像素表，美术保持原样。

Create 是 MIT 许可，所以可以这样复用，但**在此明确署名**。如果你要基于本项目再分发，请一并保留这段说明。

指南针面板的配色也是从 Create 的 `foundation/gui/Theme.class` 里读出来的：

```
面板底色 #F0100010   发光边 #DD8AB6D6   分隔线 #FF9ABBD3
正文    #FFEEEEEE   次要文字 #FFA3A3A3   强调色 #FF8AB6D6
状态    绿 #CC20CC20   红 #CCCC2020
```

那个面板的**布局是重构的** —— Create 完全用代码绘制它、不附带贴图，所以几何形状是照着观感做的，不是照搬的。

---

## 从源码编译

和同作者的 [dshmusic](https://github.com/Stone-Sprite/dshmusic) 一样，**不用 Gradle 也不用 ForgeGradle**：直接对着原版 jar 用 `javac` 编译。做法和几个坑见 [dshmusic 的 BUILDING.md](https://github.com/Stone-Sprite/dshmusic/blob/main/BUILDING.md)（同一个套路）。

要点：

```powershell
# 从版本 json 读运行期库，NeoForge 的 jar 只取 21.11.42 这一代
& "$env:APPDATA\.minecraft\runtime\java-runtime-delta\bin\javac.exe" "@build\javac.args"
```

**三个必须注意的点**（我在 dshmusic 里都踩过）：

1. **`@argfile` 里 `\` 是转义字符** —— 路径必须用正斜杠，否则 `D:\.minecraft\...` 会被吃成 `D:.minecraft...`
2. **classpath 上旧版本的库不能排在前面** —— javac 取第一个匹配的类，编译期和运行期版本不一致会埋下 `NoSuchMethodError`
3. **`net/neoforged` 下有多个世代的 jar** —— 不过滤会解析到错误的 API

---

## 许可

MIT，见 [LICENSE](LICENSE)。

其中的箭头贴图来自 Create 模组（同样 MIT），已在上文署名。

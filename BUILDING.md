# 从源码编译

**不用 Gradle，也不用 Loom / ForgeGradle。**

为什么要自己造这条链：机器上跑的是 Minecraft 1.21.11，Loom 要联网下
Maven 依赖和 mappings 才能起步，而这条路只想用磁盘上已经有的东西。
所有输入都是原版和 Fabric 自己装的 jar，一个字节都不用下载。

---

## 一、NeoForge 版

NeoForge 在**生产环境跑的就是 Mojang 名**，所以不需要重映射，编译完就能用。

```powershell
# build\javac.args 里是完整的 classpath + 源码清单
& "$env:APPDATA\.minecraft\runtime\java-runtime-delta\bin\javac.exe" "@build\javac.args"
# 然后把 build\classes 连同 resources 打进 jar
```

### 三个必须注意的点

1. **`@argfile` 里 `\` 是转义字符** —— classpath 必须用**正斜杠**，
   否则 `D:\.minecraft\...` 会被吃成 `D:.minecraft...`，
   javac 报「找不到符号」而你会以为是 classpath 少了 jar。
2. **classpath 上旧版本的库不能排在前面** —— javac 取第一个匹配的类，
   编译期和运行期版本不一致会埋下 `NoSuchMethodError`。
3. **`net/neoforged` 下有多个世代的 jar** —— 不过滤会解析到错误的 API（21.11.42 / 21.11.45 / 26.1.2.95 ...）。

---

## 二、Fabric 版

Fabric 生产环境加载的是 **intermediary** 名。同一行代码：

| | 你写的 | 游戏里跑的 |
|---|---|---|
| 类 | `net.minecraft.resources.Identifier` | `net.minecraft.class_2960` |
| 方法 | `Identifier.fromNamespaceAndPath` | `method_60655` |
| 字段 | `Items.STICK` | `field_8600` |

Loom 平时干的就是这个转换。不用 Loom 就得自己接上。

### 两个命名空间的来源（都是磁盘上现成的）

```
Mojang 的 ProGuard 映射
  D:\.minecraft\libraries\net\minecraft\client\1.21.11\client-1.21.11-mappings.txt
  official → Mojang

Fabric 的 tiny 映射
  D:\.minecraft\libraries\net\fabricmc\intermediary\1.21.11\intermediary-1.21.11.jar
  → mappings/mappings.tiny      official → intermediary

原版混淆 jar
  D:\.minecraft\versions\1.21.11\1.21.11.jar        （第 2 遍的 classpath 要用它）

Fabric 自带的 tiny-remapper
  D:\.minecraft\libraries\net\fabricmc\fabric-loader\0.19.5\fabric-loader-0.19.5.jar
  → net.fabricmc.loader.impl.lib.tinyremapper
```

### 为什么是**两遍**，而不是把两个映射文件合成一张

想省事的话会去合成一张 `Mojang ↔ intermediary` 的表（用 `official` 当公共主键）。
这条路走不通：

```
DataComponentHolder   official `c`  →  intermediary method_57826   （Mojang 名：has）
DataComponentMap      official `c`  →  intermediary method_57832   （Mojang 名：has）
```

两个不同类里的方法，在 **Mojang 命名下同名同描述符**，intermediary 名却不一样。
以「命名名」为源做 `mojang → intermediary`，tiny-remapper 会看到
一个源名对应两个目标，直接抛：

```
java.lang.RuntimeException: Unfixable conflicts
```

**走 `official` 中转就没有歧义**，因为 official 名在每个类里唯一。
而且两个映射文件都成了**原生**文件（源命名空间就是文件里的第一个命名空间），
不需要任何 join，因此一个条目都不丢。

```
第 1 遍   mojang   → official        tools/proguard_to_tiny.py 生成的 mojmap-official.tiny
                                     classpath = Mojang 名的 client jar
第 2 遍   official → intermediary    直接用 Fabric 自带的 mappings.tiny
                                     classpath = 原版混淆 1.21.11.jar
```

### ⚠️ 第 2 遍的 classpath 必须是**源命名空间**的 jar

这一条最坑：给 tiny-remapper 一个 intermediary 名的 client jar 当 classpath，
它解析不到被改名后的类，于是**静默跳过所有成员改名** —— 不报错、不警告，
产出的 jar 里类名换好了、方法名一个没换。进游戏才炸：

```
NoSuchMethodError: 'net.minecraft.class_2960
   net.minecraft.class_2960.fromNamespaceAndPath(java.lang.String, java.lang.String)'
```

### 生成第 1 遍的映射文件

```powershell
python tools\proguard_to_tiny.py `
    "D:\.minecraft\libraries\net\minecraft\client\1.21.11\client-1.21.11-mappings.txt" `
    "D:\dsh-build\mojmap-official.tiny"
# -> tiny 2 0 mojang official    （描述符按规范写在第一个命名空间，即 Mojang 名）
```

### 跑两遍

`tools/RemapTool.java` 是个薄壳，直接用 Fabric Loader 里打包的 tiny-remapper：

```
RemapTool <in.jar> <out.jar> <mappings.tiny> <fromNs> <toNs> [classpath...]
```

```powershell
$rt = "D:\dsh-build\rtool;" +
      "D:\.minecraft\libraries\net\fabricmc\fabric-loader\0.19.5\fabric-loader-0.19.5.jar;" +
      (asm 9.10.1 的 5 个 jar，分号连接)
$java = "$env:APPDATA\.minecraft\runtime\java-runtime-epsilon\bin\java.exe"

# 第 1 遍
& $java -cp $rt RemapTool diamondhud-mojang.jar diamondhud-official.jar `
    D:\dsh-build\mojmap-official.tiny mojang official `
    "D:\.minecraft\libraries\net\neoforged\minecraft-client-patched\21.11.42\minecraft-client-patched-21.11.42.jar"

# 第 2 遍  ← classpath 是原版混淆 jar，不是 intermediary jar
& $java -cp $rt RemapTool diamondhud-official.jar diamondhud-0.5.0-fabric.jar `
    D:\dsh-build\inter\mappings.tiny official intermediary `
    "D:\.minecraft\versions\1.21.11\1.21.11.jar"
```

现成的脚本：`tools/rebuild-fabric.ps1`（编译 + 两遍 + 字节校验）、`tools/install-fabric.ps1`（校验 + 装机）。

### 怎么确认映射真的生效了

**别信编译通过，也别信接口签名。** 读 jar 里的 class 字节：

```powershell
# 把 class 当 ASCII 扫一遍，看剩下的是不是 intermediary 名
$txt = [Text.Encoding]::ASCII.GetString($classBytes)
$txt.Contains('method_60655')          # 有 → 静态方法改名成功
$txt.Contains('net/minecraft/resources/')  # 有 → 还有 Mojang 名残留，映射漏了
```

`rebuild-fabric.ps1` 每次都跑这一步，五个类必须全部 `clean`。

---

## 三、踩过的坑（按发现顺序）

| # | 现象 | 真正的原因 |
|---|---|---|
| 1 | `IOException: missing name columns in line 3` | Tiny v2 的成员行必须带**第一个命名空间**的名字列 |
| 2 | `NullPointerException: logger` | `TinyRemapper.newRemapper(null)` 不收 null；`TrLogger` 只有一个抽象方法，给个 lambda 就行 |
| 3 | 类映射只有 6059 个 | 类名正则 `(\w+)` 漏掉混淆内部类的 `$`（`gir$1`），改成 `([\w$]+)` 后 9686 个 |
| 4 | 成员映射只有 38291 个 | 反向表用了点号而描述符用斜杠，先归一化再查 |
| 5 | javac 报 `net.minecraft.resources.Identifier` 找不到，但它**就在 classpath 上** | 映射文件把 Mojang 类名写成了**点号**，tiny-remapper 照抄进常量池 → class 文件损坏。改成斜杠 |
| 6 | 游戏里 `NoSuchMethodError: class_2960.fromNamespaceAndPath` | 合成映射表的**描述符写成了 intermediary 命名**（Tiny v2 要求第一命名空间），键对不上 → **一个成员都没改名**；同一个生成器还漏写了 FIELD 行 |
| 7 | 换两遍方案后 class 名对了、方法名还是没改 | 第 2 遍的 classpath 用了 intermediary jar，必须是**源命名空间**（official）的 |

> **通用教训**：症状是「类明明在 classpath 上却找不到」或者
> 「方法明明存在却 `NoSuchMethodError`」时，**去读 class 字节**，不要继续猜配置。

---

## 四、不用 Loom 换来了什么

- 不需要联网、不需要 Maven 仓库、不需要 `gradlew`
- 构建时间以秒计（一条 `javac` + 两次 remap）
- 整条链只有 3 个文件：`proguard_to_tiny.py`、`RemapTool.java`、`rebuild-fabric.ps1`

代价：

- `fabric.mod.json` 手写
- 没有 Mixin 的注解处理器（本项目不需要 Mixin）
- 打包要自己写（`rebuild-fabric.ps1` 里有）

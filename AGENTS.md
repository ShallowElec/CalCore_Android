# Calcore - Agent 协作指南

> 本文档面向 AI 编码助手，固化项目背景、技术决策与协作规范。
> 每次进入本项目上下文时，请先阅读本文件。

---

## 1. 项目背景

**Calcore** 是一款面向开发者、CS 教育者、数学研究者的高级可视化计算器。
核心差异化：**每一次用户交互都会实时展开底层原理动画**（从布尔代数、寄存器、内存布局到 AST 求值链路）。

- **Slogan**: "See the light through the machine."
- **平台策略**: 平台无关核心引擎 + 原生 UI 层（Android 为首）。
- **当前阶段**: 菱形穿透 v3.0 修订中。M1-M5 为当前冲刺目标（菱形骨架→△下行→▽上行→架构滤镜→交互抛光）；图形/数学工作台在菱形核心稳定后以 v3.1/v3.2 推进。

---

## 2. 技术基线（已确认，不可随意变更）

### 2.1 Android 工程

| 项 | 值 | 备注 |
|---|---|---|
| `minSdk` | **31** (Android 12) | Material You (Dynamic Color) 的最低要求 |
| `compileSdk` | 36 (Android 16) | 使用最新 API |
| `targetSdk` | 36 | 同上 |
| UI 框架 | **Jetpack Compose** + **Material 3** | 声明式 UI，Material You 原生支持 |
| 语言 | **Kotlin 2.2.10** | 全部业务代码 |
| 构建系统 | Gradle (Kotlin DSL) | `libs.versions.toml` 管理依赖 |
| Compose BOM | `2026.02.01` | 稳定版本 |
| 依赖注入 | **Hilt 2.59.2** + **KSP 2.3.0** | AGP 9 必须使用 Hilt ≥2.59 + KSP2 |
| 注解处理 | **KSP** (Kotlin Symbol Processing) | KAPT 与 AGP 9 built-in Kotlin 不兼容 |

### 2.2 Material You 约束与配色策略

- **Dynamic Color**: API 31+ 原生支持 `dynamicDarkColorScheme()` / `dynamicLightColorScheme()`。
- **三套配色方案**:
  1. **系统动态色 (默认)**: 跟随 Android 系统壁纸取色，完全 Material You 原生体验。
  2. **Calcore 深色 (终端绿)**: 极深色背景 `#0a0a0a` + 科技绿强调色 `#00ff41`，硬核终端/底层美学。
  3. **Calcore 浅色 (浅蓝风)**: 纯白背景 `#FFFFFF` + 浅蓝/蓝色系强调色 `#4FC3F7` / `#2196F3`，用户个人风格方案。
- **主题切换**: 用户在「设置」中可在三套方案间切换；跟随系统明暗仅对「系统动态色」生效，其余两套为固定明暗。
- **语义化颜色**: 严格使用 Material 3 Token 体系（`primary`, `onPrimary`, `surface`, `onSurface`, `inverseSurface` 等），禁止在 UI 控件中硬编码色值。

### 2.3 Hilt + KSP + AGP 9 兼容方案（已验证）

> ⚠️ 这是 Calcore 项目的**关键兼容性记录**，后续任何涉及 Hilt/KSP/AGP 的升级都必须先参考本节。

#### 问题背景
- AGP 9.x 引入 **built-in Kotlin**（默认启用），导致 `kotlin-android` 插件不再需要。
- **KAPT** 插件明确报错：`"not compatible with built-in Kotlin support"`。
- **KSP1** 明确不兼容 AGP 9+（Google/ksp#2615）。
- **Hilt 2.56** 的 Gradle 插件在 AGP 9 下报错：`"Android BaseExtension not found"`（google/dagger#4944）。

#### 最终方案（已验证 BUILD SUCCESSFUL）

| 组件 | 版本 | 备注 |
|---|---|---|
| AGP | 9.2.1 | 保持 |
| Kotlin | 2.2.10 | 保持 |
| Hilt | **2.59.2** | 首个正式支持 AGP 9 的版本 |
| KSP | **2.3.0** | KSP2，独立版本号，不再绑定 Kotlin 版本 |
| 注解处理 | `ksp()` | 替代 `kapt()` |

#### Gradle 配置

```kotlin
// gradle.properties
android.builtInKotlin=false
android.newDsl=false
```

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)   // 必须显式应用
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
}
```

> **注意**: `android.builtInKotlin=false` 和 `android.newDsl=false` 是**临时回退方案**，AGP 10.0 将移除。届时需升级到 KSP/Hilt 的最新适配版本。

#### Kotlin 2.2 注解目标变更

构造函数参数注入需使用 `@param:` 目标：

```kotlin
class ThemeDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
)
```

---

### 2.4 图形与动画

| 场景 | 技术方案 | 理由 |
|---|---|---|
| UI 层动画（按钮、切换、滑块） | Compose Animation API (`animate*AsState`, `AnimatedVisibility`, `Animatable`) | 声明式、与 Compose 生命周期集成 |
| 2D 可视化动画（位格翻转、数据流、链表） | **Compose Canvas** + 自定义 `DrawScope` | 足够处理中等复杂度的 2D 矢量动画；PUSH/POP 滑动、连线生长、光标平滑移动均已验证 |
| 2D 图形渲染（函数图像、坐标系） | **Compose Canvas** + 手动路径/采样 | 与 UI 层无缝集成，支持参数驱动 |
| 数学工作台 Canvas（坐标系/矩阵/ODE） | **Compose Canvas** + `clipToBounds()` + `detectTransformGestures` | 手势支持（平移/缩放/双击回正）+ 裁剪防止溢出 |
| 3D 曲面/流形渲染 | **OpenGL ES 3.2** + `GLSurfaceView` / `TextureView` | 10k+ 顶点、60 FPS 必需脱离主线程 |
| GPU Shader 效果（后处理、LIC） | **AGSL** (API 33+) + `RuntimeShader` | 仅用于增强效果，兜底 GLES fragment shader |
| 高性能离屏渲染 | **Surface** + 独立渲染线程 | 视频导出、复杂场景预渲染 |

> **关键决策**: 3D 渲染不直接使用 Compose Canvas（主线程瓶颈），也不使用尚不成熟的 Compose 3D 预览 API。采用 OpenGL ES 3.2 作为跨版本稳定方案。

---

## 3. 架构模式

### 3.1 整体分层

```
┌─────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)             │
│  - 计算器键盘、表达式输入、参数滑块        │
│  - 导航、设置、历史记录                  │
│  - Material You 主题适配                 │
├─────────────────────────────────────────┤
│  Presentation Layer (ViewModel)         │
│  - UI State 管理 (MVI / Unidirectional) │
│  - 用户输入 → 计算命令转换               │
│  - 动画状态机（时间轴、播放控制）          │
├─────────────────────────────────────────┤
│  Domain Layer (UseCase + Model)         │
│  - 计算模式抽象（标准/科学/程序员/图形）   │
│  - 表达式 AST、数值精度策略               │
│  - 架构抽象模型（RISC-V/ARM64/x86-64）   │
├─────────────────────────────────────────┤
│  Engine Layer (跨平台核心)               │
│  - 高精度 64-bit 计算引擎                │
│  - 表达式解析器 (Lexer/Parser)           │
│  - 图形采样引擎（2D/3D）                 │
│  - 菱形管道脚本生成器 (DiamondEngine)      │
├─────────────────────────────────────────┤
│  Platform Layer (Android 原生)           │
│  - OpenGL ES 渲染器                      │
│  - 文件 I/O、分享、视频编码                │
│  - 系统主题监听 (Dynamic Color)          │
└─────────────────────────────────────────┘
```

### 3.2 状态管理

- **UI State**: 使用 `StateFlow` + `data class` 描述完整 UI 状态，遵循 **MVI 单向数据流**。
- **计算器状态机**: 按键输入 → 表达式构建 → 求值触发 → 结果回显 → 动画播放，每步均为不可变状态快照。
- **图形视口状态**: 相机位置、缩放、旋转角度用 `Animatable` 包裹，支持手势插值。
- **动画时间轴**: 独立 `DiamondViewModel`，管理菱形管道的播放/暂停/ scrubber / 速度倍率，与计算逻辑解耦。

---

## 4. 编码规范

### 4.1 包结构

```
com.cloveriris.calcore
├── app                    # Application / DI (Hilt)
├── ui                     # Compose UI + Theme
│   ├── theme              # Color.kt, Type.kt, Theme.kt (Material You)
│   ├── components         # 可复用 Compose 组件
│   ├── calculator         # 计算器模式屏幕
│   ├── graphing           # 图形模式屏幕
│   ├── diamond            # 菱形穿透舞台（Canvas）
│   └── settings           # 设置屏幕
├── presentation           # ViewModel
├── domain                 # 领域模型、接口
├── engine                 # 计算引擎（Kotlin Multiplatform 预留）
└── platform               # Android 平台实现
    ├── opengl             # GLSurfaceView / Renderer
    ├── video              # 视频编码导出
    └── system             # Dynamic Color 监听、权限
```

### 4.2 Compose 规范

- **State Hoisting**: 所有交互状态必须上提至 ViewModel 或父级 Composable，禁止在叶子组件中持有 `mutableStateOf`。
- **Preview**: 每个 Screen 级 Composable 必须提供 `@Preview`（light + dark）。
- **Modifier 顺序**: `Modifier.fillMaxSize().padding().then(custom)`，遵循官方推荐顺序。
- **重组优化**: 大数据列表用 `LazyColumn` + `key`；动画状态用 `remember { Animatable() }` 而非 `rememberSaveable`。
- **字符串资源**: 所有用户可见文本抽取至 `strings.xml`，支持多语言预留。
- **Canvas 动画性能**: 
  - 使用 `rememberTextMeasurer()` 缓存文本测量器，避免每帧重建。
  - 滑动/生长动画优先使用 `Animatable` 在 `DrawScope` 内插值，而非触发 Compose 重组。
  - 需要裁剪的 Canvas 必须加 `.clipToBounds()`，防止绘制溢出到相邻 UI。
  - 复杂动画组件（如 `AsmCard`、`BitGrid`）使用独立 Canvas Overlay 层，与基础 UI 分离以减少重组范围。

### 4.3 颜色与主题

- **禁止**: 在 Composable 中直接使用 `Color(0xFFxxxxxx)`（除品牌 fallback 定义在 `Color.kt`）。
- **必须**: 使用 `MaterialTheme.colorScheme.xxx` 或自定义 Theme Extension。
- **可视化舞台**: 虽然 PRD 定义了科技绿 `#00ff41` 等色值，但这些属于「内容色」而非「UI 色」，需在可视化组件内部定义常量，不混入主题系统。

---

## 5. 关键设计决策记录 (ADR)

| 日期 | 决策 | 背景 | 备选方案 | 结论 |
|---|---|---|---|---|
| 2026-05-12 | minSdk=31 | Material You 需要 API 31 | 降至 29，放弃 Dynamic Color | 锁定 31 |
| 2026-05-12 | 三套配色方案 | 系统动态色 + 终端绿 + 浅蓝白 | 仅单套品牌色 | 采用三套 |
| 2026-05-12 | Hilt 2.59.2 + KSP 2.3.0 | AGP 9.2.1 与 Hilt 2.56/kapt 不兼容 | 降级 AGP 8.x 或手动 DI | Hilt 2.59.2 + KSP 2.3.0 |
| 2026-05-12 | AGP 9 built-in Kotlin 回退 | KSP/KAPT 均不兼容 AGP 9 built-in Kotlin | 长期方案等 KSP 官方适配 | `android.builtInKotlin=false` + `android.newDsl=false` 临时回退 |
| 2026-05-12 | Jetpack Compose | 声明式 UI，Material 3 一等支持 | XML + View | 采用 Compose |
| 2026-05-12 | OpenGL ES 3.2 | 3D 图形 60 FPS 必需 | Vulkan / AGSL only | GLES 为主，AGSL 增强 |
| 2026-05-12 | MVI 单向流 | 计算器状态复杂，需可预测 | MVVM 双向绑定 | MVI |
| 2026-05-12 | Compose Canvas 2D | 2D 可视化与 UI 同层，降低复杂度 | Skia 原生 / OpenGL 全量 | Canvas 2D + GLES 3D |
| 2026-05-13 | **菱形穿透心智模型** | 需要将底层可视化凝聚为统一的"符号→硅→符号"旅程 | 保留 v2.0 的分散 L1-L8 层级面板 | 采用中心对称菱形网格，△ 下行 + ALU 核心 + ▽ 上行 |
| 2026-05-13 | **严格镜像对称管道** | △ 与 ▽ 必须在动画、状态、时间轴上保持可逆 | 上下独立实现，允许视觉差异 | `DiamondEngine` 成对定义 Action，共用几何参数与缓动函数 |
| 2026-05-13 | **方块语义系统** | 需要统一两种几何元素表达所有层级 | 每层使用不同形状/颜色 | 全局仅使用「绿色实心方块」（数据实体）与「绿色空心方块」（容器/槽位/指针） |
| 2026-05-13 | **架构滤镜（ASM 层）** | 多架构差异应集中在 L3/L3'，上下层保持不动 | 每层都根据架构变化 | 仅 ASM 卡片层应用纹理滤镜与助记符差异，BIN/HEX/ASCII 层数值严格一致 |
| 2026-05-13 | **垂直时间轴 Diamond Scrubber** | 用户需要在任意层级间自由穿梭查看状态 | 水平底部 scrubber | 沿菱形中轴线放置垂直 scrubber，支持在 △ 任意层级与 ▽ 任意层级间拖动 |
| 2026-05-13 | **输入探针与删除逆向瀑布** | 光标移动和退格需要精确追踪并逆向回收已传播方块 | 仅顶层文本变动，可视化不回收 | 为每个输入字符分配 `BlockId`，退格时按 ID 沿原路径精确吸回 |
| 2026-05-12 | 动画速度 200ms/tick | PRD 要求 0.2s 基准，可配置 0.2x~2.0x | 固定 40 步硬编码 | `baseStepIntervalMs=200L` + `playbackSpeed` 乘数（v3.0 扩展为 0.25x~4.0x） |

---

## 6. 里程碑对接 (PRD v2.1 菱形穿透)

| 里程碑 | Android 端重点 | 状态 |
|---|---|---|
| **M1** | 菱形骨架引擎：中心对称菱形网格、方块系统、通道壁、ALU 核心节点 | 🔄 进行中 |
| **M2** | △ 下行链路：ASCII→HEX→ASM→BIN→Stack→ALU 的七层动画与数据流 | ⏳ 待实现 |
| **M3** | ▽ 上行链路：严格的镜像反转链路，实现菱形闭合 | ⏳ 待实现 |
| **M4** | 架构滤镜：X64 / RV64 / ARM64 的 ASM 层纹理切换与指令差异 | ⏳ 待实现 |
| **M5** | 交互抛光：输入探针、时间轴 scrubber、光标同步、删除逆向瀑布、3B1B 级缓动质感 | ⏳ 待实现 |

> **后续规划**：图形基座（2D/3D 坐标系）、线性代数工作台、微积分可视化、ODE/PDE 求解等 v2.0 内容将在菱形穿透核心（M1-M5）稳定后以 **v3.1/v3.2** 继续推进。图形与数学工作台渲染层独立于菱形舞台，不受影响。

---

## 7. 与 AI 协作约定

1. **先设计后编码**: 任何模块修改前，先在此文档或 `docs/架构.md` 中更新设计说明。
2. **最小侵入**: 优先扩展现有组件，而非重写；删除代码需说明替代方案。
3. **性能敏感代码标注**: 任何涉及 `Canvas draw`、`OpenGL render loop`、`Coroutine` 的代码需附性能注释。
4. **主题一致性**: 新增 UI 组件必须明确使用哪个 Material Theme Token，禁止自行发明颜色。
5. **菱形对称性**: 任何涉及 △ 下行的 Action/动画/状态变更，必须同时定义其 ▽ 上行的镜像版本，并在 `DiamondEngine` 中成对注册。
6. **文档同步**: 修改架构后同步更新本文件。

---

*文档版本: v1.2*  
*日期: 2026-05-13*  
*状态: 菱形穿透 v3.0 修订中，M1 进行中，M2-M5 待实现*

# Calcore - Agent 协作指南

> 本文档面向 AI 编码助手，固化项目背景、技术决策与协作规范。
> 每次进入本项目上下文时，请先阅读本文件。

---

## 1. 项目背景

**Calcore** 是一款面向开发者、CS 教育者、数学研究者的高级可视化计算器。
核心差异化：**每一次用户交互都会实时展开底层原理动画**（从布尔代数、寄存器、内存布局到 AST 求值链路）。

- **Slogan**: "See the math. See the machine."
- **平台策略**: 平台无关核心引擎 + 原生 UI 层（Android 为首）。
- **当前阶段**: M1-M7a 已完成实现，M7b-M9 待开发。

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
│  - 动画指令序列生成器                     │
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
- **动画时间轴**: 独立 `AnimationViewModel`，管理播放/暂停/ scrubber / 速度倍率，与计算逻辑解耦。

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
│   ├── visualization      # 可视化舞台（Canvas / OpenGL）
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
  - 复杂动画组件（如 `LinkedListView`）使用独立 Canvas Overlay 层，与基础 UI 分离以减少重组范围。

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
| 2026-05-12 | 可视化引擎 `AnimationScript` | 需要按时间回放、可 scrub 的动画系统 | 每帧直接修改状态 | `TimedAction` + `reduceAction` 累积式状态归约 |
| 2026-05-12 | Architecture 丰富模型 | 多架构切换需要栈方向、SP 名、寻址方式、助记符差异 | 仅寄存器名不同 | 完整 Architecture enum（SP/FP/addressingMode/mnemonic） |
| 2026-05-12 | 动画速度 200ms/tick | PRD 要求 0.2s 基准，可配置 0.2x~2.0x | 固定 40 步硬编码 | `baseStepIntervalMs=200L` + `playbackSpeed` 乘数 |

---

## 6. 里程碑对接 (PRD M1-M9)

| 里程碑 | Android 端重点 | 状态 |
|---|---|---|
| M1 内核 | 引擎 Kotlin 实现（Lexer/Parser/AST/Evaluator、64-bit 计算） | ✅ 完成 |
| M2 骨架 | Canvas 方块系统、连线系统、基础时间轴控件 | ✅ 完成 |
| M3 链路 | 按键 → 内存 → ALU → 结果的 Compose Canvas 动画（L1-L8） | ✅ 完成 |
| M4 硬核 | 程序员模式 + 架构切换 UI + 位运算可视化 + 动画速度控制 | ✅ 完成 |
| M5 图形基座 | 2D 坐标系 Canvas、表达式列表、显函数/参数/极坐标渲染 | ✅ 完成 |
| M6 图形进阶 | OpenGL ES 3D 渲染器、参数曲面、隐函数 | ⏳ M6a 完成（矩阵/微积分骨架），M6b 3D 待实现 |
| M7 计算可视化 | AST 生长动画、采样过程 Canvas 演示、ODE 求解可视化 | ✅ M7a 完成（ODE 工作台），M7b PDE 待实现 |
| M8 参数动画 | 滑块系统、关键帧、视频导出 (MediaCodec) | ⏳ 待实现 |
| M9 抛光 | 性能调优、Material You 动态色精细化、无障碍 | ⏳ 待实现 |

---

## 7. 与 AI 协作约定

1. **先设计后编码**: 任何模块修改前，先在此文档或 `docs/架构.md` 中更新设计说明。
2. **最小侵入**: 优先扩展现有组件，而非重写；删除代码需说明替代方案。
3. **性能敏感代码标注**: 任何涉及 `Canvas draw`、`OpenGL render loop`、`Coroutine` 的代码需附性能注释。
4. **主题一致性**: 新增 UI 组件必须明确使用哪个 Material Theme Token，禁止自行发明颜色。
5. **文档同步**: 修改架构后同步更新本文件。

---

*文档版本: v1.1*  
*日期: 2026-05-12*  
*状态: M1-M7a 实现完成，M7b-M9 待开发*

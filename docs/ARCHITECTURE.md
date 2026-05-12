# Calcore Android 架构设计文档

*版本：v3.0 — 菱形穿透修订版*
*日期：2026-05-13*
*状态：设计冻结 → 实现中（M1-M5）*

---

## 1. 设计哲学

Calcore 不是传统意义上的"计算器 App"，而是一个**以"菱形穿透"为核心心智模型的可交互计算机体系结构棱镜**。因此，其架构必须满足以下核心原则：

1. **菱形管道唯一性**：所有数据流必须在中心对称的菱形网格内完成旅程，不允许任何"瞬移"或"跳过"。
2. **上下镜像严格对称**：△ 下行（ASCII→HEX→ASM→BIN→STACK→ALU）与 ▽ 上行（ALU→STACK→BIN→ASM→HEX→ASCII）必须在动画语义、状态变换、时间轴上保持严格可逆。
3. **底层引擎复用**：表达式解析（Lexer → Parser → AST → Evaluator）是所有数学功能的公共基座。
4. **UI 模式解耦**：标准/科学/程序员/日期四种计算器模式共享同一套 `CalculatorScreen` + `Keypad` + `Display` 架构；图形与数学工作台是独立的页面级模块，不污染计算器 UI。
5. **教育级可扩展性**：菱形层级、架构模型（RISC-V/ARM64/x86-64）、方块语义系统均为插件化预留。
6. **Material You 原生融合**：利用 Android 12+ 的 Dynamic Color，让硬核工具也能融入用户的个性化系统美学。

---

## 2. 技术选型总览

| 层级 | 技术 | 版本/说明 |
|---|---|---|
| UI Toolkit | Jetpack Compose | BOM 2026.02.01 |
| Design System | Material Design 3 (Material You) | Dynamic Color (API 31+) |
| 架构模式 | MVI (Model-View-Intent) | 单向数据流 |
| 依赖注入 | Hilt 2.59.2 + KSP 2.3.0 | AGP 9 必须使用 Hilt ≥2.59 + KSP2 |
| 注解处理 | KSP (替代 KAPT) | KAPT 与 AGP 9 built-in Kotlin 不兼容 |
| 状态管理 | StateFlow + data class | ViewModel 层 |
| 导航 | Jetpack Navigation Compose | 单 NavHost，多 Destination |
| 异步 | Kotlin Coroutines + Flow | 数值计算在 `Dispatchers.Default` |
| 2D 绘制 | Compose Canvas / DrawScope | 主线程，轻量；菱形舞台主要渲染方式 |
| 3D 渲染 | Compose Canvas 线框 + OpenGL ES（未来） | 复杂曲面用 GLSurfaceView |
| Shader | AGSL (RuntimeShader) | API 33+ 增强，GLES fallback |
| 视频导出 | MediaCodec + MediaMuxer | 未来阶段 |
| 计算引擎 | Kotlin 纯代码 | 预留为 KMP 模块 |

### 已知兼容性约束

- **AGP 9.2.1 + Hilt**：必须使用 `android.builtInKotlin=false` + `android.newDsl=false`，JDK 21。
- **Legacy Variant API**：`applicationVariants`/`testVariants`/`unitTestVariants` 已废弃，AGP 10 将移除；当前启用 `newDsl=false` 暂时兼容。
- **KSP2**：Hilt 2.59.2 要求 KSP 2.3.0，与 Kotlin 2.2.10 配套。

---

## 3. Material You 适配策略

### 3.1 主题架构（三套配色）

```kotlin
enum class AppTheme {
    SYSTEM_DYNAMIC,   // 跟随 Material You 壁纸取色（API 31+）
    CALCORE_DARK,     // 终端绿：#0a0a0a + #00ff41
    CALCORE_LIGHT     // 浅蓝风：#FFFFFF + 浅蓝/蓝色系
}
```

### 3.2 品牌色定义

| 令牌 | 值 | 用途 |
|---|---|---|
| `TerminalGreen` | `#00FF41` | 数据流、位格翻转、指针连线、菱形通道实心方块 |
| `TerminalAmber` | `#FFA657` | 警告、进位、溢出指示、ALU 等待态边框 |
| `TerminalBackground` | `#0A0A0A` | 可视化舞台（固定，不受主题影响） |
| `TerminalGray` | `#8B949E` | 静态结构、注释、菱形通道壁、空心方块边框 |
| `AluCyan` | `#00E5FF` | ALU 运算结果方块，象征反应完成 |

### 3.3 品牌色与动态色的融合策略

| 场景 | 策略 |
|---|---|
| **App 主背景** | 由当前配色方案决定：系统动态色跟随壁纸；终端绿用 `#0a0a0a`；浅蓝白用纯白 `#FFFFFF`。 |
| **可视化舞台背景** | 固定 `#0a0a0a`（内容画布），不受配色方案影响，保持底层沉浸感。浅蓝白模式下可视化区仍保持深色，形成「暗黑舞台 + 明亮 UI」的对比美学。 |
| **数据流/强调色** | UI 控件使用 `MaterialTheme.colorScheme.primary`；可视化区保留科技绿 `#00ff41` 作为数据流语义色（不受主题切换影响）。 |
| **警告/溢出** | UI 层使用 `MaterialTheme.colorScheme.error`；可视化区保留琥珀色 `#ffa657` 作为进位指示语义色。 |

### 3.4 字体系统

- **等宽字体**：地址、寄存器名、二进制位格、HEX 值使用 `FontFamily.Monospace`。
- **数学公式**：集成 `androidx.compose.ui.text` 的 `SpanStyle` 实现上标/下标；长期预留 MathML / LaTeX 渲染接入点。
- **层级**：`displayLarge` 用于计算结果；`headlineMedium` 用于模式标题；`bodyMedium` 用于寄存器标签；`labelSmall` 用于内存地址与 ASM 助记符。

---

## 4. 顶层导航与模块划分

### 4.1 DrawerMenu 结构（目标态）

```
计算
├── 标准        (CalculatorMode.STANDARD)   → CalculatorScreen
├── 科学        (CalculatorMode.SCIENTIFIC) → CalculatorScreen
├── 程序员      (CalculatorMode.PROGRAMMER) → CalculatorScreen
├── 日期计算    (CalculatorMode.DATE)       → CalculatorScreen
└── 图形        (GraphingScreen)            → GraphingScreen

数学工作台
├── 线性代数    (LinearAlgebraScreen)
├── 微积分      (CalculusScreen)
└── 微分方程    (DifferentialEquationsScreen)

转换器
├── 货币 / 体积 / 长度 / 重量 / 温度 / 能量 / 面积 / 速度 / 时间 / 功率 / 数据

其他
├── 设置        (SettingsScreen)
└── 关于
```

### 4.2 关键架构决策

| 决策 | 说明 |
|------|------|
| **Graphing 不入 CalculatorMode** | 图形模式的 UI（表达式列表 + 2D/3D视口 + 参数面板）与计算器（显示+键盘）完全不同。Graphing 作为独立页面，通过 DrawerMenu 入口导航。 |
| **数学工作台独立页面** | 线性代数（矩阵网格编辑）、微积分（逐步动画）、微分方程（相图绘制）各自的交互范式差异极大，必须是独立 Screen + ViewModel。 |
| **共享表达式引擎** | 所有模块共用 `engine.parser` 和 `engine.evaluator`；图形与工作台的额外数值需求通过扩展 `engine` 子包实现，不破坏现有接口。 |
| **菱形舞台独占计算器可视化** | 计算器模式的可视化层被彻底重构为 `DiamondStage`，取代原有的 L1-L8 分散组件。所有数据流必须在菱形网格内完成，不允许跳层。 |
| **单模块当前，多模块未来** | 由于项目处于 M1-M3 阶段，所有代码以包形式存在于 `:app` 模块内。M5 后可视需求拆分为 `:feature:graphing`、`:feature:linearalgebra` 等 Gradle 模块。 |

---

## 5. 包结构（当前 `:app` 内）

```
com.cloveriris.calcore
├── CalcoreApplication.kt
├── MainActivity.kt
│
├── navigation
│   └── CalcoreNavHost.kt       # NavHost: calculator / graphing / linear_algebra / calculus / differential_equations / settings
│
├── data                        # DataStore、本地缓存
│   └── local/
│       └── ThemeDataStore.kt
│
├── domain
│   ├── model
│   │   ├── CalculatorMode.kt
│   │   ├── CalculatorState.kt
│   │   ├── CalculatorInput.kt
│   │   ├── DiamondEvent.kt           # 菱形动画事件（新增/重构）
│   │   ├── Architecture.kt
│   │   ├── BitWidth.kt
│   │   ├── NumberBase.kt
│   │   ├── DiamondLevel.kt           # △L1-L7 / ALU / ▽L7'-L2'（新增）
│   │   ├── BlockSemantics.kt         # 实心/空心/半实心方块语义（新增）
│   │   ├── expression/               # 表达式相关模型（AST节点、Token等）
│   │   ├── matrix/                   # 矩阵、向量、线性代数模型
│   │   ├── calculus/                 # 微积分步骤、黎曼和、数值积分模型
│   │   ├── equation/                 # ODE/PDE 定义、初边值条件
│   │   └── template/                 # 数学模板系统
│   └── usecase
│       ├── EvaluateUseCase.kt
│       ├── MatrixOperationsUseCase.kt      (TODO)
│       ├── CalculusVisualizationUseCase.kt (TODO)
│       └── OdeSolveUseCase.kt              (TODO)
│
├── engine                        # 纯算法层，无 Android 依赖
│   ├── parser/                   # 词法/语法分析（已有）
│   │   ├── Lexer.kt
│   │   ├── Parser.kt
│   │   ├── AST.kt
│   │   └── Token.kt
│   ├── evaluator/                # AST 求值（已有）
│   │   └── Evaluator.kt
│   ├── diamond/                  # 菱形管道核心引擎（新增/重构）
│   │   ├── AsciiEncoder.kt       # L1→L2: ASCII → HEX
│   │   ├── AsmTranslator.kt      # L2→L3: HEX → ASM（架构相关）
│   │   ├── BinUnfolder.kt        # L3→L4: ASM → BIN (64-bit bit array)
│   │   ├── StackInjector.kt      # L4→L5: BIN → Stack push
│   │   ├── AluExecutor.kt        # L6-L7: ALU fetch + execute
│   │   ├── DiamondScript.kt      # 完整脚本生成（TimedAction 列表）
│   │   └── DiamondReducer.kt     # 状态归约器
│   ├── matrix/                   # 矩阵运算（高斯消元、特征值、SVD...）
│   │   └── MatrixOperations.kt
│   ├── calculus/                 # 数值微积分（导数近似、积分、级数）
│   │   └── NumericalCalculus.kt
│   └── ode/                      # ODE 数值求解（欧拉、RK4、自适应步长）
│       └── OdeSolver.kt
│
├── presentation                  # ViewModel 层
│   ├── calculator/
│   │   ├── CalculatorViewModel.kt
│   │   └── CalculatorUiState.kt
│   ├── graphing/
│   │   └── GraphingViewModel.kt
│   ├── linearalgebra/
│   │   ├── LinearAlgebraViewModel.kt
│   │   └── LinearAlgebraUiState.kt
│   ├── calculus/
│   │   ├── CalculusViewModel.kt
│   │   └── CalculusUiState.kt
│   ├── diffeq/
│   │   ├── DiffEqViewModel.kt
│   │   └── DiffEqUiState.kt
│   └── diamond/                  # 菱形可视化 ViewModel（新增/重构）
│       ├── DiamondViewModel.kt
│       └── DiamondUiState.kt
│
├── ui                            # Compose UI 层
│   ├── calculator/               # CalculatorScreen、Keypads、Display
│   │   ├── CalculatorScreen.kt
│   │   ├── StandardKeypad.kt
│   │   ├── ScientificKeypad.kt
│   │   ├── ProgrammerKeypad.kt
│   │   └── ProgrammerDisplay.kt
│   ├── graphing/                 # GraphingScreen、ExpressionList、CoordinateCanvas
│   │   └── GraphingScreen.kt
│   ├── linearalgebra/            # MatrixEditor、OperationToolbar、ResultPanel、VectorTransformCanvas
│   │   └── LinearAlgebraScreen.kt
│   ├── calculus/                 # CalculusScreen、GeometryCanvas、ParameterInputBar
│   │   └── CalculusScreen.kt
│   ├── diffeq/                   # DiffEqScreen、TemplateSelector、ParameterSliderPanel
│   │   └── DifferentialEquationsScreen.kt
│   ├── diamond/                  # 菱形舞台全部组件（新增/重构）
│   │   ├── DiamondStage.kt       # 总容器：中心对称布局 + 背景 + 骨架
│   │   ├── AsciiSurface.kt       # L1 / L2': ASCII 顶层/底层光带 + 输入探针
│   │   ├── HexChannel.kt         # L2 / L3': HEX 字节流通道（左腰/右腰）
│   │   ├── AsmCard.kt            # L3 / L4': ASM 指令卡片层（架构滤镜）
│   │   ├── BitGrid.kt            # L4 / L5': 8×8 位格网展开/折叠动画
│   │   ├── StackChannel.kt       # L5 / L6': 内存栈通道（PUSH/POP + SP 箭头）
│   │   ├── AluCore.kt            # ALU 核心 ◇：碰撞动画 + 布尔门网格 + 结果生成
│   │   ├── DiamondScrubber.kt    # 垂直时间轴：沿中轴线拖动，穿越任意层级
│   │   ├── ArchitectureFilter.kt # 架构切换器：X64/RV64/ARM64 万花筒过渡
│   │   ├── LevelFilterBar.kt     # 层级过滤器：开关任意菱形层级的可见性
│   │   └── BottomControlBar.kt   # 播放控制：速度/暂停/单步/回放
│   ├── components/               # 跨模块共享组件
│   │   ├── CalcoreButton.kt
│   │   ├── CalcoreDisplay.kt
│   │   ├── DrawerMenu.kt
│   │   ├── ScrollableKeypadContainer.kt
│   │   ├── ExpressionInputBar.kt
│   │   ├── ParameterSliderPanel.kt
│   │   ├── TemplateSelector.kt
│   │   └── CoordinateCanvas.kt
│   └── theme/
│       ├── Color.kt
│       ├── Type.kt
│       ├── Shape.kt
│       └── Theme.kt
│
└── di/                           # Hilt 模块（如需额外绑定）
```

---

## 6. 数据流与状态管理

### 6.1 计算器模式（菱形管道）

```
用户按键 → CalculatorViewModel.onInput()
         → 更新 CalculatorUiState
         → emit DiamondEvent → DiamondViewModel
         → DiamondEngine.generateScript(event, architecture)
         → 生成按时间排序的 TimedAction 列表（△L1-L7 + ALU + ▽L7'-L2'）
         → DiamondPlayer 按 playbackSpeed 驱动回放
         → reduceAction 累积式更新 DiamondUiState
         → DiamondStage 重组，各层级 Canvas 组件独立绘制
```

**状态机**：

```
                    ┌──────────────┐
        数字/小数点 →│   IDLE       │←── 清除/重置
        运算符      →│ (等待输入)   │
                    └──────┬───────┘
                           │ 数字输入
                           ▼
                    ┌──────────────┐
        运算符      →│  INPUTTING   │←── 退格
        等号        →│ (输入中)     │
                    └──────┬───────┘
                           │ 等号
                           ▼
                    ┌──────────────┐
        等号(重播)  →│  EVALUATED   │←── 模式切换
        新数字      →│ (已求值)     │     (重置为 IDLE)
        运算符      →│              │
                    └──────────────┘
```

每个状态转移都触发 `DiamondViewModel` 更新对应的菱形层级状态。关键差异：
- **退格/删除**：触发 `DiamondEvent.ReverseDownward`，已下行的方块沿原路径被"吸回"顶部，所有途经层级同步逆向消隐。
- **光标移动**：触发 `DiamondEvent.ProbeMove`，输入探针在 ASCII 光带中滑动，侧壁地址指针同步高亮。

### 6.2 图形模式（保留，未来扩展）

```
用户输入表达式 → GraphingViewModel.addExpression(expr)
               → 调用 parser 生成 AST
               → 调用 evaluator 采样生成顶点数据
               → 更新 GraphingUiState（表达式列表 + 顶点缓存）
               → GraphingScreen 重组（左侧列表 + 中央 Canvas 绘制）
```

### 6.3 架构模型与播放速度控制

#### 6.3.1 Architecture 抽象模型

```kotlin
enum class Architecture(
    val displayName: String,
    val registerNames: List<String>,
    val spRegisterName: String,      // 栈指针寄存器名
    val fpRegisterName: String,      // 帧指针寄存器名
    val stackGrowsDown: Boolean,     // 主流架构均为 true
    val addressingMode: AddressingMode,
    val mnemonicStyle: MnemonicStyle // 新增：影响 ASM 卡片纹理
)

enum class MnemonicStyle {
    X86_CISC,    // 硬朗直角金属质感（MOV/PUSH/POP/CALL）
    ARM_RISC,    // 圆角流体质感（LDP/STP/MOV/ADD）
    RISCV_RISC   // 极简细线质感（LI/ADDI/LD/SD/JAL）
}
```

| 架构 | SP | FP | 寻址方式 | 指令风格 | 卡片纹理 |
|------|-----|-----|---------|---------|---------|
| x86-64 | RSP | RBP | SEGMENT_OFFSET | CISC (MOV/PUSH/POP) | 硬朗直角金属 |
| ARM64 | SP | X29 | BASE_INDEX_OFFSET | RISC (STP/LDP/BL) | 圆角流体 |
| RISC-V | SP | S0 | BASE_INDEX_OFFSET | RISC (ADDI SP/JAL) | 极简细线 |

**影响范围**：
- `DiamondEngine.generateScript(event, architecture)`：根据架构生成不同的指令助记符、寄存器名、栈指针标签。
- `AsmCard`：使用 `architecture.mnemonicStyle` 决定卡片圆角、边框粗细、背景渐变滤镜。
- `StackChannel`：使用 `architecture.spRegisterName` 标注栈指针箭头。

#### 6.3.2 动画播放速度控制

```kotlin
// DiamondViewModel
private val baseStepIntervalMs = 200L  // 基准 tick = 200ms

// 实际延迟 = baseStepIntervalMs / playbackSpeed
// speed = 0.25x → 800ms/step（极慢观察）
// speed = 1.0x → 200ms/step（默认）
// speed = 4.0x → 50ms/step（快速浏览）
```

- `playbackSpeed` 属于 `DiamondUiState`，范围 `0.25f..4.0f`。
- 速度通过 `SettingsScreen` 的 Slider 调节，设置页通过 parent backstack entry 共享 `DiamondViewModel` 实例。
- 脚本步数 = `totalDurationMs / baseStepIntervalMs`，确保不同总长度的脚本播放时间一致。

---

## 7. 渲染管线设计

### 7.1 菱形舞台（Compose Canvas）— 计算器模式

```
用户按键 → CalculatorViewModel 生成 DiamondEvent
                ↓
    DiamondViewModel.onEvent(event)
                ↓
    DiamondEngine.generateScript(event, architecture)
        → 生成按时间排序的 TimedAction 列表（△L1-L7 + ALU + ▽L7'-L2'）
                ↓
    DiamondPlayer 按 playbackSpeed 驱动回放
        → 每 200ms / speed 一个 step
        → applyScriptAtProgress(progress) 累积式更新状态
                ↓
    Compose 重组 → DiamondStage Composable
                ↓
    各层级 Canvas 组件根据状态独立绘制
        - AsciiSurface: 顶部/底部光带 + 输入探针
        - HexChannel: 左腰/右腰 HEX 方块流
        - AsmCard: 左腰/右腰指令卡片（带架构纹理滤镜）
        - BitGrid: 8×8 位格网展开/折叠动画
        - StackChannel: 垂直栈槽 + SP 箭头 + PUSH/POP 滑动
        - AluCore: 中心菱形 + 碰撞脉冲 + 布尔门闪烁
```

**状态归约器（Reducer）设计**：
- `DiamondViewModel.reduceAction(state, action)` 是单一状态变换入口。
- 每个 `DiamondAction` 子类对应一个不可变的 `copy()` 变换，避免清空重建导致的闪烁。
- 新 Action **平滑覆盖**旧字段：例如 `UpdateHexChannel` 只修改目标 HEX 槽位，保留其余层级状态。
- **镜像对称保证**：▽ 上行的 Action 必须是 △ 下行 Action 的语义镜像，Reducer 中通过成对定义确保可逆性。

**性能策略**：
- `Canvas` 的 `draw` lambda 在后台线程的 Skia 中执行，但状态更新在主线程。
- 使用 `remember { Path() }` 缓存静态路径（如菱形骨架线、通道壁网格），避免每帧重建。
- 位格翻转动画使用 `Animatable<Float>` 控制 alpha/scale，而非每帧重算 Path。
- **⚡ 栈帧滑动动画**：`StackChannel` 使用 `Animatable` 驱动 `progress`，在 `DrawScope` 中实时计算滑块坐标，避免触发额外重组。
- **⚡ 输入探针平滑移动**：`AsciiSurface` 追踪 `previousProbeIndex`，通过 `Animatable(0f→1f)` 插值实现 150ms 平滑过渡。
- **⚡ 架构滤镜过渡**：`AsmCard` 的纹理切换使用 `Animatable` 控制滤镜透明度交叉淡入淡出（300ms），而非重建卡片对象。

### 7.2 数学工作台渲染（Compose Canvas）— 保留

数学工作台的渲染层独立实现（基于 Compose Canvas 的坐标系/矩阵/向量场渲染），与菱形舞台不共享组件，仅在视觉风格（TerminalGreen `#00FF41`、方块美学）上保持一致。

---

## 8. 可视化分层：The Diamond Pipeline

### 8.1 △ 下行链路（L1 → L2 → L3 → L4 → L5 → L6 → L7）

| 层级 | 组件 | 展示内容 | 动画表现形式 | 关键模型 |
|------|------|---------|-------------|---------|
| **L1: ASCII Surface** | `AsciiSurface` | 用户输入的数字、符号、表达式字符 | 字符悬浮在菱形顶部，冷白发光；输入探针同步滑动；光带长度 = 表达式长度 | `DiamondAction.UpdateAsciiBand` + `MoveInputProbe` |
| **L2: HEX Encoding** | `HexChannel` (左腰) | 字符的十六进制字节流 | 字符"碎裂"为成对 HEX 数码；绿色实心方块沿左腰通道下行；空心通道壁被短暂点亮形成涟漪 | `DiamondAction.InjectHexBlock` + `ShatterAscii` |
| **L3: ASM Translation** | `AsmCard` (左腰) | 架构相关汇编指令（mov/push/add/li 等） | HEX 方块重组为指令卡片，显示助记符与操作数；卡片纹理依架构变化（x86=金属直角、ARM=圆角流体、RISC-V=极简细线） | `DiamondAction.FormAsmCard` + `ArchitectureFilterTransition` |
| **L4: BIN Unfolding** | `BitGrid` (左腰) | 64-bit 机器码位数组 | 指令卡片像折纸展开为 8×8 位格网；1=绿色实心微型方块，0=绿色空心微型方块 | `DiamondAction.UnfoldBitGrid` + `FlipBitCell` |
| **L5: Stack Injection** | `StackChannel` (左腰) | 内存栈压栈过程 | 位格收束为方块，垂直落入左腰栈槽；空心槽位被实心填充；SP 箭头同步下移；卡扣咬合质感 | `DiamondAction.PushStack` + `UpdateStackPointer` |
| **L6: ALU Fetch** | `StackChannel` → `AluCore` | 从栈中读取操作数 | 实心方块从栈槽水平抽取，沿通道滑向中心 ALU；拖曳绿色尾迹 | `DiamondAction.FetchToAlu` + `SpawnTrail` |
| **L7: ALU Execution** | `AluCore` | ALU 运算（ADD/SUB/MUL/DIV/AND/OR/XOR 等） | 操作数方块在中心菱形中碰撞融合；内部展开布尔逻辑门网格一帧闪烁；结果方块生成，颜色从绿渐变为青绿 | `DiamondAction.AluCollide` + `BooleanGateFlash` + `EmitResultBlock` |

### 8.2 ▽ 上行链路（L7' → L6' → L5' → L4' → L3' → L2' → L1'）

▽ 上行是 △ 下行的**严格镜像反转**：

| 层级 | 组件 | 展示内容 | 动画表现形式（与下行镜像） | 关键模型 |
|------|------|---------|------------------------|---------|
| **L7': ALU Result Emit** | `AluCore` → `StackChannel` (右腰) | 结果从 ALU 压回栈 | 结果方块从 ALU 底部"滴落"，进入右腰栈通道；SP 同步移动；视觉上呈"从核心向外生长" | `DiamondAction.EmitFromAlu` (镜像 `FetchToAlu`) |
| **L6': Stack to BIN** | `StackChannel` (右腰) → `BitGrid` (右腰) | 结果从栈取出展开为位数组 | 方块从栈槽升起，重新展开为 64 位格网，位格向上漂浮 | `DiamondAction.UnfoldFromStack` (镜像 `PushStack`) |
| **L5': BIN to ASM** | `BitGrid` (右腰) → `AsmCard` (右腰) | 位格折叠为指令卡片 | 位格折叠为指令卡片，沿右腰上浮；卡片从背面翻回正面 | `DiamondAction.FoldToAsm` (镜像 `UnfoldBitGrid`) |
| **L4': ASM to HEX** | `AsmCard` (右腰) → `HexChannel` (右腰) | 指令卡片碎裂为 HEX 字节流 | 卡片碎裂为 HEX 字节带，绿色实心方块沿右腰上行 | `DiamondAction.ShatterToHex` (镜像 `FormAsmCard`) |
| **L3': HEX to ASCII** | `HexChannel` (右腰) → `AsciiSurface` | HEX 解码为可显示字符 | 成对 HEX 方块融合，重新组装为字符形态 | `DiamondAction.FuseToAscii` (镜像 `InjectHexBlock`) |
| **L2': ASCII Surface** | `AsciiSurface` (底部) | 最终数字显示在显示屏 | 字符从菱形底部升起，落入显示屏区域；光带脉冲后稳定常亮；菱形闭合完成 | `DiamondAction.RiseToDisplay` + `DiamondClosurePulse` |

### 8.3 交汇点：The ALU Core（◇）

| 状态 | 视觉表现 | 模型 |
|------|---------|------|
| **空闲** | 缓慢呼吸灯效（透明度正弦波动），维持舞台重心 | `AluState.Idle` |
| **等待操作数** | 边框变为琥珀色；已到达的操作数以"半实心"状态悬浮在菱形侧翼 | `AluState.Waiting` |
| **运算中** | 向四周发射环形脉冲，瞬间点亮菱形骨架所有通道壁；内部布尔门网格闪烁 | `AluState.Executing` |
| **保留中间结果** | 菱形内部保留半实心结果方块，等待下一次操作数 | `AluState.Holding` |

---

## 9. 与现有代码的兼容策略

### 9.1 破坏性变更清单

本次 v3.0 架构修订涉及以下**结构性变更**：

| 旧组件 | 新组件 | 变更类型 |
|--------|--------|---------|
| `VisualizationStage.kt` | `DiamondStage.kt` | 重写 |
| `VisualizationViewModel.kt` | `DiamondViewModel.kt` | 重写（状态模型完全替换） |
| `VisualizationUiState.kt` | `DiamondUiState.kt` | 重写 |
| `RegisterBank.kt` | （移除，功能并入 `StackChannel` + `AluCore`） | 删除 |
| `MemoryGrid.kt` | （移除，功能并入 `StackChannel`） | 删除 |
| `LinkedListView.kt` | （移除，AST 与链表语义退化为菱形侧壁微型节点） | 删除 |
| `OperatorStackView.kt` | （移除，调度场算法在菱形侧壁以微型空心方块表示） | 删除 |
| `AstTreeView.kt` | （移除，AST 语义退化为侧壁微型节点） | 删除 |
| `LogicGateGrid.kt` | （降级为 `AluCore` 内部一帧闪烁动画） | 降级 |
| `AddressBusView.kt` | （移除，段位移拼接在菱形边缘刻度尺实现） | 删除 |
| `InstructionPipeline.kt` | （移除，流水线语义被 ASM 卡片层替代） | 删除 |
| `DisplayBufferView.kt` | （功能并入 `AsciiSurface` 底部） | 删除 |
| `TimelineScrubber.kt` | `DiamondScrubber.kt` | 重写（垂直中轴线布局） |

### 9.2 保留内容清单

| 层级 | 保留包/文件 | 说明 |
|------|-----------|------|
| domain | `model/expression/` | AST、Token 等表达式模型不变 |
| engine | `engine/parser/`、`engine/evaluator/` | 解析与求值引擎不变 |
| ui | `ui/calculator/` | CalculatorScreen、Keypads、Display 不变 |
| ui | `ui/theme/` | Material You 主题系统不变 |
| data | `data/local/` | ThemeDataStore 等本地缓存不变 |

---

## 10. 性能预算

| 指标 | 目标 | 实现策略 |
|---|---|---|
| 按键 → ASCII 层点亮 | ≤ 50ms | 预计算状态，`DiamondEngine.generateScript` 纯 Kotlin 无 IO；延迟 16ms 启动协程 |
| `=` → ALU 核心爆发 | ≤ 100ms | 表达式预解析，ALU 动画脚本提前生成 |
| 动画帧率 | 60 FPS | `delay(16)` + 裁剪绘制区域；`Animatable` 驱动避免连续重组 |
| 菱形骨架绘制 | ≤ 8ms/帧 | 静态 Path 缓存；仅数据方块和指针需要动态计算 |
| 栈操作动画 | ≤ 8ms/帧 | `StackChannel` 使用 `Animatable` 在 `DrawScope` 内插值，不触发重组 |
| 架构滤镜过渡 | ≤ 300ms | 交叉淡入淡出，不重建卡片对象 |
| 删除逆向瀑布 | ≤ 200ms | 逆向 Action 序列直接复用正向缓存的 `TimedAction`，无需重新计算 |
| 内存峰值 | ≤ 256MB | 菱形状态快照限制 100 条；`DiamondScript` 事件列表 ≤ 50 个 |
| APK 体积 | ≤ 50MB | 按需加载着色器；资源压缩 |

---

## 11. 实施路线图（菱形穿透修订版）

| 阶段 | 目标 | 关键交付 | 状态 |
|------|------|---------|------|
| **M1** | 菱形骨架引擎 | 完成中心对称菱形网格、方块系统、通道壁、ALU 核心节点、呼吸灯效 | 🔄 进行中 |
| **M2** | △ 下行链路 | 完成 ASCII→HEX→ASM→BIN→Stack→ALU 的七层动画与数据流；输入探针、光带、碎裂动画、折纸展开、栈压入 | ⏳ 待实现 |
| **M3** | ▽ 上行链路 | 完成严格的镜像反转链路，实现菱形闭合；结果滴落、位格上浮、卡片折叠、HEX 碎裂、字符融合 | ⏳ 待实现 |
| **M4** | 架构滤镜 | 实现 X64 / RV64 / ARM64 的 ASM 层纹理切换与指令差异；万花筒旋转过渡；栈指针与寻址方式差异 | ⏳ 待实现 |
| **M5** | 交互抛光 | 输入探针、时间轴 scrubber、光标同步、删除逆向瀑布、3B1B 级缓动质感；0.25x~4x 播放速度；单步调试 | ⏳ 待实现 |

> **注**：v2.0 中的 M5a-M9（图形基座、线性代数、微积分、ODE/PDE、模板系统、参数动画、跨模块联动）在菱形穿透核心稳定后，以 **v3.1/v3.2** 子版本继续推进。图形与数学工作台的渲染层独立于菱形舞台，不受影响。

---

## 12. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 菱形对称性实现复杂 | △ 与 ▽ 的动画难以严格镜像，导致视觉不协调 | 在 `DiamondEngine` 中成对定义 Action（`XxxDown` / `XxxUp`），共用同一套几何参数与缓动函数；通过单元测试验证镜像可逆性 |
| Compose Canvas 绘制大量方块性能不足 | 60 FPS 无法保证 | 限制同时可见方块数 ≤ 32 个；栈通道仅渲染视口内槽位；使用 `drawWithCache` 缓存静态位格 |
| 删除逆向瀑布状态回滚困难 | 已传播到 L5/L6 的方块难以精确定位并吸回 | `DiamondUiState` 维护每个输入字符的 `BlockId` 追踪链；退格时按 `BlockId` 精确回收 |
| 多架构 ASM 生成逻辑自洽难 | 同一 HEX 在不同架构下 BIN 结果不一致 | `AsmTranslator` 的单元测试覆盖全部三种架构；以 `Evaluator` 的数值结果作为唯一真值，ASM 层仅做可视化映射 |
| 旧 L1-L8 组件直接删除导致编译失败 | 大量历史代码引用被移除的组件 | 分阶段迁移：M1 先新建 `ui/diamond/` 包，保留旧 `ui/visualization/` 但标记 `@Deprecated`；M3 完成后再删除旧包 |

---

*本文档与 `PRD..md` 同步维护。任何架构级变更需同时更新两者。*

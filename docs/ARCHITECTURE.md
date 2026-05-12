# Calcore Android 架构设计文档

*版本：v2.2*  
*日期：2026-05-12*  
*状态：设计冻结 → 实现中*

---

## 1. 设计哲学

Calcore 不是传统意义上的"计算器 App"，而是一个**可交互的数学与计算机体系结构教学引擎**。因此，其架构必须满足以下核心原则：

1. **底层引擎复用**：表达式解析（Lexer → Parser → AST → Evaluator）是所有数学功能的公共基座。
2. **UI 模式解耦**：标准/科学/程序员/日期四种计算器模式共享同一套 `CalculatorScreen` + `Keypad` + `Display` 架构；图形与数学工作台是独立的页面级模块，不污染计算器 UI。
3. **可视化分层**：计算器模式的可视化（L1-L8，位格/寄存器/内存/栈）与数学工作台的可视化（G1-G4，坐标系/矩阵/向量场/相图）使用两套独立但风格统一的渲染管线。
4. **教育级可扩展性**：可视化层级、架构模型（RISC-V/ARM64/x86-64）、数学对象类型均为插件化预留。
5. **Material You 原生融合**：利用 Android 12+ 的 Dynamic Color，让硬核工具也能融入用户的个性化系统美学。

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
| 导航 | Jetpack Navigation Compose | 单 NavHost，3 个 Destination（当前） |
| 异步 | Kotlin Coroutines + Flow | 数值计算在 `Dispatchers.Default` |
| 2D 绘制 | Compose Canvas / DrawScope | 主线程，轻量；数学工作台主要渲染方式 |
| 3D 渲染 | Compose Canvas 线框 + OpenGL ES（M6） | 复杂曲面用 GLSurfaceView |
| Shader | AGSL (RuntimeShader) | API 33+ 增强，GLES fallback |
| 视频导出 | MediaCodec + MediaMuxer | M8 阶段 |
| 计算引擎 | Kotlin 纯代码 | M1 预留为 KMP 模块 |

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
| `TerminalGreen` | `#00FF41` | 数据流、位格翻转、指针连线 |
| `TerminalAmber` | `#FFA657` | 警告、进位、溢出指示 |
| `TerminalBackground` | `#0A0A0A` | 可视化舞台（固定，不受主题影响） |
| `TerminalGray` | `#8B949E` | 静态结构、注释、内存网格线 |

### 3.3 品牌色与动态色的融合策略

| 场景 | 策略 |
|---|---|
| **App 主背景** | 由当前配色方案决定：系统动态色跟随壁纸；终端绿用 `#0a0a0a`；浅蓝白用纯白 `#FFFFFF`。 |
| **可视化舞台背景** | 固定 `#0a0a0a`（内容画布），不受配色方案影响，保持底层沉浸感。浅蓝白模式下可视化区仍保持深色，形成「暗黑舞台 + 明亮 UI」的对比美学。 |
| **数据流/强调色** | UI 控件使用 `MaterialTheme.colorScheme.primary`；可视化区保留科技绿 `#00ff41` 作为数据流语义色（不受主题切换影响）。 |
| **警告/溢出** | UI 层使用 `MaterialTheme.colorScheme.error`；可视化区保留琥珀色 `#ffa657` 作为进位指示语义色。 |

### 3.4 字体系统

- **等宽字体**：地址、寄存器名、二进制位格使用 `FontFamily.Monospace`。
- **数学公式**：集成 `androidx.compose.ui.text` 的 `SpanStyle` 实现上标/下标；长期预留 MathML / LaTeX 渲染接入点。
- **层级**：`displayLarge` 用于计算结果；`headlineMedium` 用于模式标题；`bodyMedium` 用于寄存器标签；`labelSmall` 用于内存地址。

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
| **Graphing 不入 CalculatorMode** | 图形模式的 UI（表达式列表 + 2D/3D视口 + 参数面板）与计算器（显示+键盘）完全不同。若强行塞进 `CalculatorScreen`，会导致巨大的 `when(mode)` 分支和代码污染。Graphing 作为独立页面，通过 DrawerMenu 入口导航。 |
| **数学工作台独立页面** | 线性代数（矩阵网格编辑）、微积分（逐步动画）、微分方程（相图绘制）各自的交互范式差异极大，必须是独立 Screen + ViewModel。 |
| **共享表达式引擎** | 所有模块共用 `engine.parser` 和 `engine.evaluator`；图形与工作台的额外数值需求（ODE 求解、矩阵运算）通过扩展 `engine` 子包实现，不破坏现有接口。 |
| **可视化双轨制** | 计算器模式的 `VisualizationStage/VisualizationViewModel`（L1-L8）保持不动；图形与工作台的渲染层独立实现（基于 Compose Canvas 的坐标系/矩阵/向量场渲染），仅在视觉风格（终端绿、方块美学）上保持一致。 |
| **单模块当前，多模块未来** | 由于项目处于 M3-M4 阶段，所有代码以包形式存在于 `:app` 模块内。M5 后可视需求拆分为 `:feature:graphing`、`:feature:linearalgebra` 等 Gradle 模块。 |

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
│   │   ├── AnimationEvent.kt
│   │   ├── Architecture.kt
│   │   ├── BitWidth.kt
│   │   ├── NumberBase.kt
│   │   ├── VisualizationLevel.kt
│   │   ├── expression/         # 表达式相关模型（AST节点、Token等）
│   │   ├── matrix/             # 矩阵、向量、线性代数模型
│   │   ├── calculus/           # 微积分步骤、黎曼和、数值积分模型
│   │   ├── equation/           # ODE/PDE 定义、初边值条件
│   │   └── template/           # 数学模板系统
│   └── usecase
│       ├── EvaluateUseCase.kt
│       ├── MatrixOperationsUseCase.kt      (TODO)
│       ├── CalculusVisualizationUseCase.kt (TODO)
│       └── OdeSolveUseCase.kt              (TODO)
│
├── engine                      # 纯算法层，无 Android 依赖
│   ├── parser/                 # 词法/语法分析（已有）
│   │   ├── Lexer.kt
│   │   ├── Parser.kt
│   │   ├── AST.kt
│   │   └── Token.kt
│   ├── evaluator/              # AST 求值（已有）
│   │   └── Evaluator.kt
│   ├── matrix/                 # 矩阵运算（高斯消元、特征值、SVD...）
│   │   └── MatrixOperations.kt
│   ├── calculus/               # 数值微积分（导数近似、积分、级数）
│   │   └── NumericalCalculus.kt
│   └── ode/                    # ODE 数值求解（欧拉、RK4、自适应步长）
│       └── OdeSolver.kt
│
├── presentation                # ViewModel 层
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
│   └── visualization/
│       ├── VisualizationViewModel.kt
│       └── VisualizationUiState.kt
│
├── ui                          # Compose UI 层
│   ├── calculator/             # CalculatorScreen、Keypads、Display
│   │   ├── CalculatorScreen.kt
│   │   ├── StandardKeypad.kt
│   │   ├── ScientificKeypad.kt
│   │   ├── ProgrammerKeypad.kt
│   │   └── ProgrammerDisplay.kt
│   ├── graphing/               # GraphingScreen、ExpressionList、CoordinateCanvas
│   │   └── GraphingScreen.kt
│   ├── linearalgebra/          # MatrixEditor、OperationToolbar、ResultPanel、VectorTransformCanvas
│   │   ├── LinearAlgebraScreen.kt
│   │   ├── MatrixEditor.kt
│   │   ├── MatrixListPanel.kt
│   │   ├── OperationToolbar.kt
│   │   ├── ResultPanel.kt
│   │   └── VectorTransformCanvas.kt
│   ├── calculus/               # CalculusScreen、GeometryCanvas、ParameterInputBar
│   │   └── CalculusScreen.kt
│   ├── diffeq/                 # DiffEqScreen、TemplateSelector、ParameterSliderPanel
│   │   └── DifferentialEquationsScreen.kt
│   ├── visualization/          # L1-L8 全部可视化组件
│   │   ├── VisualizationStage.kt
│   │   ├── BitGrid.kt
│   │   ├── RegisterBank.kt
│   │   ├── MemoryGrid.kt
│   │   ├── StackView.kt
│   │   ├── LinkedListView.kt
│   │   ├── AstTreeView.kt
│   │   ├── OperatorStackView.kt
│   │   ├── LogicGateGrid.kt
│   │   ├── AluVisualizer.kt
│   │   ├── AddressBusView.kt
│   │   ├── InstructionPipeline.kt
│   │   ├── DisplayBufferView.kt
│   │   ├── TimelineScrubber.kt
│   │   └── BottomControlBar.kt
│   ├── components/             # 跨模块共享组件
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
└── di/                         # Hilt 模块（如需额外绑定）
```

---

## 6. 数据流与状态管理

### 6.1 计算器模式（已有）

```
用户按键 → CalculatorViewModel.onInput()
         → 更新 CalculatorUiState
         → emit AnimationEvent → VisualizationViewModel
         → VisualizationUiState 更新 → VisualizationStage 重组
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

每个状态转移都触发 `VisualizationViewModel` 更新对应的位格/寄存器/AST 状态。

### 6.2 图形模式（新增）

```
用户输入表达式 → GraphingViewModel.addExpression(expr)
               → 调用 parser 生成 AST
               → 调用 evaluator 采样生成顶点数据
               → 更新 GraphingUiState（表达式列表 + 顶点缓存）
               → GraphingScreen 重组（左侧列表 + 中央 Canvas 绘制）

用户拖动参数滑块 → GraphingViewModel.updateParameter(name, value)
                  → 重采样 → 更新顶点缓存 → Canvas 重绘
```

**状态机**：

```
                    ┌──────────────┐
        输入表达式   →│   EDITING    │
        确认         →│ (编辑中)     │
                    └──────┬───────┘
                           │ 确认
                           ▼
                    ┌──────────────┐
        参数变化     →│  RENDERING   │←── 采样中
        相机手势     →│ (渲染中)     │
        求值/分析    →│              │
                    └──────┬───────┘
                           │ 选择分析工具
                           ▼
                    ┌──────────────┐
        关闭分析     →│  ANALYZING   │←── 显示结果
                    │ (分析中)     │     + 可视化链路
                    └──────────────┘
```

### 6.3 数学工作台（新增）

```
用户选择模板 → *ViewModel.loadTemplate(templateId)
             → 生成默认参数 + 预填充表达式
             → 用户修改参数 → 触发计算/求解
             → 生成结果数据 + 步骤描述
             → 更新 UiState → Screen 重组（几何动画/矩阵/相图）
```

### 6.4 架构模型与播放速度控制

#### 6.4.1 Architecture 抽象模型

```kotlin
enum class Architecture(
    val displayName: String,
    val registerNames: List<String>,
    val spRegisterName: String,      // 栈指针寄存器名
    val fpRegisterName: String,      // 帧指针寄存器名
    val stackGrowsDown: Boolean,     // 主流架构均为 true
    val addressingMode: AddressingMode,
    val mnemonicPrefix: String
)
```

| 架构 | SP | FP | 寻址方式 | 指令风格 |
|------|-----|-----|---------|---------|
| x86-64 | RSP | RBP | SEGMENT_OFFSET | CISC (PUSH/POP/CALL) |
| ARM64 | SP | X29 | BASE_INDEX_OFFSET | RISC (STP/LDP/BL) |
| RISC-V | SP | S0 | BASE_INDEX_OFFSET | RISC (ADDI SP/JAL) |

**影响范围**：
- `VisualizationEngine.generateScript(event, architecture)`：根据架构生成不同的指令助记符、寄存器名、栈指针标签。
- `RegisterBank`：使用 `architecture.registerNames` 初始化默认寄存器列表。
- `StackView`：使用 `architecture.spRegisterName` 标注栈指针箭头。
- `AddressBusView`：x86-64 展示段选择子+偏移量拼接；ARM64/RISC-V 展示基址+索引+偏移。

#### 6.4.2 动画播放速度控制

```kotlin
// VisualizationViewModel
private val baseStepIntervalMs = 200L  // 基准 tick = 200ms

// 实际延迟 = baseStepIntervalMs / playbackSpeed
// speed = 0.2x → 1000ms/step（极慢观察）
// speed = 1.0x → 200ms/step（默认）
// speed = 2.0x → 100ms/step（快速浏览）
```

- `playbackSpeed` 属于 `VisualizationUiState`，范围 `0.2f..2.0f`。
- 速度通过 `SettingsScreen` 的 Slider 调节，设置页通过 parent backstack entry 共享 `VisualizationViewModel` 实例。
- 脚本步数 = `totalDurationMs / baseStepIntervalMs`，确保不同总长度的脚本播放时间一致。

---

## 7. 渲染管线设计

### 7.1 2D 可视化舞台（Compose Canvas）— 计算器模式

```
用户按键 → CalculatorViewModel 生成 AnimationEvent
                ↓
    VisualizationViewModel.onEvent(event)
                ↓
    VisualizationEngine.generateScript(event, architecture)
        → 生成按时间排序的 TimedAction 列表（L1-L8 全覆盖）
                ↓
    ScriptPlayer 按 playbackSpeed 驱动回放
        → 每 200ms / speed 一个 step
        → applyScriptAtProgress(progress) 累积式更新状态
                ↓
    Compose 重组 → VisualizationStage Composable
                ↓
    各层级 Canvas 组件根据状态独立绘制
```

**状态归约器（Reducer）设计**：
- `VisualizationViewModel.reduceAction(state, action)` 是单一状态变换入口。
- 每个 `AnimationAction` 子类对应一个不可变的 `copy()` 变换，避免清空重建导致的闪烁。
- 新 Action **平滑覆盖**旧字段：例如 `UpdateRegister` 只修改目标寄存器，保留其余寄存器状态。

**性能策略**：
- `Canvas` 的 `draw` lambda 在后台线程的 Skia 中执行，但状态更新在主线程。
- 使用 `remember { Path() }` 缓存静态路径（如内存网格线），避免每帧重建。
- 位格翻转动画使用 `Animatable<Float>` 控制 alpha/scale，而非每帧重算 Path。
- 大型内存网格启用裁剪：只绘制视口内单元格。
- **⚡ 栈帧滑动动画**：`StackView` 使用 `Animatable` 驱动 `progress`，在 `DrawScope` 中实时计算滑块坐标，避免触发额外重组。
- **⚡ 链表连线生长**：`LinkedListView` 的 Wire Overlay 使用独立 `Canvas` 覆盖层，与节点 Row 分离，减少不必要的节点重组。
- **⚡ 光标平滑移动**：`MemoryGrid` 追踪 `previousCursorAddress`，通过 `Animatable(0f→1f)` 插值实现 250ms 平滑过渡，不依赖连续重组。

### 7.2 数学工作台渲染（Compose Canvas）

```
用户选择模板/修改参数 → ViewModel 触发计算
                        ↓
    引擎生成 GeometryFrame 序列（或采样点数组）
                        ↓
    ViewModel 更新 UiState.shapes / UiState.frames
                        ↓
    Compose 重组 → Screen 中央视口
                        ↓
    Canvas {
        drawCoordinateGrid()          // 坐标轴与网格
        drawShapes(shapes)            // 点/线/面/矩形/圆
        drawLabels(labels)            // 数值标签
        drawAnimationOverlay(progress)// 动画进度遮罩
    }
                        ↓
    LaunchedEffect(isPlaying) {       // 60 FPS 动画驱动
        while (isPlaying && progress < 1f) {
            progress += 1f / totalFrames
            delay(16)
        }
    }
```

**与计算器可视化的差异**：
- 数学工作台 Canvas 负责坐标变换（世界坐标 → 屏幕坐标）、裁剪、透视投影。
- 计算器 VisualizationStage 不负责坐标变换，只负责绘制固定布局的位格和寄存器。

### 7.3 3D 图形渲染（OpenGL ES）— M6 阶段

```
用户输入表达式 → Engine 采样生成 VertexBuffer (FloatArray)
                        ↓
    GraphingViewModel 持有 VertexBuffer 引用
                        ↓
    GLSurfaceView.Renderer.onDrawFrame()
        ↓
    GLES20.glUseProgram(shaderProgram)
    GLES20.glUniformMatrix4fv(mvpMatrix, …)
    GLES20.glBindBuffer(GL_ARRAY_BUFFER, vboId)
    GLES20.glDrawArrays(GL_TRIANGLES, 0, vertexCount)
        ↓
    双缓冲 → SurfaceFlinger 合成
```

**交互同步**：
- 相机控制（旋转/平移/缩放）在 Compose 层通过 `pointerInput` 捕获手势，生成 `CameraState`。
- `CameraState` 通过 `StateFlow` 流向 `GraphRenderer`，在 `onDrawFrame` 中读取最新的 MVP 矩阵。
- 参数滑块变化 → 重新采样 → 更新 VBO → 请求 `requestRender()`。

---

## 8. 可视化分层

### 8.1 L1-L8（计算器模式）— 已实现

| 层级 | 组件 | 展示内容 | 动画表现形式 | 关键模型 |
|------|------|---------|-------------|---------|
| **L1: 布尔代数** | `LogicGateGrid` | 逻辑门 (AND/OR/XOR/NOT) 对每一位的运算 | 绿色信号流在逻辑门网格中流动；`signalProgress` 控制流过的光点 | `AnimationAction.UpdateLogicGates` |
| **L2: 数值表示** | `BitGrid` | int/float/double 的 IEEE 754 / 补码二进制展开 | 64 位格（8×8 或 1×64）；位翻转时颜色过渡；IEEE 754 模式下高亮符号位/指数位/尾数位（琥珀色三角标记） | `AnimationAction.UpdateBitGrid` + `UpdateBitGridHighlights` |
| **L3: 寄存器与ALU** | `RegisterBank` + `AluVisualizer` | 64-bit 寄存器组（架构相关命名） | 寄存器槽位高亮（`isHighlighted`）；16 组 4-bit Hex 块显示寄存器值；`DataPathVisual` 驱动绿色流光曲线（贝塞尔光点）从源寄存器流向目标 | `AnimationAction.UpdateRegister` + `UpdateDataPath` + `UpdateAluOperation` |
| **L4: 内存布局** | `MemoryGrid` + `StackView` | 内存网格（堆/数据段）+ 栈帧堆叠 | **内存**：绿色实心=已分配数据，绿色空心=指针/空闲槽位；光标读取头（琥珀色边框+三角）平滑移动；写入时白色脉冲边框。**栈**：PUSH 时方块从寄存器区滑入栈顶；POP 时滑出；SP 箭头同步移动并闪烁 | `AnimationAction.WriteMemory` + `PushStack/PopStack` + `UpdateStackPointer` + `UpdateStackAnimation` |
| **L5: 数据结构** | `LinkedListView` + `OperatorStackView` + `AstTreeView` | 链表节点、运算符栈、AST 生长 | **链表**：实心方块=数据节点，空心方块=指针节点；新节点插入时缩放淡入；指针连线使用 Canvas Overlay 绘制贝塞尔生长曲线（流光点+箭头头）。**AST**：`growthProgress` 控制节点逐个出现 | `AnimationAction.UpdateLinkedList` + `AnimateLinkedListWire` + `UpdateAstGrowth` |
| **L6: 指针与寻址** | `AddressBusView` | 内存地址（64-bit 虚拟地址）、段偏移、指针解引用 | SEG 方块 + OFF 方块 → ALU 拼接 → ADDR 结果方块；三段贝塞尔曲线绿色流光依次点亮；x86 展示段选择子+偏移量，ARM/RISC-V 展示基址+索引+偏移 | `AnimationAction.UpdateAddressBus` + `AnimateMemoryPointer` |
| **L7: 指令集抽象** | `InstructionPipeline` | 模拟 RISC-V/ARM64/x86-64 指令执行流 | FETCH→DECODE→EXECUTE→WRITEBACK 四阶段流水线；当前阶段高亮并显示架构相关助记符（如 ARM64 `STP X0, X1, [SP, #-16]!`） | `AnimationAction.UpdateInstructionPipeline` |
| **L8: 结果回显** | `DisplayBufferView` | 计算结果从寄存器→内存缓冲区→显示驱动 | `resultFlowProgress` 控制数据流汇聚；显示缓冲区同步光标位置与输入状态 | `AnimationAction.UpdateDisplayBuffer` + `UpdateResultFlow` |

### 8.2 G1-G4（图形与数学工作台）— 新增

| 层级 | 名称 | 展示内容 | 适用模块 |
|------|------|---------|---------|
| **G1: 坐标系与几何** | 坐标轴、网格、点/线/面/曲面的数学表示与屏幕映射 | 图形、微积分、微分方程 |
| **G2: 数值采样与离散化** | 采样点生成、网格细分、离散化策略（黎曼和矩形、 marching squares） | 图形、微积分 |
| **G3: 向量与张量** | 向量箭头、矩阵变换的基向量动画、特征向量方向、张量椭球 | 线性代数、微分方程 |
| **G4: 时间演化与相空间** | 参数动画轨迹、相图流形、解曲线随时间推进、PDE 场的帧序列 | 微积分、微分方程 |

**渲染实现**：基于 Compose `Canvas` 的自定义绘制，而非复用 `VisualizationStage`。原因：
- G1-G4 需要坐标变换、裁剪、透视投影等图形学操作，与 L1-L8 的"位格翻转+寄存器高亮"完全不同。
- 共享统一的视觉令牌（TerminalGreen `#00FF41`、方块圆角、Monospace 字体）。

---

## 9. 模板系统（Template System）

### 9.1 设计目标

- **降低门槛**：用户无需手写复杂方程，选择模板后填入参数即可。
- **统一可视化**：每个模板自带推荐的可视化配置（坐标系类型、采样范围、动画参数）。
- **可扩展**：新增模板只需实现 `MathTemplate` 接口并注册到模板库。

### 9.2 核心模型

```kotlin
sealed interface MathTemplate {
    val id: String
    val category: TemplateCategory       // GRAPHING / LINEAR_ALGEBRA / CALCULUS / DIFF_EQ
    val name: String
    val description: String
    val parameters: List<TemplateParameter>
    val defaultExpressions: List<String>
    val recommendedViewport: ViewportConfig?
}

data class TemplateParameter(
    val name: String,
    val symbol: String,
    val defaultValue: Double,
    val range: ClosedFloatingPointRange<Double>,
    val step: Double = 0.01
)
```

### 9.3 预置模板（当前已创建 9 个）

| 模板 ID | 名称 | 类别 | 参数 | 可视化 |
|---------|------|------|------|--------|
| `graphing.parabola` | 抛物线 | GRAPHING | a, b, c | 2D 笛卡尔 |
| `graphing.lissajous` | 李萨如曲线 | GRAPHING | A, B, a, b, δ | 2D 参数曲线 |
| `linalg.rotation_2d` | 二维旋转矩阵 | LINEAR_ALGEBRA | θ | 2D 向量变换动画 |
| `calculus.riemann` | 黎曼和 | CALCULUS | a, b, n | 矩形累加动画 |
| `calculus.taylor` | 泰勒展开 | CALCULUS | x₀, n | 多项式逼近动画 |
| `ode.decay` | 指数衰减 | ORDINARY_DIFF_EQ | y₀, λ | y-t 解曲线 + 方向场 |
| `ode.harmonic` | 简谐振动 | ORDINARY_DIFF_EQ | ω, A, φ | 相图 (y, y') |
| `ode.lotka_volterra` | 捕食者-猎物 | ORDINARY_DIFF_EQ | α, β, γ, δ | 相图 + 时序图 |
| `pde.heat_1d` | 一维热方程 | PARTIAL_DIFF_EQ | α, L | 温度场演化动画 |

---

## 10. 引擎扩展设计

### 10.1 矩阵引擎（`engine.matrix`）

- **数据模型**：`Matrix<T>` 使用二维 `Array<Array<T>>`，支持 `Double` / `Fraction` / `Complex`。
- **核心运算**：加法、乘法（带逐步步骤）、转置、行列式、逆矩阵、LU 分解、QR 分解、特征值（幂法 / Jacobi）、SVD。
- **与 Parser 的整合**：表达式中支持矩阵字面量（如 `[[1,2],[3,4]]`），Parser 生成 `Expression.MatrixLiteral` 节点（M6 阶段扩展）。

### 10.2 数值微积分（`engine.calculus`）

- **数值微分**：前向/后向/中心差分、Richardson 外推。
- **数值积分**：矩形法、梯形法、Simpson 法、Gauss-Legendre。
- **结果表示**：`CalculusResult` 封装数值结果 + 中间步骤 + `GeometryFrame` 序列（用于 Canvas 动画）。

### 10.3 ODE/PDE 求解器（`engine.ode` / `engine.pde`）

- **ODE**：Euler、改进 Euler、经典 RK4、自适应 RK-Fehlberg。
- **PDE（一维）**：热方程/波动方程的有限差分法（FTCS、Crank-Nicolson）。
- **结果表示**：`OdeSolution`（时间-值序列）和 `PdeSolution`（`TimeSlice` 序列，用于动画播放）。

---

## 11. 与现有代码的兼容策略

### 11.1 零破坏原则

- `CalculatorScreen`、`CalculatorViewModel`、`CalculatorMode`（STANDARD/SCIENTIFIC/PROGRAMMER/DATE）**不做结构性修改**（仅增加 `onToggleLevel` 参数透传）。
- `engine.parser` 和 `engine.evaluator` **向后兼容**。
- `VisualizationStage/VisualizationViewModel` 允许**扩展式修改**：新增字段使用默认值，新增 Action 类型在 `reduceAction` 中补充分支，不破坏现有调用方。
- **架构模型演进**：`Architecture` enum 从简单 `(displayName, registerNames)` 扩展为包含 `spRegisterName`、`fpRegisterName`、`stackGrowsDown`、`addressingMode`、`mnemonicPrefix` 的丰富模型，所有原有调用点通过默认参数保持兼容。

### 11.2 新增内容清单

| 层级 | 新增包/文件 | 状态 |
|------|-----------|------|
| domain | `model/matrix/`, `model/calculus/`, `model/equation/`, `model/template/` | ✅ 已创建骨架 |
| engine | `engine/matrix/`, `engine/calculus/`, `engine/ode/` | ✅ 已创建骨架 |
| presentation | `presentation/graphing/`, `presentation/linearalgebra/`, `presentation/calculus/`, `presentation/diffeq/` | ⏳ 占位（ViewModel 未实现） |
| ui | `ui/graphing/`, `ui/linearalgebra/`, `ui/calculus/`, `ui/diffeq/` | ✅ 占位 Screen 已创建 |
| navigation | `CalcoreNavHost.kt` 新增路由 | ✅ 已更新 |
| ui | `ui/components/DrawerMenu.kt` 新增菜单项 | ✅ 已更新 |

---

## 12. 性能预算

| 指标 | 目标 | 实现策略 |
|---|---|---|
| 按键 → 首帧动画 | ≤ 100ms | 预计算状态，`VisualizationEngine.generateScript` 纯 Kotlin 无 IO；延迟 16ms 启动协程 |
| 动画帧率 | 60 FPS | `delay(16)` + 裁剪绘制区域；`Animatable` 驱动避免连续重组 |
| 2D 函数采样+渲染 | ≤ 16ms | 自适应步长 + 后台 Coroutine 采样 |
| 3D 网格渲染 (10k 顶点) | ≤ 16ms | VBO + 视锥裁剪 + LOD（M6） |
| 矩阵运算 (≤10×10) | ≤ 50ms | `Dispatchers.Default` + 逐步动画分帧 |
| ODE 求解 (1000 步 RK4) | ≤ 200ms | `Dispatchers.Default`，结果流式回传 |
| 可视化 Canvas 绘制 | ≤ 8ms/帧 | Stack/LinkedList/Memory 均使用 `remember` 缓存 `TextMeasurer`；路径复用；避免在 `drawScope` 中创建对象 |
| 内存峰值 | ≤ 256MB | 大网格流式加载；历史记录限制 1000 条；`AnimationScript` 事件列表 ≤ 50 个 |
| APK 体积 | ≤ 50MB | 按需加载着色器；资源压缩 |

---

## 13. 实施路线图

| 阶段 | 目标 | 关键交付 | 状态 |
|------|------|---------|------|
| **M1** | 表达式引擎 | Lexer/Parser/AST/Evaluator、64-bit 高精度计算 | ✅ 完成 |
| **M2** | 计算器骨架 | Standard/Scientific/Programmer/Date 模式、历史记录、内存 | ✅ 完成 |
| **M3** | L1-L8 可视化引擎 | BitGrid、RegisterBank、MemoryGrid、StackView、AST、LinkedList、AddressBus、Pipeline、DisplayBuffer | ✅ 完成 |
| **M4** | 可视化动画规范 | PUSH/POP 滑动动画、指针连线生长、光标平滑移动、多架构切换、播放速度控制 | ✅ 完成 |
| **M5a** | 图形模式骨架 | GraphingScreen 三栏布局、2D 坐标系 Canvas、表达式列表管理、9 个模板 | ✅ 完成 |
| **M5b** | 图形模式功能 | 显函数、参数曲线、极坐标、不等式渲染；参数滑块；模板系统 | ✅ 完成 |
| **M6a** | 线性代数工作台 | 矩阵 Grid 编辑器、基本运算（加/乘/转置/逆/特征值）、2D 向量变换动画 | ✅ 完成 |
| **M6b** | 微积分可视化 | 极限动画、导数几何（切线/法线）、积分近似（黎曼/梯形/Simpson）、泰勒展开 | ✅ 完成 |
| **M7a** | 微分方程工作台 | ODE 数值求解（Euler/RK4/RKF45）、相图绘制、方向场、3 个预设模板 | ✅ 完成 |
| **M7b** | 高级可视化 | PDE 全场演化动画、G1-G4 可视化分层完善 | ⏳ 待实现 |
| **M8** | 模板与参数动画 | 12+ 预置模板、参数驱动、动画导出（快照/视频） | ⏳ 待实现 |
| **M9** | 跨模块联动与抛光 | 计算器结果发送到图形/微积分模块、3B1B 级动画质感调优、公开发布 | ⏳ 待实现 |

---

## 14. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Compose Canvas 3D 渲染性能不足 | 3D 曲面/参数方程卡顿 | M6 之前先用 Canvas 做 3D 线框预览；必要时引入 OpenGL ES |
| 矩阵/ODE 引擎在 UI 线程阻塞 | 大矩阵或复杂 ODE 导致 ANR | 所有数值计算在 `Dispatchers.Default` 协程中执行，结果通过 StateFlow 回传 |
| AST 扩展导致 exhaustive when 遗漏 | 编译错误或运行时崩溃 | 每次扩展 `Expression` 时，全局搜索所有 `when(expr)` 并补充新分支 |
| 模板系统过度设计 | 开发周期拉长 | 模板系统从最小可行版本开始（hardcoded 模板列表 + JSON 序列化），M8 再引入动态加载 |
| 多模块拆分过早 | 增加构建复杂度 | M9 之前保持单 `:app` 模块，仅通过包名约定隔离；M9 后按需拆分为 Gradle 模块 |

---

*本文档与 `PRD..md` 同步维护。任何架构级变更需同时更新两者。*

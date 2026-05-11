# Calcore Android 架构设计文档

*版本：v2.1*  
*日期：2026-05-12*  
*状态：草案*

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
│   │   └── GraphingViewModel.kt        (TODO)
│   ├── linearalgebra/
│   │   └── LinearAlgebraViewModel.kt   (TODO)
│   ├── calculus/
│   │   └── CalculusViewModel.kt        (TODO)
│   ├── diffeq/
│   │   └── DifferentialEquationsViewModel.kt (TODO)
│   └── visualization/
│       └── VisualizationViewModel.kt
│
├── ui                          # Compose UI 层
│   ├── calculator/             # CalculatorScreen、Keypads、Display
│   │   ├── CalculatorScreen.kt
│   │   ├── StandardKeypad.kt
│   │   ├── ScientificKeypad.kt
│   │   ├── ProgrammerKeypad.kt
│   │   └── ProgrammerDisplay.kt
│   ├── graphing/               # GraphingScreen、ExpressionList、Viewport2D/3D
│   │   └── GraphingScreen.kt   (TODO)
│   ├── linearalgebra/          # MatrixEditor、OperationToolbar、ResultPanel
│   │   └── LinearAlgebraScreen.kt (TODO)
│   ├── calculus/               # CalculusScreen、StepAnimator、GeometryCanvas
│   │   └── CalculusScreen.kt   (TODO)
│   ├── diffeq/                 # DifferentialEquationsScreen、PhasePortraitCanvas
│   │   └── DifferentialEquationsScreen.kt (TODO)
│   ├── visualization/          # BitGrid、RegisterBank、MemoryGrid、StackView、AstTreeView、TimelineScrubber
│   │   ├── VisualizationStage.kt
│   │   ├── BitGrid.kt
│   │   ├── RegisterBank.kt
│   │   ├── MemoryGrid.kt
│   │   ├── StackView.kt
│   │   ├── AstTreeView.kt
│   │   ├── TimelineScrubber.kt
│   │   └── BottomControlBar.kt
│   ├── components/             # 跨模块共享组件
│   │   ├── CalcoreButton.kt
│   │   ├── CalcoreDisplay.kt
│   │   ├── DrawerMenu.kt
│   │   ├── ScrollableKeypadContainer.kt
│   │   ├── ExpressionInputBar.kt        (TODO)
│   │   ├── ParameterSliderPanel.kt      (TODO)
│   │   ├── TemplateSelector.kt          (TODO)
│   │   └── CoordinateCanvas.kt          (TODO)
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

---

## 7. 渲染管线设计

### 7.1 2D 可视化舞台（Compose Canvas）— 计算器模式

```
用户按键 → ViewModel 生成 AnimationEvent
                ↓
    VisualizationViewModel 接收 Event
                ↓
    状态更新：uiState = uiState.copy(
                bitGridBits = newBits,
                registers = newRegs,
                currentAst = ast,
                evaluationProgress = progress
              )
                ↓
    Compose 重组 → VisualizationStage Composable
                ↓
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawBitGrid(bitGridBits)
        drawRegisterBank(registers)
        drawAstTree(astRoot)
        drawTimelineScrubber(progress)
    }
                ↓
    LaunchedEffect(animating) {
        while (animating) {
            progress += step
            delay(80)
        }
    }
```

**性能策略**：
- `Canvas` 的 `draw` lambda 在后台线程的 Skia 中执行，但状态更新在主线程。
- 使用 `remember { Path() }` 缓存静态路径（如内存网格线），避免每帧重建。
- 位格翻转动画使用 `Animatable<Float>` 控制 alpha/scale，而非每帧重算 Path。
- 大型内存网格启用裁剪：只绘制视口内单元格。

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

### 8.1 L1-L8（计算器模式）— 已有，保持不变

| 层级 | 展示内容 | 动画表现形式 |
|------|---------|-------------|
| **L1: 布尔代数** | 逻辑门 (AND/OR/XOR/NOT) 对每一位的运算 | 绿色信号流在逻辑门网格中流动 |
| **L2: 数值表示** | int / float / double 的 IEEE 754 / 补码二进制展开 | 位格翻转动画，符号位/指数位/尾数位高亮 |
| **L3: 寄存器与ALU** | 64-bit 寄存器组 (RISC-V/ARM64/x86-64 抽象模型) | 寄存器槽位高亮，数据搬运路径 |
| **L4: 内存布局** | 栈区、堆区、数据段、常量池 | 绿色实心/空心方块构成的内存网格 |
| **L5: 数据结构** | 表达式解析用的链表、操作数数组、运算符栈 | 链表节点用绿色方块串联，指针用箭头/连线 |
| **L6: 指针与寻址** | 内存地址、段偏移、指针解引用 | 地址总线动画，指针段位移拼接 |
| **L7: 指令集抽象** | 模拟 RISC-V / ARM64 / x86-64 的指令执行流 | 指令取指→译码→执行→写回的流水线动画 |
| **L8: 结果回显** | 计算结果从寄存器 → 内存缓冲区 → 显示驱动 | 数据流汇聚到显示区域 |

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

- `CalculatorScreen`、`CalculatorViewModel`、`CalculatorMode`（STANDARD/SCIENTIFIC/PROGRAMMER/DATE）**不做任何结构性修改**。
- `VisualizationStage/VisualizationViewModel`（L1-L8）**不做任何修改**。
- `engine.parser` 和 `engine.evaluator` **向后兼容**；新增 AST 节点类型（如 `MatrixLiteral`）时，全局搜索所有 `when(expr)` 并补充新分支。

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
| 按键 → 首帧动画 | ≤ 100ms | 预计算状态，延迟 16ms 启动协程 |
| 动画帧率 | 60 FPS | `delay(16)` + 裁剪绘制区域 |
| 2D 函数采样+渲染 | ≤ 16ms | 自适应步长 + 后台 Coroutine 采样 |
| 3D 网格渲染 (10k 顶点) | ≤ 16ms | VBO + 视锥裁剪 + LOD（M6） |
| 矩阵运算 (≤10×10) | ≤ 50ms | `Dispatchers.Default` + 逐步动画分帧 |
| ODE 求解 (1000 步 RK4) | ≤ 200ms | `Dispatchers.Default`，结果流式回传 |
| 内存峰值 | ≤ 256MB | 大网格流式加载；历史记录限制 1000 条 |
| APK 体积 | ≤ 50MB | 按需加载着色器；资源压缩 |

---

## 13. 实施路线图

| 阶段 | 目标 | 关键交付 |
|------|------|---------|
| **M5a** | 图形模式骨架 | GraphingScreen 三栏布局、2D 坐标系 Canvas、表达式列表管理 |
| **M5b** | 图形模式功能 | 显函数、参数曲线、极坐标、不等式渲染；参数滑块；模板系统骨架 |
| **M6a** | 线性代数工作台 | 矩阵 Grid 编辑器、基本运算、特征值 / SVD、2D 向量变换动画 |
| **M6b** | 微积分可视化 | 极限动画、导数几何（切线/法线/曲率圆）、积分近似（黎曼/梯形/Simpson） |
| **M7a** | 微分方程工作台 | ODE 数值求解（RK4）、相图绘制、方向场、PDE 有限差分 |
| **M7b** | 高级可视化 | PDE 全场演化动画、G1-G4 可视化分层完善 |
| **M8** | 模板与参数动画 | 12+ 预置模板、参数驱动、动画导出（快照/视频） |
| **M9** | 跨模块联动与抛光 | 计算器结果发送到图形/微积分模块、3B1B 级动画质感调优、公开发布 |

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

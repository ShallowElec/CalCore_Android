package com.cloveriris.calcore.engine.visualization

import com.cloveriris.calcore.domain.model.AnimationAction
import com.cloveriris.calcore.domain.model.AnimationEvent
import com.cloveriris.calcore.domain.model.AnimationScript
import com.cloveriris.calcore.domain.model.Architecture
import com.cloveriris.calcore.domain.model.MemoryOpType
import com.cloveriris.calcore.domain.model.PipelinePhase
import com.cloveriris.calcore.domain.model.TimedAction
import com.cloveriris.calcore.domain.usecase.EvaluateUseCase
import com.cloveriris.calcore.engine.parser.BinaryOperator
import com.cloveriris.calcore.engine.parser.Expression

/**
 * 可视化引擎 —— 纯 Kotlin，无 Android 依赖
 *
 * 将用户输入事件（AnimationEvent）转换为按时间排序的视觉动作脚本（AnimationScript），
 * 覆盖 L1-L8 全部可视化层级。
 *
 * 脚本的 tick 基准为 200ms，通过 VisualizationViewModel 的 playbackSpeed 控制实际播放速度。
 */
object VisualizationEngine {

    private val evaluator = EvaluateUseCase()

    fun generateScript(event: AnimationEvent, architecture: Architecture = Architecture.X86_64): AnimationScript {
        return when (event) {
            is AnimationEvent.DigitEntered -> generateDigitScript(event.digit, event.currentExpression, architecture)
            is AnimationEvent.OperatorEntered -> generateOperatorScript(event.operator, event.currentExpression, architecture)
            is AnimationEvent.Evaluated -> generateEvaluatedScript(event.expression, event.result, architecture)
            AnimationEvent.Clear -> generateClearScript(architecture)
            AnimationEvent.Backspace -> generateBackspaceScript(architecture)
            is AnimationEvent.MemoryOperation -> generateMemoryScript(event.type, architecture)
            AnimationEvent.DecimalEntered -> generateDecimalScript(architecture)
            is AnimationEvent.ExpressionParsed -> generateExpressionParsedScript(event.expression)
            is AnimationEvent.BitOperation -> generateBitOperationScript(event.op, event.left, event.right, event.result)
        }
    }

    // ==================== L1-L8 脚本生成器 ====================

    private fun generateDigitScript(
        digit: Char,
        expr: String,
        arch: Architecture
    ): AnimationScript {
        val ascii = digit.code
        val bits = longToBits(ascii.toLong())
        val actions = mutableListOf<TimedAction>()
        val spName = arch.spRegisterName

        // T0: 描述 + 位格展示 ASCII
        actions += TimedAction(0, AnimationAction.UpdateDescription("输入数字: $digit"))
        actions += TimedAction(0, AnimationAction.UpdateBitGrid(bits, "ASCII '$digit' = 0x%02X".format(ascii)))

        // T1: 寄存器装载（绿色高亮）
        actions += TimedAction(1, AnimationAction.UpdateRegister(0, ascii.toLong(), isHighlighted = true))
        actions += TimedAction(1, AnimationAction.UpdateDataPath("KEYBOARD", spName, progress = 0.3f))

        // T2: 段寄存器 → 偏移量拼接（L6 寻址）
        actions += TimedAction(2, AnimationAction.UpdateAddressBus(
            segment = 0x0007L,
            offset = 0x1000 + expr.length * 8L,
            fullAddress = 0x0007_1000L + expr.length * 8L,
            progress = 0.5f
        ))

        // T3: 内存写入（绿色实心方块）+ 地址总线完成
        actions += TimedAction(3, AnimationAction.WriteMemory(0x1000 + expr.length, ascii.toByte(), isAllocated = true, isPointer = false, isWriting = true))
        actions += TimedAction(3, AnimationAction.UpdateAddressBus(
            segment = 0x0007L,
            offset = 0x1000 + expr.length * 8L,
            fullAddress = 0x0007_1000L + expr.length * 8L,
            progress = 1.0f
        ))
        actions += TimedAction(3, AnimationAction.UpdateDataPath("KEYBOARD", spName, progress = 0.7f))

        // T4: 显示缓冲区更新 + 光标地址映射
        actions += TimedAction(4, AnimationAction.UpdateDisplayBuffer(expr, expr.length - 1, isTyping = true))
        actions += TimedAction(4, AnimationAction.UpdateCursorAddress(0x1000 + expr.length, expr.length - 1))

        // T5: 数据路径完成，寄存器取消高亮
        actions += TimedAction(5, AnimationAction.UpdateDataPath(spName, "MEM", progress = 1.0f))
        actions += TimedAction(5, AnimationAction.UpdateRegister(0, ascii.toLong(), isHighlighted = false))
        actions += TimedAction(5, AnimationAction.WriteMemory(0x1000 + expr.length, ascii.toByte(), isAllocated = true, isPointer = false, isWriting = false))

        // T6: 结果数据流汇聚
        actions += TimedAction(6, AnimationAction.UpdateResultFlow("REGISTER", "DISPLAY", progress = 0.5f))

        val total = 8L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    private fun generateOperatorScript(
        op: String,
        expr: String,
        arch: Architecture
    ): AnimationScript {
        val actions = mutableListOf<TimedAction>()
        val stack = extractOperators(expr)
        val spName = arch.spRegisterName

        actions += TimedAction(0, AnimationAction.UpdateDescription("输入运算符: $op"))

        // T0: 运算符压入运算符栈（L5）
        actions += TimedAction(0, AnimationAction.UpdateOperatorStack(stack, pushLabel = op, popCount = 0))

        // T1: AST 部分生长
        actions += TimedAction(1, AnimationAction.UpdateAstGrowth(null, progress = 0.3f))

        // T2: 运算符写入内存（作为指针/标记）
        actions += TimedAction(2, AnimationAction.WriteMemory(0x1001, op.first().code.toByte(), isAllocated = true, isPointer = true, isWriting = true))
        actions += TimedAction(2, AnimationAction.UpdateDataPath("KEYBOARD", spName, progress = 0.5f))

        // T3: 指令流水线 — FETCH（L7）
        actions += TimedAction(3, AnimationAction.UpdateInstructionPipeline(
            listOf(
                AnimationAction.PipelineStageData("FETCH", mnemonic(arch, "PUSH"), isActive = true, progress = 1.0f),
                AnimationAction.PipelineStageData("DECODE", "", isActive = false, progress = 0f),
                AnimationAction.PipelineStageData("EXECUTE", "", isActive = false, progress = 0f),
                AnimationAction.PipelineStageData("WRITEBACK", "", isActive = false, progress = 0f)
            )
        ))

        // T4: 栈操作动画 — PUSH 运算符到栈帧
        actions += TimedAction(4, AnimationAction.UpdateStackAnimation(
            operation = AnimationAction.StackOperationType.PUSH,
            progress = 0.5f,
            frameLabel = "op='$op'",
            frameValue = "0x%02X".format(op.first().code),
            registerName = spName
        ))
        actions += TimedAction(4, AnimationAction.UpdateStackPointer("${spName}-0x08", isHighlighted = true))

        // T5: PUSH 完成，栈帧加入
        actions += TimedAction(5, AnimationAction.PushStack("op='$op'", "0x%04X".format(op.first().code)))
        actions += TimedAction(5, AnimationAction.UpdateStackAnimation(
            operation = AnimationAction.StackOperationType.PUSH,
            progress = 1.0f,
            frameLabel = "op='$op'",
            frameValue = "0x%02X".format(op.first().code),
            registerName = spName
        ))
        actions += TimedAction(5, AnimationAction.UpdateStackPointer("${spName}-0x08", isHighlighted = false))

        // T6: 显示缓冲区更新
        actions += TimedAction(6, AnimationAction.UpdateDisplayBuffer(expr, expr.length - 1, isTyping = false))
        actions += TimedAction(6, AnimationAction.WriteMemory(0x1001, op.first().code.toByte(), isAllocated = true, isPointer = true, isWriting = false))

        val total = 8L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    private fun generateEvaluatedScript(
        expression: String,
        result: Double,
        arch: Architecture
    ): AnimationScript {
        val actions = mutableListOf<TimedAction>()
        val ast = evaluator.parse(expression).getOrNull()
        val rawBits = result.toRawBits()
        val bits = longToBits(rawBits)
        val op = extractMainOperator(expression) ?: "ADD"
        val spName = arch.spRegisterName

        // 提取操作数（用于分阶段展示）
        val (leftVal, rightVal) = extractOperands(ast, result)
        val leftBits = longToBits(leftVal.toRawBits())
        val rightBits = longToBits(rightVal.toRawBits())

        // ========== PHASE 1: INPUT / NUMERIC (L1-L2) T0-T3 ==========
        actions += TimedAction(0, AnimationAction.UpdateDescription("计算: $expression = $result"))
        actions += TimedAction(0, AnimationAction.EnterPhase(PipelinePhase.PHASE_INPUT, phaseDurationMs = 800L))

        // T0: 左操作数位格亮起
        actions += TimedAction(0, AnimationAction.UpdateBitGrid(leftBits, "LEFT OPERAND = $leftVal"))
        actions += TimedAction(1, AnimationAction.UpdateBitGridHighlights(
            highlightIndices = buildSet {
                add(63); addAll(52..62); addAll(0..51)
            },
            highlightColor = 0xFFFFA657.toInt()
        ))

        // T2: 右操作数位格亮起
        actions += TimedAction(2, AnimationAction.UpdateBitGrid(rightBits, "RIGHT OPERAND = $rightVal"))

        // T3: 退出输入阶段
        actions += TimedAction(3, AnimationAction.ExitPhase(
            PipelinePhase.PHASE_INPUT,
            "L1-L2: $leftVal, $rightVal loaded"
        ))

        // ========== PHASE 2: REGISTERS (L3) T4-T7 ==========
        actions += TimedAction(4, AnimationAction.EnterPhase(PipelinePhase.PHASE_REGISTERS, phaseDurationMs = 800L))

        // T4: RAX 装载左操作数
        actions += TimedAction(4, AnimationAction.UpdateRegister(0, leftVal.toLong(), isHighlighted = true))
        actions += TimedAction(4, AnimationAction.UpdateDataPath("KEYBOARD", arch.registerNames[0], progress = 0.5f))

        // T5: RBX 装载右操作数
        actions += TimedAction(5, AnimationAction.UpdateRegister(1, rightVal.toLong(), isHighlighted = true))
        actions += TimedAction(5, AnimationAction.UpdateDataPath("KEYBOARD", arch.registerNames[1], progress = 0.5f))

        // T6: 数据路径 RAX→ALU, RBX→ALU
        actions += TimedAction(6, AnimationAction.UpdateDataPath(arch.registerNames[0], "ALU", progress = 0.7f))
        actions += TimedAction(6, AnimationAction.UpdateDataPath(arch.registerNames[1], "ALU", progress = 0.7f))
        actions += TimedAction(6, AnimationAction.UpdateAluOperation(op, leftVal.toLong(), rightVal.toLong(), result.toLong(), isActive = false))

        // T7: 退出寄存器阶段，ALU 待机
        actions += TimedAction(7, AnimationAction.ExitPhase(
            PipelinePhase.PHASE_REGISTERS,
            "L3: ${arch.registerNames[0]}←$leftVal, ${arch.registerNames[1]}←$rightVal"
        ))

        // ========== PHASE 3: MEMORY (L4) T8-T11 ==========
        actions += TimedAction(8, AnimationAction.EnterPhase(PipelinePhase.PHASE_MEMORY, phaseDurationMs = 800L))

        // T8: 内存写入左操作数 + 栈 PUSH
        actions += TimedAction(8, AnimationAction.WriteMemory(0x1000, leftVal.toRawBits().toByte(), isAllocated = true, isPointer = false, isWriting = true))
        actions += TimedAction(8, AnimationAction.PushStack("left=$leftVal", "0x1000"))
        actions += TimedAction(8, AnimationAction.UpdateStackAnimation(
            operation = AnimationAction.StackOperationType.PUSH,
            progress = 0.5f, frameLabel = "left=$leftVal", frameValue = "0x1000", registerName = spName
        ))
        actions += TimedAction(8, AnimationAction.UpdateStackPointer("$spName-0x08", isHighlighted = true))

        // T9: 内存写入右操作数 + 栈 PUSH
        actions += TimedAction(9, AnimationAction.WriteMemory(0x1008, rightVal.toRawBits().toByte(), isAllocated = true, isPointer = false, isWriting = true))
        actions += TimedAction(9, AnimationAction.PushStack("right=$rightVal", "0x1008"))
        actions += TimedAction(9, AnimationAction.UpdateStackAnimation(
            operation = AnimationAction.StackOperationType.PUSH,
            progress = 1.0f, frameLabel = "right=$rightVal", frameValue = "0x1008", registerName = spName
        ))
        actions += TimedAction(9, AnimationAction.UpdateStackPointer("$spName-0x10", isHighlighted = false))

        // T10: 内存指针生长动画
        actions += TimedAction(10, AnimationAction.AnimateMemoryPointer(0x1000, 0x1008, progress = 0.6f))
        actions += TimedAction(10, AnimationAction.WriteMemory(0x1000, leftVal.toRawBits().toByte(), isAllocated = true, isPointer = false, isWriting = false))
        actions += TimedAction(10, AnimationAction.WriteMemory(0x1008, rightVal.toRawBits().toByte(), isAllocated = true, isPointer = false, isWriting = false))

        // T11: 退出内存阶段
        actions += TimedAction(11, AnimationAction.ExitPhase(
            PipelinePhase.PHASE_MEMORY,
            "L4: mem[0x1000]=$leftVal, mem[0x1008]=$rightVal, stack pushed"
        ))

        // ========== PHASE 4: PARSE (L5) T12-T16 ==========
        actions += TimedAction(12, AnimationAction.EnterPhase(PipelinePhase.PHASE_PARSE, phaseDurationMs = 1000L))

        // T12: AST 根节点出现
        actions += TimedAction(12, AnimationAction.UpdateAstGrowth(ast, progress = 0.2f))
        actions += TimedAction(12, AnimationAction.UpdateOperatorStack(emptyList(), pushLabel = null, popCount = 0))

        // T13: AST 左子节点生长 + 运算符栈 push 左操作数
        actions += TimedAction(13, AnimationAction.UpdateAstGrowth(ast, progress = 0.5f))
        actions += TimedAction(13, AnimationAction.UpdateOperatorStack(listOf(leftVal.toString())))

        // T14: AST 右子节点生长 + 运算符栈 push 运算符
        actions += TimedAction(14, AnimationAction.UpdateAstGrowth(ast, progress = 0.8f))
        actions += TimedAction(14, AnimationAction.UpdateOperatorStack(listOf(leftVal.toString(), op)))

        // T15: AST 完全生长 + 运算符栈完整 + 链表节点插入
        actions += TimedAction(15, AnimationAction.UpdateAstGrowth(ast, progress = 1.0f))
        actions += TimedAction(15, AnimationAction.UpdateOperatorStack(listOf(leftVal.toString(), op, rightVal.toString())))
        actions += TimedAction(15, AnimationAction.UpdateLinkedList(listOf(
            AnimationAction.LinkedListNodeData("n1", leftVal.toString(), "n2", isNew = true),
            AnimationAction.LinkedListNodeData("n2", op, "n3", isNew = true),
            AnimationAction.LinkedListNodeData("n3", rightVal.toString(), null, isNew = true)
        )))
        actions += TimedAction(15, AnimationAction.AnimateLinkedListWire("n1", "n2", progress = 0.7f))
        actions += TimedAction(15, AnimationAction.AnimateLinkedListWire("n2", "n3", progress = 0.7f))

        // T16: 退出解析阶段
        actions += TimedAction(16, AnimationAction.ExitPhase(
            PipelinePhase.PHASE_PARSE,
            "L5: AST built, op-stack [$leftVal, $op, $rightVal]"
        ))

        // ========== PHASE 5: EXECUTE (L6-L7) T17-T20 ==========
        actions += TimedAction(17, AnimationAction.EnterPhase(PipelinePhase.PHASE_EXECUTE, phaseDurationMs = 800L))

        // T17: 地址总线拼接
        actions += TimedAction(17, AnimationAction.UpdateAddressBus(
            segment = 0x0007L, offset = 0x1000L,
            fullAddress = 0x0007_1000L, progress = 0.5f
        ))
        actions += TimedAction(17, AnimationAction.UpdateInstructionPipeline(listOf(
            AnimationAction.PipelineStageData("FETCH", mnemonic(arch, "EVAL"), isActive = true, progress = 1.0f),
            AnimationAction.PipelineStageData("DECODE", op.lowercase(), isActive = false, progress = 0f),
            AnimationAction.PipelineStageData("EXECUTE", "", isActive = false, progress = 0f),
            AnimationAction.PipelineStageData("WRITEBACK", "", isActive = false, progress = 0f)
        )))

        // T18: 指令流水线 DECODE→EXECUTE 激活
        actions += TimedAction(18, AnimationAction.UpdateInstructionPipeline(listOf(
            AnimationAction.PipelineStageData("FETCH", mnemonic(arch, "EVAL"), isActive = true, progress = 1.0f),
            AnimationAction.PipelineStageData("DECODE", op.lowercase(), isActive = true, progress = 1.0f),
            AnimationAction.PipelineStageData("EXECUTE", "", isActive = true, progress = 0.5f),
            AnimationAction.PipelineStageData("WRITEBACK", "", isActive = false, progress = 0f)
        )))
        actions += TimedAction(18, AnimationAction.UpdateAddressBus(
            segment = 0x0007L, offset = 0x1000L,
            fullAddress = 0x0007_1000L, progress = 1.0f
        ))

        // T19: ALU 运算脉冲 + WRITEBACK
        actions += TimedAction(19, AnimationAction.UpdateAluOperation(op, leftVal.toLong(), rightVal.toLong(), result.toLong(), isActive = true))
        actions += TimedAction(19, AnimationAction.UpdateInstructionPipeline(listOf(
            AnimationAction.PipelineStageData("FETCH", mnemonic(arch, "EVAL"), isActive = true, progress = 1.0f),
            AnimationAction.PipelineStageData("DECODE", op.lowercase(), isActive = true, progress = 1.0f),
            AnimationAction.PipelineStageData("EXECUTE", "", isActive = true, progress = 1.0f),
            AnimationAction.PipelineStageData("WRITEBACK", "", isActive = true, progress = 1.0f)
        )))

        // T20: 退出执行阶段
        actions += TimedAction(20, AnimationAction.ExitPhase(
            PipelinePhase.PHASE_EXECUTE,
            "L6-L7: $op executed, result=$result"
        ))

        // ========== PHASE 6: OUTPUT (L8) T21-T24 ==========
        actions += TimedAction(21, AnimationAction.EnterPhase(PipelinePhase.PHASE_OUTPUT, phaseDurationMs = 800L))

        // T21: 结果位格 + 数据路径 ALU→RAX
        actions += TimedAction(21, AnimationAction.UpdateBitGrid(bits, "RESULT IEEE 754 = $result"))
        actions += TimedAction(21, AnimationAction.UpdateDataPath("ALU", arch.registerNames[0], progress = 0.5f))
        actions += TimedAction(21, AnimationAction.UpdateRegister(0, result.toLong(), isHighlighted = true))

        // T22: 数据路径 RAX→MEMORY→DISPLAY
        actions += TimedAction(22, AnimationAction.UpdateDataPath(arch.registerNames[0], "MEM", progress = 0.7f))
        actions += TimedAction(22, AnimationAction.WriteMemory(0x1002, rawBits.toByte(), isAllocated = true, isPointer = false, isWriting = true))

        // T23: 显示缓冲区更新
        actions += TimedAction(23, AnimationAction.UpdateResultFlow("REGISTER", "DISPLAY", progress = 1.0f))
        actions += TimedAction(23, AnimationAction.UpdateDisplayBuffer(result.toString(), result.toString().length, isTyping = false))
        actions += TimedAction(23, AnimationAction.WriteMemory(0x1002, rawBits.toByte(), isAllocated = true, isPointer = false, isWriting = false))

        // T24: 寄存器取消高亮，退出输出阶段
        actions += TimedAction(24, AnimationAction.UpdateRegister(0, result.toLong(), isHighlighted = false))
        actions += TimedAction(24, AnimationAction.ExitPhase(
            PipelinePhase.PHASE_OUTPUT,
            "L8: result displayed"
        ))

        val total = 25L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    private fun generateClearScript(arch: Architecture): AnimationScript {
        val actions = mutableListOf<TimedAction>()

        actions += TimedAction(0, AnimationAction.UpdateDescription("清除"))
        actions += TimedAction(0, AnimationAction.ClearAll)
        // 重置所有阶段状态
        PipelinePhase.orderedValues().reversed().forEach { phase ->
            actions += TimedAction(0, AnimationAction.ExitPhase(phase, ""))
        }
        actions += TimedAction(1, AnimationAction.UpdateBitGrid(List(64) { false }, "CLEARED"))
        actions += TimedAction(2, AnimationAction.UpdateDisplayBuffer("", 0, isTyping = false))
        actions += TimedAction(3, AnimationAction.UpdateOperatorStack(emptyList()))
        actions += TimedAction(3, AnimationAction.UpdateInstructionPipeline(emptyList()))
        actions += TimedAction(4, AnimationAction.UpdateStackPointer(arch.spRegisterName, isHighlighted = false))

        val total = 5L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    private fun generateBackspaceScript(arch: Architecture): AnimationScript {
        val actions = mutableListOf<TimedAction>()

        actions += TimedAction(0, AnimationAction.UpdateDescription("退格"))
        actions += TimedAction(0, AnimationAction.UpdateCursorAddress(0x1000, 0))
        actions += TimedAction(1, AnimationAction.WriteMemory(0x1000, 0, isAllocated = false, isPointer = false, isWriting = true))
        actions += TimedAction(2, AnimationAction.UpdateDisplayBuffer("", 0, isTyping = true))
        actions += TimedAction(3, AnimationAction.WriteMemory(0x1000, 0, isAllocated = false, isPointer = false, isWriting = false))

        val total = 4L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    private fun generateMemoryScript(type: MemoryOpType, arch: Architecture): AnimationScript {
        val desc = when (type) {
            MemoryOpType.STORE -> "内存存储"
            MemoryOpType.RECALL -> "内存读取"
            MemoryOpType.ADD -> "内存加"
            MemoryOpType.SUBTRACT -> "内存减"
            MemoryOpType.CLEAR -> "内存清除"
        }
        val actions = mutableListOf<TimedAction>()
        val spName = arch.spRegisterName

        actions += TimedAction(0, AnimationAction.UpdateDescription(desc))
        actions += TimedAction(0, AnimationAction.UpdateDataPath("MEM", spName, progress = 0.3f))

        // T1: 栈 PUSH 内存操作帧
        actions += TimedAction(1, AnimationAction.UpdateStackAnimation(
            operation = AnimationAction.StackOperationType.PUSH,
            progress = 0.5f,
            frameLabel = "MEM $type",
            frameValue = "0x2000",
            registerName = spName
        ))
        actions += TimedAction(1, AnimationAction.UpdateStackPointer("${spName}-0x08", isHighlighted = true))

        // T2: 内存写入 + 栈帧加入
        actions += TimedAction(2, AnimationAction.WriteMemory(0x2000, 0, isAllocated = true, isPointer = false, isWriting = true))
        actions += TimedAction(2, AnimationAction.PushStack("MEM $type", "0x2000"))
        actions += TimedAction(2, AnimationAction.UpdateStackAnimation(
            operation = AnimationAction.StackOperationType.PUSH,
            progress = 1.0f,
            frameLabel = "MEM $type",
            frameValue = "0x2000",
            registerName = spName
        ))

        // T3: 数据路径完成
        actions += TimedAction(3, AnimationAction.UpdateDataPath("MEM", spName, progress = 1.0f))
        actions += TimedAction(3, AnimationAction.UpdateStackPointer("${spName}-0x08", isHighlighted = false))
        actions += TimedAction(3, AnimationAction.WriteMemory(0x2000, 0, isAllocated = true, isPointer = false, isWriting = false))

        val total = 4L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    private fun generateDecimalScript(arch: Architecture): AnimationScript {
        val actions = mutableListOf<TimedAction>()
        val spName = arch.spRegisterName

        actions += TimedAction(0, AnimationAction.UpdateDescription("输入小数点"))
        actions += TimedAction(0, AnimationAction.UpdateBitGrid(List(64) { false }, "DECIMAL '.'"))
        actions += TimedAction(1, AnimationAction.UpdateRegister(0, '.'.code.toLong(), isHighlighted = true))
        actions += TimedAction(2, AnimationAction.WriteMemory(0x1000, '.'.code.toByte(), isAllocated = true, isPointer = true, isWriting = true))
        actions += TimedAction(2, AnimationAction.UpdateDataPath("KEYBOARD", spName, progress = 0.5f))
        actions += TimedAction(3, AnimationAction.UpdateRegister(0, '.'.code.toLong(), isHighlighted = false))
        actions += TimedAction(3, AnimationAction.WriteMemory(0x1000, '.'.code.toByte(), isAllocated = true, isPointer = true, isWriting = false))
        actions += TimedAction(3, AnimationAction.UpdateDataPath("KEYBOARD", spName, progress = 1.0f))

        val total = 4L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    private fun generateExpressionParsedScript(expression: String): AnimationScript {
        val ast = evaluator.parse(expression).getOrNull()
        val actions = mutableListOf<TimedAction>()

        actions += TimedAction(0, AnimationAction.UpdateDescription("解析表达式"))
        actions += TimedAction(0, AnimationAction.UpdateAstGrowth(ast, progress = 0.5f))
        actions += TimedAction(2, AnimationAction.UpdateOperatorStack(extractOperators(expression)))

        val total = 3L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    private fun generateBitOperationScript(
        op: String,
        left: Long,
        right: Long,
        result: Long
    ): AnimationScript {
        val leftBits = longToBits(left)
        val rightBits = longToBits(right)
        val resultBits = longToBits(result)
        val gateType = when (op.uppercase()) {
            "AND", "&" -> AnimationAction.LogicGateType.AND
            "OR", "|" -> AnimationAction.LogicGateType.OR
            "XOR", "^" -> AnimationAction.LogicGateType.XOR
            "NOT", "~" -> AnimationAction.LogicGateType.NOT
            else -> AnimationAction.LogicGateType.AND
        }
        val actions = mutableListOf<TimedAction>()

        actions += TimedAction(0, AnimationAction.UpdateDescription("位运算: $op"))
        actions += TimedAction(0, AnimationAction.UpdateBitGrid(leftBits, "LEFT"))
        actions += TimedAction(1, AnimationAction.UpdateBitGrid(rightBits, "RIGHT"))
        actions += TimedAction(2, AnimationAction.UpdateLogicGates(gateType, leftBits, rightBits, resultBits, signalProgress = 1.0f))
        actions += TimedAction(4, AnimationAction.UpdateBitGrid(resultBits, "RESULT"))
        actions += TimedAction(5, AnimationAction.UpdateRegister(0, result, isHighlighted = true))
        actions += TimedAction(6, AnimationAction.UpdateRegister(0, result, isHighlighted = false))

        val total = 7L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    // ==================== 工具方法 ====================

    private fun longToBits(value: Long): List<Boolean> {
        return List(64) { i -> ((value shr i) and 1L) == 1L }
    }

    /**
     * 从 AST 提取左右操作数。
     * 对于二元表达式，提取 left 和 right 的数值；
     * 对于非二元表达式，使用 result/2 和 result/2 作为默认值。
     */
    private fun extractOperands(ast: Expression?, result: Double): Pair<Double, Double> {
        return when (ast) {
            is Expression.Binary -> {
                val left = evaluateLiteral(ast.left)
                val right = evaluateLiteral(ast.right)
                Pair(left, right)
            }
            is Expression.Unary -> {
                val operand = evaluateLiteral(ast.operand)
                Pair(0.0, operand)
            }
            else -> {
                // 无法解析，用结果拆分作为示意
                Pair(result / 2.0, result / 2.0)
            }
        }
    }

    private fun evaluateLiteral(expr: Expression?): Double {
        return when (expr) {
            is Expression.NumberLiteral -> expr.value
            is Expression.ConstantRef -> expr.value
            is Expression.Binary -> evaluateLiteral(expr.left) // 简化处理
            is Expression.Unary -> evaluateLiteral(expr.operand)
            else -> 0.0
        }
    }

    private fun extractOperators(expression: String): List<String> {
        return expression.filter { it in "+-×÷*/^" }.map { it.toString() }
    }

    private fun extractMainOperator(expression: String): String? {
        val ast = evaluator.parse(expression).getOrNull() ?: return null
        return extractOpFromAst(ast)
    }

    private fun extractOpFromAst(expr: Expression?): String? {
        return when (expr) {
            is Expression.Binary -> when (expr.operator) {
                BinaryOperator.ADD -> "ADD"
                BinaryOperator.SUBTRACT -> "SUB"
                BinaryOperator.MULTIPLY -> "MUL"
                BinaryOperator.DIVIDE -> "DIV"
                BinaryOperator.POWER -> "POW"
            }
            is Expression.Unary -> "NEG"
            is Expression.FunctionCall -> expr.name.uppercase()
            else -> null
        }
    }

    /** 根据架构生成指令助记符 */
    private fun mnemonic(arch: Architecture, op: String): String {
        return when (arch) {
            Architecture.X86_64 -> when (op) {
                "PUSH" -> "PUSH"
                "POP" -> "POP"
                "EVAL" -> "CALL eval"
                else -> op.uppercase()
            }
            Architecture.ARM64 -> when (op) {
                "PUSH" -> "STP X0, X1, [SP, #-16]!"
                "POP" -> "LDP X0, X1, [SP], #16"
                "EVAL" -> "BL eval"
                else -> op.uppercase()
            }
            Architecture.RISC_V -> when (op) {
                "PUSH" -> "ADDI SP, SP, -16"
                "POP" -> "ADDI SP, SP, 16"
                "EVAL" -> "JAL eval"
                else -> op.uppercase()
            }
        }
    }
}

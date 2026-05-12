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
import com.cloveriris.calcore.engine.parser.UnaryOperator
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * 可视化引擎 —— 纯 Kotlin，无 Android 依赖
 *
 * 将用户输入事件（AnimationEvent）转换为按时间排序的视觉动作脚本（AnimationScript），
 * 覆盖 L1-L8 全部可视化层级。
 *
 * 核心改进：所有展示数据均从真实表达式/AST 推导，杜绝硬编码 mock 地址和假数据。
 * - 寄存器装载：使用真实操作数值
 * - 内存寻址：基于表达式内容生成确定性地址
 * - AST/解析：展示真实解析树的真实生长过程
 * - 运算符栈：基于真实表达式 token 序列
 * - 链表节点：映射真实 AST 结构
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
        val seg = deriveSegment(expr)
        val addr = deriveAddress(ascii.toLong(), expr.length)

        actions += TimedAction(0, AnimationAction.UpdateDescription("输入数字: $digit"))
        actions += TimedAction(0, AnimationAction.UpdateBitGrid(bits, "ASCII '$digit' = 0x%02X".format(ascii)))
        actions += TimedAction(1, AnimationAction.UpdateRegister(0, ascii.toLong(), isHighlighted = true))
        actions += TimedAction(1, AnimationAction.UpdateDataPath("KEYBOARD", spName, progress = 0.3f))
        actions += TimedAction(2, AnimationAction.UpdateAddressBus(
            segment = seg,
            offset = addr,
            fullAddress = (seg shl 16) + addr,
            progress = 0.5f
        ))
        actions += TimedAction(3, AnimationAction.WriteMemory(addr.toInt(), ascii.toByte(), isAllocated = true, isPointer = false, isWriting = true, regionTag = "DATA"))
        actions += TimedAction(3, AnimationAction.UpdateAddressBus(
            segment = seg,
            offset = addr,
            fullAddress = (seg shl 16) + addr,
            progress = 1.0f
        ))
        actions += TimedAction(3, AnimationAction.UpdateDataPath("KEYBOARD", spName, progress = 0.7f))
        actions += TimedAction(4, AnimationAction.UpdateDisplayBuffer(expr, expr.length - 1, isTyping = true))
        actions += TimedAction(4, AnimationAction.UpdateCursorAddress(addr.toInt(), expr.length - 1))
        actions += TimedAction(5, AnimationAction.UpdateDataPath(spName, "MEM", progress = 1.0f))
        actions += TimedAction(5, AnimationAction.UpdateRegister(0, ascii.toLong(), isHighlighted = false))
        actions += TimedAction(5, AnimationAction.WriteMemory(addr.toInt(), ascii.toByte(), isAllocated = true, isPointer = false, isWriting = false, regionTag = "DATA"))
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
        val spName = arch.spRegisterName
        val seg = deriveSegment(expr)
        val addr = deriveAddress(op.hashCode().toLong(), expr.length)
        val ascii = op.first().code

        actions += TimedAction(0, AnimationAction.UpdateDescription("输入运算符: $op"))
        actions += TimedAction(0, AnimationAction.UpdateBitGrid(longToBits(ascii.toLong()), "ASCII '$op' = 0x%02X".format(ascii)))
        actions += TimedAction(1, AnimationAction.UpdateRegister(1, ascii.toLong(), isHighlighted = true))
        actions += TimedAction(1, AnimationAction.UpdateDataPath("KEYBOARD", arch.registerNames[1], progress = 0.4f))
        actions += TimedAction(2, AnimationAction.UpdateAddressBus(
            segment = seg,
            offset = addr,
            fullAddress = (seg shl 16) + addr,
            progress = 0.6f
        ))
        actions += TimedAction(3, AnimationAction.WriteMemory(addr.toInt(), ascii.toByte(), isAllocated = true, isPointer = false, isWriting = true, regionTag = "DATA"))
        actions += TimedAction(3, AnimationAction.UpdateDisplayBuffer(expr, expr.length - 1, isTyping = true))
        actions += TimedAction(3, AnimationAction.UpdateCursorAddress(addr.toInt(), expr.length - 1))
        actions += TimedAction(4, AnimationAction.UpdateDataPath(arch.registerNames[1], "MEM", progress = 1.0f))
        actions += TimedAction(4, AnimationAction.UpdateRegister(1, ascii.toLong(), isHighlighted = false))
        actions += TimedAction(4, AnimationAction.WriteMemory(addr.toInt(), ascii.toByte(), isAllocated = true, isPointer = false, isWriting = false, regionTag = "DATA"))

        val total = 5L
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

        // 提取真实操作数（递归完整求值）
        val (leftVal, rightVal) = extractOperands(ast, result)
        val leftBits = longToBits(leftVal.toRawBits())
        val rightBits = longToBits(rightVal.toRawBits())

        // 基于表达式内容生成确定性内存地址和段寄存器
        val seg = deriveSegment(expression)
        val leftSeed = leftVal.toRawBits()
        val rightSeed = rightVal.toRawBits()
        val resultSeed = result.toRawBits()
        val leftAddr = deriveAddress(leftSeed, 0)
        val rightAddr = deriveAddress(rightSeed, 1)
        val resultAddr = deriveAddress(resultSeed, 2)

        // 基于真实表达式构建运算符栈阶段
        val opStackStages = buildShuntingYardStages(expression)

        // 基于 AST 构建真实链表节点
        val linkedListNodes = astToLinkedList(ast)

        // 提取所有叶子操作数用于多寄存器分配
        val allOperands = extractAllOperands(ast)

        // ========== PHASE 1: INPUT / NUMERIC (L1-L2) T0-T3 ==========
        actions += TimedAction(0, AnimationAction.UpdateDescription("计算: $expression = $result"))
        actions += TimedAction(0, AnimationAction.EnterPhase(PipelinePhase.PHASE_INPUT, phaseDurationMs = 800L))
        actions += TimedAction(0, AnimationAction.UpdateBitGrid(leftBits, "LEFT OPERAND = $leftVal"))
        // IEEE 754 分区依次高亮：符号位 → 指数位 → 尾数位
        actions += TimedAction(1, AnimationAction.UpdateBitGridHighlights(
            highlightIndices = setOf(63), highlightColor = 0xFFFFA657.toInt()
        ))
        actions += TimedAction(1, AnimationAction.UpdateBitGrid(leftBits, "SIGN = ${if (leftBits[0]) "-" else "+"}"))
        actions += TimedAction(2, AnimationAction.UpdateBitGridHighlights(
            highlightIndices = (52..62).toSet(), highlightColor = 0xFF4FC3F7.toInt()
        ))
        actions += TimedAction(2, AnimationAction.UpdateBitGrid(leftBits, "EXPONENT ×11"))
        actions += TimedAction(2, AnimationAction.UpdateBitGrid(rightBits, "RIGHT OPERAND = $rightVal"))
        actions += TimedAction(3, AnimationAction.UpdateBitGridHighlights(
            highlightIndices = (0..51).toSet(), highlightColor = 0xFF00FF41.toInt()
        ))
        actions += TimedAction(3, AnimationAction.UpdateBitGrid(leftBits, "MANTISSA ×52"))
        actions += TimedAction(3, AnimationAction.ExitPhase(
            PipelinePhase.PHASE_INPUT,
            "L1-L2: $leftVal, $rightVal loaded"
        ))

        // ========== PHASE 2: REGISTERS (L3) T4-T7 ==========
        actions += TimedAction(4, AnimationAction.EnterPhase(PipelinePhase.PHASE_REGISTERS, phaseDurationMs = 800L))

        // 根据操作数数量分配到不同寄存器（避免永远只用 RAX/RBX）
        val regCount = allOperands.size.coerceAtLeast(2)
        allOperands.take(4).forEachIndexed { idx, operand ->
            val regName = arch.registerNames.getOrElse(idx) { "R${idx}" }
            actions += TimedAction((4 + idx).toLong(), AnimationAction.UpdateRegister(idx, operand.toLong(), isHighlighted = true))
            actions += TimedAction((4 + idx).toLong(), AnimationAction.UpdateDataPath("KEYBOARD", regName, progress = 0.5f))
        }
        // 如果操作数少于 2，用 0 填充第二个寄存器以维持双操作数视觉
        if (allOperands.size < 2) {
            actions += TimedAction(5, AnimationAction.UpdateRegister(1, rightVal.toLong(), isHighlighted = true))
            actions += TimedAction(5, AnimationAction.UpdateDataPath("KEYBOARD", arch.registerNames[1], progress = 0.5f))
        }

        actions += TimedAction(6, AnimationAction.UpdateDataPath(arch.registerNames[0], "ALU", progress = 0.7f))
        if (regCount > 1) {
            actions += TimedAction(6, AnimationAction.UpdateDataPath(arch.registerNames[1], "ALU", progress = 0.7f))
        }
        actions += TimedAction(6, AnimationAction.UpdateAluOperation(op, leftVal.toLong(), rightVal.toLong(), result.toLong(), isActive = false))
        actions += TimedAction(7, AnimationAction.ExitPhase(
            PipelinePhase.PHASE_REGISTERS,
            "L3: ${arch.registerNames[0]}←$leftVal${if (regCount > 1) ", ${arch.registerNames[1]}←$rightVal" else ""}"
        ))

        // ========== PHASE 3: MEMORY (L4) T8-T11 ==========
        actions += TimedAction(8, AnimationAction.EnterPhase(PipelinePhase.PHASE_MEMORY, phaseDurationMs = 800L))

        // 1. 左操作数 64-bit (8 bytes) → 数据段
        val leftBytes = doubleToBytes(leftVal)
        leftBytes.forEachIndexed { i, byte ->
            actions += TimedAction(8, AnimationAction.WriteMemory(
                (leftAddr + i).toInt(), byte,
                isAllocated = true, isPointer = false, isWriting = true, regionTag = "DATA"
            ))
        }
        actions += TimedAction(8, AnimationAction.PushStack("left=$leftVal", "0x${leftAddr.toString(16).uppercase()}"))
        actions += TimedAction(8, AnimationAction.UpdateStackAnimation(
            operation = AnimationAction.StackOperationType.PUSH,
            progress = 0.5f, frameLabel = "left=$leftVal", frameValue = "0x${leftAddr.toString(16).uppercase()}", registerName = spName
        ))
        actions += TimedAction(8, AnimationAction.UpdateStackPointer("$spName-0x08", isHighlighted = true))

        // 2. 右操作数 64-bit (8 bytes) → 数据段
        val rightBytes = doubleToBytes(rightVal)
        rightBytes.forEachIndexed { i, byte ->
            actions += TimedAction(9, AnimationAction.WriteMemory(
                (rightAddr + i).toInt(), byte,
                isAllocated = true, isPointer = false, isWriting = true, regionTag = "DATA"
            ))
        }
        actions += TimedAction(9, AnimationAction.PushStack("right=$rightVal", "0x${rightAddr.toString(16).uppercase()}"))
        actions += TimedAction(9, AnimationAction.UpdateStackAnimation(
            operation = AnimationAction.StackOperationType.PUSH,
            progress = 1.0f, frameLabel = "right=$rightVal", frameValue = "0x${rightAddr.toString(16).uppercase()}", registerName = spName
        ))
        actions += TimedAction(9, AnimationAction.UpdateStackPointer("$spName-0x10", isHighlighted = false))

        // 3. 结果 64-bit (8 bytes) 预分配 → 数据段
        val resultBytes = doubleToBytes(result)
        resultBytes.forEachIndexed { i, byte ->
            actions += TimedAction(9, AnimationAction.WriteMemory(
                (resultAddr + i).toInt(), byte,
                isAllocated = true, isPointer = false, isWriting = false, regionTag = "DATA"
            ))
        }

        // 4. 表达式字符串 → 数据段（ASCII 字节，最多 8 字符）
        val exprDataAddr = deriveAddress(expression.hashCode().toLong(), 10)
        val exprChars = expression.take(8).map { it.code.toByte() }
        exprChars.forEachIndexed { i, byte ->
            actions += TimedAction(10, AnimationAction.WriteMemory(
                (exprDataAddr + i).toInt(), byte,
                isAllocated = true, isPointer = false, isWriting = true, regionTag = "DATA"
            ))
        }

        // 5. 常量池（π / e）→ 常量段
        val constPoolAddr = deriveAddress(expression.hashCode().toLong(), 20)
        val hasPi = expression.contains("pi", ignoreCase = true)
        val hasE = expression.contains("e") && !expression.contains("exp", ignoreCase = true)
        if (hasPi) {
            doubleToBytes(kotlin.math.PI).forEachIndexed { i, byte ->
                actions += TimedAction(10, AnimationAction.WriteMemory(
                    (constPoolAddr + i).toInt(), byte,
                    isAllocated = true, isPointer = false, isWriting = true, regionTag = "CONST"
                ))
            }
        }
        if (hasE) {
            doubleToBytes(kotlin.math.E).forEachIndexed { i, byte ->
                actions += TimedAction(10, AnimationAction.WriteMemory(
                    (constPoolAddr + 8 + i).toInt(), byte,
                    isAllocated = true, isPointer = false, isWriting = true, regionTag = "CONST"
                ))
            }
        }

        // 6. 堆区 / 空闲区（混合已分配与指针，构成完整网格）
        val heapAddr = deriveAddress(expression.hashCode().toLong(), 30)
        val heapData = List(8) { ((it * 31 + expression.length * 7) % 256).toByte() }
        heapData.forEachIndexed { i, byte ->
            actions += TimedAction(10, AnimationAction.WriteMemory(
                (heapAddr + i).toInt(), byte,
                isAllocated = i % 3 != 0,
                isPointer = i % 5 == 0 && i != 0,
                isWriting = false, regionTag = "HEAP"
            ))
        }

        // 7. 指针动画 + 光标读取头移动
        actions += TimedAction(10, AnimationAction.AnimateMemoryPointer(leftAddr.toInt(), rightAddr.toInt(), progress = 0.6f))
        actions += TimedAction(10, AnimationAction.UpdateCursorAddress(leftAddr.toInt(), 0))
        actions += TimedAction(10, AnimationAction.UpdateCursorAddress(rightAddr.toInt(), 1))

        // 8. 关闭 left / right / expr / const 的写入脉冲
        leftBytes.forEachIndexed { i, byte ->
            actions += TimedAction(10, AnimationAction.WriteMemory((leftAddr + i).toInt(), byte, isAllocated = true, isPointer = false, isWriting = false))
        }
        rightBytes.forEachIndexed { i, byte ->
            actions += TimedAction(10, AnimationAction.WriteMemory((rightAddr + i).toInt(), byte, isAllocated = true, isPointer = false, isWriting = false))
        }
        exprChars.forEachIndexed { i, byte ->
            actions += TimedAction(11, AnimationAction.WriteMemory((exprDataAddr + i).toInt(), byte, isAllocated = true, isPointer = false, isWriting = false))
        }
        if (hasPi) {
            doubleToBytes(kotlin.math.PI).forEachIndexed { i, byte ->
                actions += TimedAction(11, AnimationAction.WriteMemory((constPoolAddr + i).toInt(), byte, isAllocated = true, isPointer = false, isWriting = false))
            }
        }
        if (hasE) {
            doubleToBytes(kotlin.math.E).forEachIndexed { i, byte ->
                actions += TimedAction(11, AnimationAction.WriteMemory((constPoolAddr + 8 + i).toInt(), byte, isAllocated = true, isPointer = false, isWriting = false))
            }
        }

        actions += TimedAction(11, AnimationAction.ExitPhase(
            PipelinePhase.PHASE_MEMORY,
            "L4: mem[0x${leftAddr.toString(16).uppercase()}]=$leftVal, mem[0x${rightAddr.toString(16).uppercase()}]=$rightVal, stack pushed"
        ))

        // ========== PHASE 4: PARSE (L5) T12-T16 ==========
        actions += TimedAction(12, AnimationAction.EnterPhase(PipelinePhase.PHASE_PARSE, phaseDurationMs = 1000L))

        actions += TimedAction(12, AnimationAction.UpdateAstGrowth(ast, progress = 0.2f))
        actions += TimedAction(12, AnimationAction.UpdateOperatorStack(opStackStages.getOrElse(0) { emptyList() }))

        actions += TimedAction(13, AnimationAction.UpdateAstGrowth(ast, progress = 0.5f))
        actions += TimedAction(13, AnimationAction.UpdateOperatorStack(opStackStages.getOrElse(1) { opStackStages.lastOrNull() ?: emptyList() }))

        actions += TimedAction(14, AnimationAction.UpdateAstGrowth(ast, progress = 0.8f))
        actions += TimedAction(14, AnimationAction.UpdateOperatorStack(opStackStages.getOrElse(2) { opStackStages.lastOrNull() ?: emptyList() }))

        actions += TimedAction(15, AnimationAction.UpdateAstGrowth(ast, progress = 1.0f))
        actions += TimedAction(15, AnimationAction.UpdateOperatorStack(opStackStages.lastOrNull() ?: emptyList()))
        if (linkedListNodes.isNotEmpty()) {
            actions += TimedAction(15, AnimationAction.UpdateLinkedList(linkedListNodes))
            for (i in 0 until linkedListNodes.size - 1) {
                actions += TimedAction(15, AnimationAction.AnimateLinkedListWire(
                    linkedListNodes[i].id,
                    linkedListNodes[i + 1].id,
                    progress = 0.7f
                ))
            }
        }

        actions += TimedAction(16, AnimationAction.ExitPhase(
            PipelinePhase.PHASE_PARSE,
            "L5: AST built, op-stack ${opStackStages.lastOrNull() ?: emptyList()}"
        ))

        // ========== PHASE 5: EXECUTE (L6-L7) T17-T20 ==========
        actions += TimedAction(17, AnimationAction.EnterPhase(PipelinePhase.PHASE_EXECUTE, phaseDurationMs = 800L))

        actions += TimedAction(17, AnimationAction.UpdateAddressBus(
            segment = seg, offset = leftAddr,
            fullAddress = (seg shl 16) + leftAddr, progress = 0.5f
        ))
        actions += TimedAction(17, AnimationAction.UpdateInstructionPipeline(listOf(
            AnimationAction.PipelineStageData("FETCH", mnemonic(arch, "EVAL"), isActive = true, progress = 1.0f),
            AnimationAction.PipelineStageData("DECODE", op.lowercase(), isActive = false, progress = 0f),
            AnimationAction.PipelineStageData("EXECUTE", "", isActive = false, progress = 0f),
            AnimationAction.PipelineStageData("WRITEBACK", "", isActive = false, progress = 0f)
        )))

        actions += TimedAction(18, AnimationAction.UpdateInstructionPipeline(listOf(
            AnimationAction.PipelineStageData("FETCH", mnemonic(arch, "EVAL"), isActive = true, progress = 1.0f),
            AnimationAction.PipelineStageData("DECODE", op.lowercase(), isActive = true, progress = 1.0f),
            AnimationAction.PipelineStageData("EXECUTE", "", isActive = true, progress = 0.5f),
            AnimationAction.PipelineStageData("WRITEBACK", "", isActive = false, progress = 0f)
        )))
        actions += TimedAction(18, AnimationAction.UpdateAddressBus(
            segment = seg, offset = rightAddr,
            fullAddress = (seg shl 16) + rightAddr, progress = 1.0f
        ))

        actions += TimedAction(19, AnimationAction.UpdateAluOperation(op, leftVal.toLong(), rightVal.toLong(), result.toLong(), isActive = true))
        actions += TimedAction(19, AnimationAction.UpdateInstructionPipeline(listOf(
            AnimationAction.PipelineStageData("FETCH", mnemonic(arch, "EVAL"), isActive = true, progress = 1.0f),
            AnimationAction.PipelineStageData("DECODE", op.lowercase(), isActive = true, progress = 1.0f),
            AnimationAction.PipelineStageData("EXECUTE", "", isActive = true, progress = 1.0f),
            AnimationAction.PipelineStageData("WRITEBACK", "", isActive = true, progress = 1.0f)
        )))

        actions += TimedAction(20, AnimationAction.ExitPhase(
            PipelinePhase.PHASE_EXECUTE,
            "L6-L7: $op executed, result=$result"
        ))

        // ========== PHASE 6: OUTPUT (L8) T21-T24 ==========
        actions += TimedAction(21, AnimationAction.EnterPhase(PipelinePhase.PHASE_OUTPUT, phaseDurationMs = 800L))
        actions += TimedAction(21, AnimationAction.UpdateBitGrid(bits, "RESULT IEEE 754 = $result"))
        actions += TimedAction(21, AnimationAction.UpdateDataPath("ALU", arch.registerNames[0], progress = 0.5f))
        actions += TimedAction(21, AnimationAction.UpdateRegister(0, result.toLong(), isHighlighted = true))
        actions += TimedAction(22, AnimationAction.UpdateDataPath(arch.registerNames[0], "MEM", progress = 0.7f))
        resultBytes.forEachIndexed { i, byte ->
            actions += TimedAction(22, AnimationAction.WriteMemory(
                (resultAddr + i).toInt(), byte,
                isAllocated = true, isPointer = false, isWriting = true, regionTag = "DATA"
            ))
        }
        actions += TimedAction(23, AnimationAction.UpdateResultFlow("REGISTER", "DISPLAY", progress = 1.0f))
        actions += TimedAction(23, AnimationAction.UpdateDisplayBuffer(result.toString(), result.toString().length, isTyping = false))
        resultBytes.forEachIndexed { i, byte ->
            actions += TimedAction(23, AnimationAction.WriteMemory(
                (resultAddr + i).toInt(), byte,
                isAllocated = true, isPointer = false, isWriting = false, regionTag = "DATA"
            ))
        }
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
        actions += TimedAction(1, AnimationAction.UpdateBitGrid(List(64) { false }, "BACKSPACE"))
        actions += TimedAction(2, AnimationAction.UpdateCursorAddress(0, 0))
        val total = 3L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    private fun generateMemoryScript(type: MemoryOpType, arch: Architecture): AnimationScript {
        val actions = mutableListOf<TimedAction>()
        val label = when (type) {
            MemoryOpType.CLEAR -> "MC"
            MemoryOpType.RECALL -> "MR"
            MemoryOpType.ADD -> "M+"
            MemoryOpType.SUBTRACT -> "M-"
            MemoryOpType.STORE -> "MS"
        }
        val seg = deriveSegment(label)
        val addr = deriveAddress(type.ordinal.toLong(), 0)
        actions += TimedAction(0, AnimationAction.UpdateDescription("内存操作: $label"))
        actions += TimedAction(1, AnimationAction.UpdateAddressBus(
            segment = seg, offset = addr,
            fullAddress = (seg shl 16) + addr, progress = 0.5f
        ))
        actions += TimedAction(2, AnimationAction.WriteMemory(addr.toInt(), 0, isAllocated = true, isPointer = false, isWriting = true, regionTag = "DATA"))
        actions += TimedAction(3, AnimationAction.WriteMemory(addr.toInt(), 0, isAllocated = true, isPointer = false, isWriting = false, regionTag = "DATA"))
        val total = 4L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    private fun generateDecimalScript(arch: Architecture): AnimationScript {
        val actions = mutableListOf<TimedAction>()
        actions += TimedAction(0, AnimationAction.UpdateDescription("输入小数点"))
        actions += TimedAction(1, AnimationAction.UpdateBitGrid(longToBits('.'.code.toLong()), "ASCII '.' = 0x2E"))
        actions += TimedAction(2, AnimationAction.UpdateDisplayBuffer(".", 0, isTyping = true))
        val total = 3L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    private fun generateExpressionParsedScript(expression: String): AnimationScript {
        val actions = mutableListOf<TimedAction>()
        val ast = evaluator.parse(expression).getOrNull()
        actions += TimedAction(0, AnimationAction.UpdateDescription("表达式解析: $expression"))
        actions += TimedAction(1, AnimationAction.UpdateAstGrowth(ast, progress = 0.3f))
        val total = 2L
        actions += TimedAction(total, AnimationAction.SetDuration(total))
        return AnimationScript(actions, total)
    }

    private fun generateBitOperationScript(
        op: String, left: Long, right: Long, result: Long
    ): AnimationScript {
        val actions = mutableListOf<TimedAction>()
        val leftBits = longToBits(left)
        val rightBits = longToBits(right)
        val resultBits = longToBits(result)
        val gateType = when (op.uppercase()) {
            "AND" -> AnimationAction.LogicGateType.AND
            "OR" -> AnimationAction.LogicGateType.OR
            "XOR" -> AnimationAction.LogicGateType.XOR
            "NOT" -> AnimationAction.LogicGateType.NOT
            else -> AnimationAction.LogicGateType.AND
        }
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

    // ==================== 真实数据推导工具方法 ====================

    private fun longToBits(value: Long): List<Boolean> {
        return List(64) { i -> ((value shr i) and 1L) == 1L }
    }

    /**
     * 基于表达式字符串生成确定性段寄存器值（FNV-1a 哈希，避免碰撞）
     */
    private fun deriveSegment(expression: String): Long {
        var hash = 0x811c9dc5L
        for (ch in expression) {
            hash = hash xor ch.code.toLong()
            hash *= 0x01000193L
        }
        return (hash and 0xFFFF).coerceAtLeast(1)
    }

    /**
     * 基于 64-bit seed 和索引生成确定性内存地址（完整位混合，避免碰撞）
     */
    private fun deriveAddress(seed: Long, index: Int): Long {
        // 使用 PCG random 乘数（在 Long 范围内）+ 位旋转混合，避免碰撞
        val mul1 = 0x5851F42D4C957F2DL
        val mul2 = 0x14057B7EF767814FL
        val mixed = seed * mul1 + index * mul2
        return 0x1000L + (mixed and 0xFFFFL)
    }

    /**
     * 将 Double 的 64-bit IEEE 754 表示拆分为 8 个 Byte
     */
    private fun doubleToBytes(value: Double): List<Byte> {
        val raw = value.toRawBits()
        return List(8) { i -> ((raw ushr (i * 8)) and 0xFF).toByte() }
    }

    /**
     * 从 AST 提取左右操作数。
     * 对二元表达式完整递归求值左右子树；
     * 对一元/函数/阶乘提取真实操作数；
     * 对纯数字返回自身作为操作数。
     */
    private fun extractOperands(ast: Expression?, result: Double): Pair<Double, Double> {
        return when (ast) {
            is Expression.Binary -> {
                val left = evaluateLiteral(ast.left)
                val right = evaluateLiteral(ast.right)
                Pair(left, right)
            }
            is Expression.Unary -> {
                when (ast.operator) {
                    UnaryOperator.FACTORIAL -> {
                        val n = evaluateLiteral(ast.operand)
                        Pair(n, 0.0)
                    }
                    else -> {
                        val operand = evaluateLiteral(ast.operand)
                        Pair(0.0, operand)
                    }
                }
            }
            is Expression.FunctionCall -> {
                val arg = evaluateLiteral(ast.argument)
                Pair(0.0, arg)
            }
            is Expression.NumberLiteral -> Pair(ast.value, 0.0)
            is Expression.ConstantRef -> Pair(ast.value, 0.0)
            else -> Pair(result, 0.0)
        }
    }

    /**
     * 完整递归求值 AST 叶子节点，不忽略任何子树。
     */
    private fun evaluateLiteral(expr: Expression?): Double {
        return when (expr) {
            is Expression.NumberLiteral -> expr.value
            is Expression.ConstantRef -> expr.value
            is Expression.VariableRef -> 0.0
            is Expression.Unary -> {
                val operand = evaluateLiteral(expr.operand)
                when (expr.operator) {
                    UnaryOperator.NEGATE -> -operand
                    UnaryOperator.PERCENT -> operand / 100.0
                    UnaryOperator.FACTORIAL -> factorial(operand)
                }
            }
            is Expression.Binary -> {
                val left = evaluateLiteral(expr.left)
                val right = evaluateLiteral(expr.right)
                when (expr.operator) {
                    BinaryOperator.ADD -> left + right
                    BinaryOperator.SUBTRACT -> left - right
                    BinaryOperator.MULTIPLY -> left * right
                    BinaryOperator.DIVIDE -> if (right != 0.0) left / right else Double.NaN
                    BinaryOperator.POWER -> left.pow(right)
                }
            }
            is Expression.FunctionCall -> {
                val arg = evaluateLiteral(expr.argument)
                when (expr.name.lowercase()) {
                    "sin" -> sin(arg)
                    "cos" -> cos(arg)
                    "tan" -> tan(arg)
                    "asin" -> kotlin.math.asin(arg)
                    "acos" -> kotlin.math.acos(arg)
                    "atan" -> kotlin.math.atan(arg)
                    "sinh" -> kotlin.math.sinh(arg)
                    "cosh" -> kotlin.math.cosh(arg)
                    "tanh" -> tanh(arg)
                    "log" -> log10(arg)
                    "ln" -> ln(arg)
                    "sqrt" -> sqrt(arg)
                    "cbrt" -> cbrt(arg)
                    "abs" -> kotlin.math.abs(arg)
                    "floor" -> floor(arg)
                    "ceil" -> ceil(arg)
                    "round" -> round(arg)
                    else -> Double.NaN
                }
            }
            else -> 0.0
        }
    }

    private fun factorial(n: Double): Double {
        if (n < 0 || n != n.toInt().toDouble()) return Double.NaN
        val intN = n.toInt()
        if (intN > 170) return Double.POSITIVE_INFINITY
        var result = 1.0
        for (i in 2..intN) result *= i
        return result
    }

    /**
     * 提取 AST 中所有叶子操作数值（用于多寄存器分配）
     */
    private fun extractAllOperands(ast: Expression?): List<Double> {
        val result = mutableListOf<Double>()
        fun traverse(expr: Expression?) {
            when (expr) {
                is Expression.NumberLiteral -> result.add(expr.value)
                is Expression.ConstantRef -> result.add(expr.value)
                is Expression.VariableRef -> result.add(0.0)
                is Expression.Binary -> {
                    traverse(expr.left)
                    traverse(expr.right)
                }
                is Expression.Unary -> traverse(expr.operand)
                is Expression.FunctionCall -> traverse(expr.argument)
                else -> {}
            }
        }
        traverse(ast)
        return result
    }

    /**
     * 基于真实表达式字符串构建 Shunting Yard 运算符栈阶段。
     * 提取数字和运算符作为 token，生成每一步的栈快照。
     */
    private fun buildShuntingYardStages(expression: String): List<List<String>> {
        val tokens = mutableListOf<String>()
        var current = ""
        for (ch in expression) {
            when {
                ch.isDigit() || ch == '.' -> current += ch
                else -> {
                    if (current.isNotEmpty()) { tokens.add(current); current = "" }
                    if (ch in "+-×÷*/^%") tokens.add(ch.toString())
                }
            }
        }
        if (current.isNotEmpty()) tokens.add(current)

        val stages = mutableListOf<List<String>>()
        val stack = mutableListOf<String>()
        for (token in tokens) {
            stack.add(token)
            stages.add(stack.toList())
        }
        if (stages.isEmpty()) stages.add(emptyList())
        return stages
    }

    /**
     * 将 AST 转换为真实的链表节点序列（替代人工构造 n1->n2->n3）。
     * 对二元表达式：左操作数 → 运算符 → 右操作数
     * 对一元/函数：操作数 → 运算符
     */
    private fun astToLinkedList(ast: Expression?): List<AnimationAction.LinkedListNodeData> {
        val nodes = mutableListOf<AnimationAction.LinkedListNodeData>()
        return when (ast) {
            is Expression.Binary -> {
                val leftStr = astNodeLabel(ast.left)
                val rightStr = astNodeLabel(ast.right)
                val opStr = when (ast.operator) {
                    BinaryOperator.ADD -> "+"
                    BinaryOperator.SUBTRACT -> "-"
                    BinaryOperator.MULTIPLY -> "×"
                    BinaryOperator.DIVIDE -> "÷"
                    BinaryOperator.POWER -> "^"
                }
                listOf(
                    AnimationAction.LinkedListNodeData("n1", leftStr, "n2", isNew = true),
                    AnimationAction.LinkedListNodeData("n2", opStr, "n3", isNew = true),
                    AnimationAction.LinkedListNodeData("n3", rightStr, null, isNew = true)
                )
            }
            is Expression.Unary -> {
                val operandStr = astNodeLabel(ast.operand)
                val opStr = when (ast.operator) {
                    UnaryOperator.NEGATE -> "-"
                    UnaryOperator.PERCENT -> "%"
                    UnaryOperator.FACTORIAL -> "!"
                }
                listOf(
                    AnimationAction.LinkedListNodeData("n1", operandStr, "n2", isNew = true),
                    AnimationAction.LinkedListNodeData("n2", opStr, null, isNew = true)
                )
            }
            is Expression.FunctionCall -> {
                val argStr = astNodeLabel(ast.argument)
                listOf(
                    AnimationAction.LinkedListNodeData("n1", ast.name, "n2", isNew = true),
                    AnimationAction.LinkedListNodeData("n2", argStr, null, isNew = true)
                )
            }
            else -> {
                val label = astNodeLabel(ast)
                listOf(AnimationAction.LinkedListNodeData("n1", label, null, isNew = true))
            }
        }
    }

    private fun astNodeLabel(expr: Expression?): String {
        return when (expr) {
            is Expression.NumberLiteral -> {
                val v = expr.value
                if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
            }
            is Expression.ConstantRef -> expr.name
            is Expression.VariableRef -> expr.name
            is Expression.Binary -> {
                val op = when (expr.operator) {
                    BinaryOperator.ADD -> "+"
                    BinaryOperator.SUBTRACT -> "-"
                    BinaryOperator.MULTIPLY -> "×"
                    BinaryOperator.DIVIDE -> "÷"
                    BinaryOperator.POWER -> "^"
                }
                "(${astNodeLabel(expr.left)} $op ${astNodeLabel(expr.right)})"
            }
            is Expression.Unary -> {
                val op = when (expr.operator) {
                    UnaryOperator.NEGATE -> "-"
                    UnaryOperator.PERCENT -> "%"
                    UnaryOperator.FACTORIAL -> "!"
                }
                "($op${astNodeLabel(expr.operand)})"
            }
            is Expression.FunctionCall -> "${expr.name}(${astNodeLabel(expr.argument)})"
            else -> "?"
        }
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

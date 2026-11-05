package com.kletaq.app.features.lab

import kotlin.random.Random

object LabKernelExtractor {

    data class ExtractedFunction(
        val name: String,
        val headerLine: String,
        val fullFunctionCode: String,
        val executableLines: List<String>
    )

    /**
     * Dynamically slices any C/Java program from genuine syllabus code or an uploaded PDF into real code Kernels.
     * Zero hardcoded "// Global Definitions" or empty boxes.
     */
    fun extractKernels(programId: String, fullCode: String): List<LabKernel> {
        val normalized = fullCode
            .replace("\r\n", "\n")
            .replace("$", "") // Cleans OCR artifacts like $i=0$ from PDF text
            .trim()

        if (normalized.isBlank()) return emptyList()

        val (globalsCode, functions) = sliceGlobalsAndFunctions(normalized)
        val kernels = mutableListOf<LabKernel>()

        // Kernel 1: Exact Global, Header & Struct lines from the program
        if (globalsCode.isNotBlank()) {
            kernels.add(
                LabKernel(
                    id = "${programId}_k1",
                    name = "1. Headers, Struct & Global State",
                    codeSnippet = globalsCode,
                    examTrick = buildDynamicTip("Global Setup", globalsCode)
                )
            )
        }

        val mainFn = functions.find { it.name == "main" }
        val workerFns = functions.filter { it.name != "main" }

        // Group worker functions into logical slots so the Blueprint is always populated with real code
        val groupedWorkers: List<List<ExtractedFunction>> = when {
            workerFns.isEmpty() -> emptyList()
            workerFns.size <= 3 -> workerFns.map { listOf(it) }
            else -> {
                // Keep first function separate, group middle helpers, keep last core function
                val first = listOf(workerFns[0])
                val middle = workerFns.subList(1, workerFns.size - 1)
                val last = listOf(workerFns.last())
                listOf(first, middle, last)
            }
        }

        groupedWorkers.forEach { group ->
            val combinedNames = group.joinToString(" & ") { "${it.name}()" }
            val combinedCode = group.joinToString("\n\n") { it.fullFunctionCode }
            val index = kernels.size + 1
            kernels.add(
                LabKernel(
                    id = "${programId}_k$index",
                    name = "$index. $combinedNames",
                    codeSnippet = combinedCode,
                    examTrick = buildDynamicTip(combinedNames, combinedCode)
                )
            )
        }

        // Final Kernel: Exact main() driver from the code
        if (mainFn != null) {
            val index = kernels.size + 1
            kernels.add(
                LabKernel(
                    id = "${programId}_k$index",
                    name = "$index. main() Menu & Driver",
                    codeSnippet = mainFn.fullFunctionCode,
                    examTrick = buildDynamicTip("main()", mainFn.fullFunctionCode)
                )
            )
        }

        return kernels
    }

    /**
     * Generates all 4 drills strictly from genuine program code and Sample Output.
     */
    fun generateDrills(
        title: String,
        fullCode: String,
        inputOutputSample: String = ""
    ): LabProgramDrills {
        val cleanCode = fullCode.replace("\r\n", "\n").replace("$", "").trim()
        if (cleanCode.isBlank()) return LabProgramDrills()

        val (_, functions) = sliceGlobalsAndFunctions(cleanCode)
        if (functions.isEmpty()) return LabProgramDrills()

        val coreFunctions = functions.filter { it.name != "main" && it.name != "display" }
            .ifEmpty { functions }

        val parsonsTarget = coreFunctions.firstOrNull { it.executableLines.size >= 4 } ?: coreFunctions.first()
        val clozeTarget = coreFunctions.maxByOrNull { it.fullFunctionCode.length } ?: coreFunctions.first()
        val bugTarget = coreFunctions.lastOrNull() ?: coreFunctions.first()

        return LabProgramDrills(
            dryRun = generateDynamicDryRun(title, functions, inputOutputSample),
            parsonsPuzzle = generateDynamicParsons(parsonsTarget),
            clozeDrill = generateDynamicCloze(clozeTarget),
            bugHunt = generateDynamicBugHunt(bugTarget)
        )
    }

    /**
     * Unified extractor for both Kernels and Drills.
     */
    fun extractKernelsAndDrills(
        fullCode: String,
        title: String,
        language: String = "C",
        inputOutputSample: String = ""
    ): Pair<List<LabKernel>, LabProgramDrills> {
        val kernels = extractKernels(title.take(16).trim(), fullCode)
        val drills = generateDrills(title, fullCode, inputOutputSample)
        return kernels to drills
    }

    fun sliceGlobalsAndFunctions(code: String): Pair<String, List<ExtractedFunction>> {
        val lines = code.lines()
        val globalLines = mutableListOf<String>()
        val functions = mutableListOf<ExtractedFunction>()

        val fnSignatureRegex = Regex(
            """^\s*(?:void|int|float|double|char|long|struct\s+\w+|\w+\s*\*?)\s+([a-zA-Z_]\w*)\s*\([^;]*\)\s*\{?\s*$"""
        )
        val controlKeywords = setOf("if", "for", "while", "switch", "else", "return", "sizeof")

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val match = fnSignatureRegex.find(line)
            val fnName = match?.groupValues?.getOrNull(1)

            if (fnName != null && fnName !in controlKeywords) {
                val fnBlock = mutableListOf<String>()
                var braceBalance = 0
                var opened = false

                while (i < lines.size) {
                    val cur = lines[i]
                    fnBlock.add(cur)
                    for (ch in cur) {
                        if (ch == '{') {
                            braceBalance++
                            opened = true
                        } else if (ch == '}') {
                            braceBalance--
                        }
                    }
                    i++
                    if (opened && braceBalance <= 0) break
                }

                val execLines = fnBlock
                    .drop(1)
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it != "{" && it != "}" }

                functions.add(
                    ExtractedFunction(
                        name = fnName,
                        headerLine = fnBlock.first().trim(),
                        fullFunctionCode = fnBlock.joinToString("\n"),
                        executableLines = execLines
                    )
                )
            } else {
                globalLines.add(line)
                i++
            }
        }

        return globalLines.joinToString("\n").trim() to functions
    }

    /**
     * Inspects actual C/Java syntax tokens inside the snippet to generate an authentic exam tip.
     */
    private fun buildDynamicTip(fnName: String, snippet: String): String {
        val notes = mutableListOf<String>()

        Regex("""typedef\s+struct\s*\{([^}]+)\}\s*(\w+)""").find(snippet)?.let { m ->
            val structName = m.groupValues[2]
            notes.add("Defines struct '$structName' so all functions share a single record type.")
        }
        Regex("""(\w+)\s*\*\s*(\w+)\s*=\s*NULL""").find(snippet)?.let { m ->
            notes.add("Global pointer '${m.groupValues[2]}' is initialized to NULL for dynamic allocation across functions.")
        }
        Regex("""(\w+)\s*=\s*realloc\(([^)]+)\)""").find(snippet)?.let { m ->
            notes.add("Resizes '${m.groupValues[1]}' dynamically via realloc(${m.groupValues[2]}) and checks if (!${m.groupValues[1]}) exit(1).")
        }
        if ("%[^\\n]" in snippet) {
            notes.add("Uses \"%[^\\n]\" in scanf to capture multi-word strings with spaces until a newline.")
        }
        Regex("""strcmp\(([^,]+),\s*("[^"]+")\)""").find(snippet)?.let { m ->
            notes.add("Checks if ${m.groupValues[1]} matches ${m.groupValues[2]} using strcmp(...) == 0.")
        }
        Regex("""strcpy\(([^,]+),\s*("[^"]+")\)""").find(snippet)?.let { m ->
            notes.add("Updates ${m.groupValues[1]} to ${m.groupValues[2]} using strcpy().")
        }
        Regex("""\((\w+)\)\s*\{([^}]+)\}""").find(snippet)?.let { m ->
            notes.add("Uses compound literal (${m.groupValues[1]}){${m.groupValues[2]}} to assign struct fields inline.")
        }
        if ("return;" in snippet && "for" in snippet) {
            notes.add("Executes 'return;' immediately inside the match block so the fallback message at the end of $fnName only prints when no match is found.")
        }
        if ("switch" in snippet && "do" in snippet) {
            notes.add("Menu loop runs until the exit choice is entered; verify each case calls the right function and breaks.")
        }

        return if (notes.isNotEmpty()) {
            notes.joinToString(" ")
        } else {
            val firstCall = snippet.lines().firstOrNull { it.contains("(") && !it.startsWith("void") && !it.startsWith("int") }?.trim() ?: fnName
            "Focus on the execution order around `$firstCall` in $fnName."
        }
    }

    private fun generateDynamicDryRun(
        title: String,
        functions: List<ExtractedFunction>,
        sampleIo: String
    ): DryRunQuestion {
        // If sample output is provided, build dry-run directly from actual I/O trace
        val cleanIoLines = sampleIo.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (cleanIoLines.size >= 4) {
            val contextLines = cleanIoLines.take(cleanIoLines.size - 1).takeLast(6).joinToString("\n")
            val finalOutputLine = cleanIoLines.last()

            // Pull other output strings from printf statements as realistic distractors
            val allPrintfs = functions.flatMap { fn ->
                Regex("""printf\("([^"\\]+)(?:\\n)?"\)""").findAll(fn.fullFunctionCode)
                    .map { it.groupValues[1].trim() }
                    .filter { it.isNotBlank() && it != finalOutputLine }
                    .toList()
            }.distinct()

            val distractor1 = allPrintfs.getOrNull(0) ?: "Memory allocation failed!"
            val distractor2 = allPrintfs.getOrNull(1) ?: "Invalid choice!"
            val distractor3 = allPrintfs.getOrNull(2) ?: "No records found."

            val options = listOf(finalOutputLine, distractor1, distractor2, distractor3).distinct()
            val shuffled = options.shuffled(Random(title.hashCode()))
            val correctIdx = shuffled.indexOf(finalOutputLine).coerceAtLeast(0)

            return DryRunQuestion(
                scenarioText = "Trace from sample output for $title:\n\n$contextLines",
                question = "Based on the program's execution flow, what is the exact next output printed?",
                options = shuffled,
                correctOptionIndex = correctIdx,
                explanation = "Following the sample trace, the program prints: \"$finalOutputLine\"."
            )
        }

        // Fallback: build directly from the first conditional function in genuine code
        val targetFn = functions.firstOrNull { "if" in it.fullFunctionCode && it.name != "main" } ?: functions.first()
        val snippetPreview = targetFn.executableLines.take(5).joinToString("\n")
        val correctLine = targetFn.executableLines.firstOrNull { it.startsWith("if") || it.startsWith("for") || it.startsWith("while") } ?: targetFn.headerLine
        return DryRunQuestion(
            scenarioText = "Inside ${targetFn.name}():\n$snippetPreview",
            question = "Which statement governs the primary branch in ${targetFn.name}()?",
            options = listOf(
                correctLine,
                "while (1) infinite loop without exit condition",
                "Recursive self-call to ${targetFn.name}()"
            ).shuffled(Random(targetFn.name.hashCode())),
            correctOptionIndex = 0,
            explanation = buildDynamicTip(targetFn.name, targetFn.fullFunctionCode)
        )
    }

    private fun generateDynamicParsons(fn: ExtractedFunction): ParsonsPuzzle {
        val targetLines = fn.executableLines
            .filter { !it.startsWith("printf(\"\\n") }
            .take(6)
            .ifEmpty { fn.fullFunctionCode.lines().map { it.trim() }.filter { it.isNotBlank() }.take(5) }

        var scrambled = targetLines.shuffled(Random(fn.fullFunctionCode.hashCode()))
        if (scrambled == targetLines && scrambled.size > 1) {
            scrambled = scrambled.reversed()
        }

        return ParsonsPuzzle(
            prompt = "Arrange the exact lines of `${fn.name}()` in the right execution order:",
            scrambledLines = scrambled,
            correctLines = targetLines,
            explanation = buildDynamicTip(fn.name, fn.fullFunctionCode)
        )
    }

    private fun generateDynamicCloze(fn: ExtractedFunction): ClozeDrill {
        val tokenRegex = Regex(
            """(realloc|malloc|free|strcpy|strcmp|sizeof\(\w+\)|"\s*%\[\^\\n\]"|\(\w+\)\s*\{[^}]+\}|\w+\[i\]\.\w+|\w+\[0\]\.\w+|k\s*-\s*1)"""
        )
        val matches = tokenRegex.findAll(fn.fullFunctionCode).toList().take(4)

        if (matches.isEmpty()) {
            return ClozeDrill(
                prompt = "Fill in the function identifier for `${fn.name}()`:",
                codeTemplate = fn.fullFunctionCode.replaceFirst(fn.name, "[ ___1___ ]"),
                blanks = listOf(fn.name),
                chipOptions = listOf(fn.name, "main", "display", "init").shuffled()
            )
        }

        val sb = StringBuilder()
        var cursor = 0
        val blanks = mutableListOf<String>()

        matches.forEachIndexed { index, m ->
            sb.append(fn.fullFunctionCode.substring(cursor, m.range.first))
            sb.append("[ ___${index + 1}___ ]")
            blanks.add(m.value)
            cursor = m.range.last + 1
        }
        sb.append(fn.fullFunctionCode.substring(cursor))

        // Build contextual distractors by mutating actual tokens found in the code
        val distractors = blanks.mapNotNull { token ->
            when {
                token == "realloc" -> "malloc"
                token == "strcmp" -> "strcpy"
                token == "strcpy" -> "strcmp"
                "k - 1" in token -> "k + 1"
                "sizeof" in token -> "sizeof(int)"
                "%[^\\n]" in token -> "\"%d\""
                else -> null
            }
        }.distinct()

        val allChips = (blanks + distractors).distinct().shuffled(Random(fn.name.hashCode()))

        return ClozeDrill(
            prompt = "Tap the chips to complete `${fn.name}()`:",
            codeTemplate = sb.toString(),
            blanks = blanks,
            chipOptions = allChips
        )
    }

    private fun generateDynamicBugHunt(fn: ExtractedFunction): BugHuntDrill {
        val lines = fn.fullFunctionCode.lines().filter { it.isNotBlank() }.toMutableList()
        var buggyLineIdx = -1
        var reason = ""

        for (i in lines.indices) {
            val l = lines[i]
            when {
                "strcmp(" in l && "== 0" in l -> {
                    lines[i] = l.replace(Regex("""strcmp\(([^,]+),\s*([^)]+)\)\s*==\s*0"""), "$1 == $2")
                    buggyLineIdx = i
                    reason = "In C, comparing strings with `==` compares memory addresses instead of character values. The program uses `strcmp(...) == 0` on this line."
                    break
                }
                "==" in l && "if" in l -> {
                    lines[i] = l.replaceFirst("==", "=")
                    buggyLineIdx = i
                    reason = "Single `=` assigns instead of checking equality (`==`) inside the `if` condition."
                    break
                }
                "k - 1" in l || "k-1" in l -> {
                    lines[i] = l.replace("k - 1", "k").replace("k-1", "k")
                    buggyLineIdx = i
                    reason = "Because `k` starts at 1 and increments after each element (`k++`), the total count stored in header index 0 must be `k - 1`."
                    break
                }
                "realloc(" in l -> {
                    lines[i] = l.replace(Regex("""\*\s*sizeof\(\w+\)"""), "")
                    buggyLineIdx = i
                    reason = "Missing `* sizeof(...)` in `realloc` allocates only `n` bytes instead of `n` full structs!"
                    break
                }
            }
        }

        if (buggyLineIdx == -1) {
            buggyLineIdx = (lines.size / 2).coerceAtLeast(0)
            reason = "Verify the condition and state mutation on line ${buggyLineIdx + 1}."
        }

        return BugHuntDrill(
            prompt = "Spot the line in `${fn.name}()` that contains an exam bug:",
            codeLinesWithNumbers = lines.mapIndexed { idx, s -> (idx + 1) to s },
            buggyLineNumber = buggyLineIdx + 1,
            explanation = reason
        )
    }
}

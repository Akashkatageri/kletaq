package com.kletaq.app.features.lab

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.kletaq.app.features.journey.components.SubjectJourney
import com.kletaq.app.features.journey.components.SubjectType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

object LabDefenseRepository {

    private const val PREFS_NAME = "kletaq_lab_defense_prefs"
    private const val KEY_IS_LAB_PREFIX = "is_lab_"
    private const val KEY_LAB_DAY_PREFIX = "lab_day_"
    private const val KEY_MASTERY_PREFIX = "mastery_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Determines whether a subject is a Lab / Practical subject.
     * Evaluates explicit user overrides first, then auto-detection heuristics.
     */
    fun isSubjectLab(context: Context, subjectId: String, subjectName: String): Boolean {
        val prefs = getPrefs(context)
        val cleanId = subjectId.trim()
        val overrideKey = KEY_IS_LAB_PREFIX + cleanId
        if (prefs.contains(overrideKey)) {
            return prefs.getBoolean(overrideKey, false)
        }

        // Auto-detection heuristics
        val combined = "$subjectId $subjectName".lowercase()
        return combined.contains("lab") ||
               combined.contains("laboratory") ||
               combined.contains("practical") ||
               cleanId.matches(Regex(".*\\d+.*l.*", RegexOption.IGNORE_CASE))
    }

    /**
     * Sets or overrides whether a subject is treated as a Lab subject.
     */
    fun setSubjectLab(context: Context, subjectId: String, isLab: Boolean) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_IS_LAB_PREFIX + subjectId.trim(), isLab).apply()
    }

    /**
     * Retrieves the scheduled day of the week for this lab (e.g., Calendar.THURSDAY).
     * Defaults to null (unset).
     */
    fun getLabDay(context: Context, subjectId: String): Int? {
        val prefs = getPrefs(context)
        val day = prefs.getInt(KEY_LAB_DAY_PREFIX + subjectId.trim(), -1)
        return if (day in Calendar.SUNDAY..Calendar.SATURDAY) day else null
    }

    /**
     * Sets the scheduled day of the week for this lab.
     */
    fun setLabDay(context: Context, subjectId: String, dayOfWeek: Int) {
        val prefs = getPrefs(context)
        prefs.edit().putInt(KEY_LAB_DAY_PREFIX + subjectId.trim(), dayOfWeek).apply()
    }

    /**
     * Retrieves the mastery status of a specific lab program.
     */
    fun getProgramMastery(context: Context, subjectId: String, programNumber: Int): ProgramMasteryStatus {
        val prefs = getPrefs(context)
        val key = "${KEY_MASTERY_PREFIX}${subjectId.trim()}_$programNumber"
        val name = prefs.getString(key, null) ?: return ProgramMasteryStatus.UNTOUCHED
        return try {
            ProgramMasteryStatus.valueOf(name)
        } catch (_: Exception) {
            ProgramMasteryStatus.UNTOUCHED
        }
    }

    /**
     * Updates the mastery status of a specific lab program.
     */
    fun setProgramMastery(context: Context, subjectId: String, programNumber: Int, status: ProgramMasteryStatus) {
        val prefs = getPrefs(context)
        val key = "${KEY_MASTERY_PREFIX}${subjectId.trim()}_$programNumber"
        prefs.edit().putString(key, status.name).apply()
    }

    /**
     * Calculates the Lab External Pass Probability:
     * Readiness = (Verified Programs / Total Programs) ^ 2
     */
    fun calculateReadiness(programs: List<LabProgramItem>): Pair<Float, Float> {
        if (programs.isEmpty()) return Pair(0f, 0f)
        val verifiedCount = programs.count { it.masteryStatus == ProgramMasteryStatus.BLIND_VERIFIED || it.masteryStatus == ProgramMasteryStatus.COLLEGE_EXECUTED }
        val completionRatio = verifiedCount.toFloat() / programs.size.toFloat()
        // Squaring reflects the probability of drawing 2 programs and knowing BOTH
        val passProbability = completionRatio * completionRatio
        return Pair(completionRatio, passProbability)
    }

    private fun getCustomProgramsFile(context: Context, subjectId: String): File {
        val clean = subjectId.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        val dir = File(context.filesDir, "custom_lab_programs")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$clean.json")
    }

    fun hasCustomLabPrograms(context: Context, subjectId: String): Boolean {
        val file = getCustomProgramsFile(context, subjectId)
        return file.exists() && file.length() > 0L
    }

    fun saveCustomLabPrograms(context: Context, subjectId: String, programs: List<LabProgramItem>) {
        try {
            val file = getCustomProgramsFile(context, subjectId)
            val root = JSONObject()
            val arr = JSONArray()
            for (p in programs) {
                val obj = JSONObject()
                obj.put("programNumber", p.programNumber)
                obj.put("title", p.title)
                obj.put("shortTitle", p.shortTitle)
                obj.put("language", p.language)
                obj.put("fullCode", p.fullCode)
                obj.put("blueprintBlock", p.blueprintBlock)
                obj.put("coreFunctionBlock", p.coreFunctionBlock)
                obj.put("guardBlock", p.guardBlock)
                obj.put("driverBlock", p.driverBlock)

                val stepsArr = JSONArray()
                p.algorithmSteps.forEach { stepsArr.put(it) }
                obj.put("algorithmSteps", stepsArr)

                val vivaArr = JSONArray()
                p.vivaQuestions.forEach { (q, a) ->
                    val vObj = JSONObject()
                    vObj.put("question", q)
                    vObj.put("answer", a)
                    vivaArr.put(vObj)
                }
                obj.put("vivaQuestions", vivaArr)

                val trapsArr = JSONArray()
                p.crashTraps.forEach { trapsArr.put(it) }
                obj.put("crashTraps", trapsArr)

                arr.put(obj)
            }
            root.put("programs", arr)
            file.writeText(root.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("LabDefenseRepository", "Error saving custom lab programs: ${e.message}")
        }
    }

    fun getCustomLabPrograms(context: Context, subjectId: String): List<LabProgramItem>? {
        val file = getCustomProgramsFile(context, subjectId)
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val json = file.readText(Charsets.UTF_8)
            val list = mutableListOf<LabProgramItem>()
            val root = JSONObject(json)
            val arr = root.optJSONArray("programs") ?: return null
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pNum = obj.optInt("programNumber", i + 1)
                val title = obj.optString("title", "Program $pNum")
                val shortTitle = obj.optString("shortTitle", title.take(24))
                val language = obj.optString("language", "C")
                val fullCode = obj.optString("fullCode", "")
                val blueprint = obj.optString("blueprintBlock", "")
                val coreFunc = obj.optString("coreFunctionBlock", "")
                val guard = obj.optString("guardBlock", "")
                val driver = obj.optString("driverBlock", "")

                val stepsArr = obj.optJSONArray("algorithmSteps")
                val steps = mutableListOf<String>()
                if (stepsArr != null) {
                    for (s in 0 until stepsArr.length()) steps.add(stepsArr.getString(s))
                }

                val vivaArr = obj.optJSONArray("vivaQuestions")
                val viva = mutableListOf<Pair<String, String>>()
                if (vivaArr != null) {
                    for (v in 0 until vivaArr.length()) {
                        val vObj = vivaArr.getJSONObject(v)
                        viva.add(vObj.optString("question") to vObj.optString("answer"))
                    }
                }

                val trapsArr = obj.optJSONArray("crashTraps")
                val traps = mutableListOf<String>()
                if (trapsArr != null) {
                    for (t in 0 until trapsArr.length()) traps.add(trapsArr.getString(t))
                }

                list.add(
                    LabProgramItem(
                        programNumber = pNum,
                        title = title,
                        shortTitle = shortTitle,
                        language = language,
                        fullCode = fullCode,
                        blueprintBlock = blueprint,
                        coreFunctionBlock = coreFunc,
                        guardBlock = guard,
                        driverBlock = driver,
                        algorithmSteps = steps,
                        vivaQuestions = viva,
                        crashTraps = traps,
                        masteryStatus = ProgramMasteryStatus.UNTOUCHED
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (_: Exception) {
            null
        }
    }

    fun getCustomLabManualPdfFile(context: Context, subjectId: String): File? {
        val dir = File(context.filesDir, "lab_manual_pdfs")
        val clean = subjectId.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        val file = File(dir, "$clean.pdf")
        return if (file.exists() && file.length() > 0L) file else null
    }

    fun saveLabManualPdf(context: Context, subjectId: String, pdfBytes: ByteArray) {
        try {
            val dir = File(context.filesDir, "lab_manual_pdfs")
            if (!dir.exists()) dir.mkdirs()
            val clean = subjectId.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
            File(dir, "$clean.pdf").writeBytes(pdfBytes)
        } catch (e: Exception) {
            Log.e("LabDefenseRepository", "Failed to save lab manual PDF: ${e.message}")
        }
    }

    fun openLabManualPdf(context: Context, subjectId: String) {
        val file = getCustomLabManualPdfFile(context, subjectId)
        if (file == null) {
            Toast.makeText(context, "No saved PDF manual found.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resInfoList = context.packageManager.queryIntentActivities(viewIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                context.grantUriPermission(resolveInfo.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearCustomLabPrograms(context: Context, subjectId: String) {
        val file = getCustomProgramsFile(context, subjectId)
        if (file.exists()) file.delete()
        val pdfFile = getCustomLabManualPdfFile(context, subjectId)
        if (pdfFile != null && pdfFile.exists()) pdfFile.delete()
    }

    /**
     * Loads the lab programs for a subject. If the user has attached/parsed a custom lab manual,
     * returns those custom programs. Otherwise, falls back to standard built-in or synthesized templates.
     */
    fun getLabProgramsForSubject(context: Context, subject: SubjectJourney): List<LabProgramItem> {
        val customList = getCustomLabPrograms(context, subject.id)
        val baseList = if (customList != null && customList.isNotEmpty()) {
            customList
        } else {
            val combined = "${subject.id} ${subject.name}".lowercase()
            if (combined.contains("data structure") || combined.contains("bcsl305") || combined.contains("21csl32")) {
                createDefaultDataStructurePrograms()
            } else {
                synthesizeProgramsFromSubject(subject)
            }
        }

        // Apply saved mastery statuses
        return baseList.map { program ->
            val savedMastery = getProgramMastery(context, subject.id, program.programNumber)
            program.copy(masteryStatus = savedMastery)
        }
    }

    private fun synthesizeProgramsFromSubject(subject: SubjectJourney): List<LabProgramItem> {
        val allLessons = subject.units.flatMap { it.lessons }
        if (allLessons.isEmpty()) {
            return (1..8).map { i ->
                createTemplateProgram(i, "Practical Program $i: Algorithm & Implementation")
            }
        }

        return allLessons.mapIndexed { idx, lesson ->
            val pNum = idx + 1
            createTemplateProgram(pNum, lesson.title)
        }
    }

    private fun createTemplateProgram(pNum: Int, title: String): LabProgramItem {
        return LabProgramItem(
            programNumber = pNum,
            title = title,
            shortTitle = title.take(28),
            language = "C",
            fullCode = """
                #include <stdio.h>
                #include <stdlib.h>

                // Practical Program $pNum: $title
                void executeCoreLogic() {
                    // Algorithmic processing
                    printf("Executing $title...\n");
                }

                int main() {
                    printf("Running Program $pNum\n");
                    executeCoreLogic();
                    return 0;
                }
            """.trimIndent(),
            blueprintBlock = "#include <stdio.h>\n#include <stdlib.h>\n#define MAX 100",
            coreFunctionBlock = "void executeCoreLogic() {\n    // Core algorithmic steps\n}",
            guardBlock = "if (input == NULL) {\n    printf(\"Error: Invalid input\");\n    return;\n}",
            driverBlock = "int main() {\n    executeCoreLogic();\n    return 0;\n}",
            algorithmSteps = listOf(
                "Initialize environment and variables",
                "Read input parameters from user",
                "Execute the primary logic computation",
                "Verify boundary conditions and edge states",
                "Print output results and exit cleanly"
            ),
            vivaQuestions = listOf(
                "What is the time complexity of this algorithm?" to "O(n) for linear operations, O(n log n) if sorting is utilized.",
                "Why is this data structure chosen?" to "Provides optimal cache locality and predictable memory boundaries.",
                "How are edge cases handled?" to "Input validation guards catch empty, negative, or overflow values."
            ),
            crashTraps = listOf(
                "Buffer overflow on exceeding MAX bound",
                "Uninitialized pointers or variables",
                "Missing termination condition in loop",
                "Memory leak from unreleased allocations"
            )
        )
    }

    private fun createDefaultDataStructurePrograms(): List<LabProgramItem> {
        return listOf(
            LabProgramItem(
                programNumber = 1,
                title = "Program 1: Array Stack Operations (Push, Pop, Display, Overflow/Underflow)",
                shortTitle = "Prog 1: Array Stack",
                language = "C",
                fullCode = """
                    #include <stdio.h>
                    #include <stdlib.h>
                    #define MAX 5

                    int stack[MAX];
                    int top = -1;

                    void push(int item) {
                        if (top == MAX - 1) {
                            printf("Stack Overflow! Cannot push %d\n", item);
                            return;
                        }
                        stack[++top] = item;
                        printf("Pushed %d onto stack\n", item);
                    }

                    void pop() {
                        if (top == -1) {
                            printf("Stack Underflow! Stack is empty\n");
                            return;
                        }
                        printf("Popped %d from stack\n", stack[top--]);
                    }

                    void display() {
                        if (top == -1) {
                            printf("Stack is empty\n");
                            return;
                        }
                        printf("Stack elements (top to bottom):\n");
                        for (int i = top; i >= 0; i--) {
                            printf("| %d |\n", stack[i]);
                        }
                    }

                    int main() {
                        int choice, item;
                        while (1) {
                            printf("\n--- STACK MENU ---\n1. Push\n2. Pop\n3. Display\n4. Exit\nEnter choice: ");
                            if (scanf("%d", &choice) != 1) break;
                            switch (choice) {
                                case 1:
                                    printf("Enter integer to push: ");
                                    scanf("%d", &item);
                                    push(item);
                                    break;
                                case 2:
                                    pop();
                                    break;
                                case 3:
                                    display();
                                    break;
                                case 4:
                                    exit(0);
                                default:
                                    printf("Invalid choice!\n");
                            }
                        }
                        return 0;
                    }
                """.trimIndent(),
                blueprintBlock = "#include <stdio.h>\n#include <stdlib.h>\n#define MAX 5\n\nint stack[MAX];\nint top = -1;",
                coreFunctionBlock = """
                    void push(int item) {
                        if (top == MAX - 1) {
                            printf("Stack Overflow!\n");
                            return;
                        }
                        stack[++top] = item;
                    }

                    void pop() {
                        if (top == -1) {
                            printf("Stack Underflow!\n");
                            return;
                        }
                        printf("Popped: %d\n", stack[top--]);
                    }
                """.trimIndent(),
                guardBlock = "if (top == MAX - 1) // Overflow guard\nif (top == -1)      // Underflow guard",
                driverBlock = "int main() {\n    // Menu loop with switch-case 1..4\n}",
                algorithmSteps = listOf(
                    "Check for Overflow condition: `top == MAX - 1`",
                    "Increment `top` pointer: `++top`",
                    "Store element at new index: `stack[top] = item`",
                    "For Pop: Check for Underflow condition: `top == -1`",
                    "Retrieve element `stack[top]` and decrement `top--`"
                ),
                vivaQuestions = listOf(
                    "What is the time complexity of push and pop?" to "O(1) constant time, as we only manipulate the top index.",
                    "Why initialize top = -1 instead of 0?" to "Because in 0-indexed C arrays, index 0 is valid. top = -1 represents an empty stack.",
                    "What is the difference between prefix (++top) and postfix (top++)?" to "++top increments before accessing the array; top++ would write to the current index before incrementing, causing off-by-one errors."
                ),
                crashTraps = listOf(
                    "Writing to `stack[top++]` instead of `stack[++top]` during push",
                    "Missing `top == -1` check before popping (causes array out-of-bounds)",
                    "Forgetting `break` in switch statements causing fall-through"
                )
            ),
            LabProgramItem(
                programNumber = 2,
                title = "Program 2: Infix to Postfix Expression Conversion Using Stack",
                shortTitle = "Prog 2: Infix to Postfix",
                language = "C",
                fullCode = """
                    #include <stdio.h>
                    #include <ctype.h>
                    #define MAX 50

                    char stack[MAX];
                    int top = -1;

                    void push(char c) { stack[++top] = c; }
                    char pop() { return (top == -1) ? '#' : stack[top--]; }
                    char peek() { return (top == -1) ? '#' : stack[top]; }

                    int precedence(char op) {
                        if (op == '+' || op == '-') return 1;
                        if (op == '*' || op == '/' || op == '%') return 2;
                        if (op == '^') return 3;
                        return 0;
                    }

                    void infixToPostfix(char infix[], char postfix[]) {
                        int i = 0, k = 0;
                        char ch;
                        while ((ch = infix[i++]) != '\0') {
                            if (isalnum(ch)) {
                                postfix[k++] = ch;
                            } else if (ch == '(') {
                                push(ch);
                            } else if (ch == ')') {
                                while (top != -1 && peek() != '(') {
                                    postfix[k++] = pop();
                                }
                                pop(); // Remove '('
                            } else {
                                while (top != -1 && precedence(peek()) >= precedence(ch)) {
                                    postfix[k++] = pop();
                                }
                                push(ch);
                            }
                        }
                        while (top != -1) {
                            postfix[k++] = pop();
                        }
                        postfix[k] = '\0';
                    }

                    int main() {
                        char infix[MAX], postfix[MAX];
                        printf("Enter Infix expression: ");
                        scanf("%s", infix);
                        infixToPostfix(infix, postfix);
                        printf("Postfix expression: %s\n", postfix);
                        return 0;
                    }
                """.trimIndent(),
                blueprintBlock = "#include <stdio.h>\n#include <ctype.h>\n#define MAX 50\nchar stack[MAX];\nint top = -1;",
                coreFunctionBlock = """
                    void infixToPostfix(char infix[], char postfix[]) {
                        int i = 0, k = 0;
                        char ch;
                        while ((ch = infix[i++]) != '\0') {
                            if (isalnum(ch)) postfix[k++] = ch;
                            else if (ch == '(') push(ch);
                            else if (ch == ')') {
                                while (top != -1 && peek() != '(') postfix[k++] = pop();
                                pop();
                            } else {
                                while (top != -1 && precedence(peek()) >= precedence(ch))
                                    postfix[k++] = pop();
                                push(ch);
                            }
                        }
                        while (top != -1) postfix[k++] = pop();
                        postfix[k] = '\0';
                    }
                """.trimIndent(),
                guardBlock = "while (top != -1 && peek() != '(') // Parenthesis balance\npostfix[k] = '\\0'; // Null-terminate result",
                driverBlock = "int main() {\n    char infix[MAX], postfix[MAX];\n    scanf(\"%s\", infix);\n    infixToPostfix(infix, postfix);\n    printf(\"%s\\n\", postfix);\n}",
                algorithmSteps = listOf(
                    "Scan infix expression from left to right",
                    "If operand (alphanumeric): directly append to postfix output",
                    "If `(`: push onto operator stack",
                    "If `)`: pop and output until matching `(` is encountered",
                    "If operator: pop operators of higher or equal precedence, then push current operator",
                    "Pop remaining operators and append null terminator `\\0`"
                ),
                vivaQuestions = listOf(
                    "Why do computers convert infix to postfix before evaluation?" to "Postfix expressions require no parenthesis and have clear operator order, allowing single-pass evaluation via a stack in O(n) time.",
                    "Why is operator associativity important?" to "For operators with the same precedence (e.g. + and -), left-to-right associativity ensures the earlier operator is evaluated first."
                ),
                crashTraps = listOf(
                    "Forgetting to null-terminate `postfix[k] = '\\0'`, printing garbage memory",
                    "Not popping the opening parenthesis `(` from the stack",
                    "Operator precedence order inversion (assigning ^ lower precedence than *)"
                )
            ),
            LabProgramItem(
                programNumber = 3,
                title = "Program 3: Circular Queue with Modulo Arithmetic",
                shortTitle = "Prog 3: Circular Queue",
                language = "C",
                fullCode = """
                    #include <stdio.h>
                    #include <stdlib.h>
                    #define MAX 5

                    int queue[MAX];
                    int front = -1, rear = -1;

                    void insert(int item) {
                        if ((rear + 1) % MAX == front) {
                            printf("Queue Overflow! Queue is full\n");
                            return;
                        }
                        if (front == -1) front = 0;
                        rear = (rear + 1) % MAX;
                        queue[rear] = item;
                        printf("Inserted %d\n", item);
                    }

                    void deleteItem() {
                        if (front == -1) {
                            printf("Queue Underflow! Queue is empty\n");
                            return;
                        }
                        printf("Deleted %d\n", queue[front]);
                        if (front == rear) {
                            front = -1;
                            rear = -1;
                        } else {
                            front = (front + 1) % MAX;
                        }
                    }

                    void display() {
                        if (front == -1) {
                            printf("Queue is empty\n");
                            return;
                        }
                        printf("Queue elements:\n");
                        int i = front;
                        while (1) {
                            printf("%d ", queue[i]);
                            if (i == rear) break;
                            i = (i + 1) % MAX;
                        }
                        printf("\n");
                    }

                    int main() {
                        int choice, val;
                        while (1) {
                            printf("\n--- CIRCULAR QUEUE ---\n1. Insert\n2. Delete\n3. Display\n4. Exit\nChoice: ");
                            scanf("%d", &choice);
                            switch (choice) {
                                case 1: printf("Enter value: "); scanf("%d", &val); insert(val); break;
                                case 2: deleteItem(); break;
                                case 3: display(); break;
                                case 4: exit(0);
                            }
                        }
                        return 0;
                    }
                """.trimIndent(),
                blueprintBlock = "#include <stdio.h>\n#define MAX 5\nint queue[MAX];\nint front = -1, rear = -1;",
                coreFunctionBlock = """
                    void insert(int item) {
                        if ((rear + 1) % MAX == front) { printf("Overflow!\n"); return; }
                        if (front == -1) front = 0;
                        rear = (rear + 1) % MAX;
                        queue[rear] = item;
                    }

                    void deleteItem() {
                        if (front == -1) { printf("Underflow!\n"); return; }
                        printf("Deleted: %d\n", queue[front]);
                        if (front == rear) { front = -1; rear = -1; }
                        else front = (front + 1) % MAX;
                    }
                """.trimIndent(),
                guardBlock = "if ((rear + 1) % MAX == front) // Circular Overflow\nif (front == -1)               // Underflow\nif (front == rear) front = rear = -1; // Reset when empty",
                driverBlock = "int main() {\n    // Menu loop for insert, delete, display\n}",
                algorithmSteps = listOf(
                    "Check circular overflow condition: `(rear + 1) % MAX == front`",
                    "If queue is empty (`front == -1`), initialize `front = 0`",
                    "Advance rear pointer using circular modulo: `rear = (rear + 1) % MAX`",
                    "For deletion: verify underflow (`front == -1`)",
                    "If single element remains (`front == rear`), reset both to `-1`",
                    "Else advance front pointer: `front = (front + 1) % MAX`"
                ),
                vivaQuestions = listOf(
                    "What advantage does a Circular Queue have over a Linear Queue?" to "In a linear queue, freed space at the front cannot be reused once rear reaches MAX-1 (false overflow). Circular queues wrap around to index 0, utilizing all memory.",
                    "Why must front and rear be reset to -1 when the last item is deleted?" to "To indicate that the queue has returned to an empty state, allowing the next insertion to initialize front = 0."
                ),
                crashTraps = listOf(
                    "Using `rear = rear + 1` instead of `(rear + 1) % MAX` causing array overflow",
                    "Forgetting the single-element reset `if (front == rear) front = rear = -1;`",
                    "Display loop infinite cycling if loop termination `i == rear` is missed"
                )
            ),
            LabProgramItem(
                programNumber = 4,
                title = "Program 4: Singly Linked List (Insert Front, Delete End, Display, Count)",
                shortTitle = "Prog 4: Singly Linked List",
                language = "C",
                fullCode = """
                    #include <stdio.h>
                    #include <stdlib.h>

                    struct Node {
                        int data;
                        struct Node* next;
                    };

                    struct Node* head = NULL;

                    void insertFront(int item) {
                        struct Node* newNode = (struct Node*)malloc(sizeof(struct Node));
                        if (!newNode) { printf("Heap Exhausted!\n"); return; }
                        newNode->data = item;
                        newNode->next = head;
                        head = newNode;
                        printf("Inserted %d at front\n", item);
                    }

                    void deleteEnd() {
                        if (head == NULL) {
                            printf("List is empty\n");
                            return;
                        }
                        if (head->next == NULL) {
                            printf("Deleted %d\n", head->data);
                            free(head);
                            head = NULL;
                            return;
                        }
                        struct Node* temp = head;
                        struct Node* prev = NULL;
                        while (temp->next != NULL) {
                            prev = temp;
                            temp = temp->next;
                        }
                        printf("Deleted %d\n", temp->data);
                        prev->next = NULL;
                        free(temp);
                    }

                    void display() {
                        if (head == NULL) { printf("List is empty\n"); return; }
                        struct Node* temp = head;
                        printf("List: ");
                        while (temp != NULL) {
                            printf("%d -> ", temp->data);
                            temp = temp->next;
                        }
                        printf("NULL\n");
                    }

                    int main() {
                        int ch, val;
                        while (1) {
                            printf("\n--- SLL MENU ---\n1. Insert Front\n2. Delete End\n3. Display\n4. Exit\nChoice: ");
                            scanf("%d", &ch);
                            switch (ch) {
                                case 1: printf("Val: "); scanf("%d", &val); insertFront(val); break;
                                case 2: deleteEnd(); break;
                                case 3: display(); break;
                                case 4: exit(0);
                            }
                        }
                        return 0;
                    }
                """.trimIndent(),
                blueprintBlock = "struct Node {\n    int data;\n    struct Node* next;\n};\nstruct Node* head = NULL;",
                coreFunctionBlock = """
                    void insertFront(int item) {
                        struct Node* newNode = (struct Node*)malloc(sizeof(struct Node));
                        newNode->data = item;
                        newNode->next = head;
                        head = newNode;
                    }

                    void deleteEnd() {
                        if (!head) return;
                        if (!head->next) { free(head); head = NULL; return; }
                        struct Node *temp = head, *prev = NULL;
                        while (temp->next) { prev = temp; temp = temp->next; }
                        prev->next = NULL;
                        free(temp);
                    }
                """.trimIndent(),
                guardBlock = "if (!newNode) return; // Malloc failure\nif (!head) return;    // Empty list check\nif (!head->next) ...  // Single node deletion",
                driverBlock = "int main() {\n    // Linked list operations menu\n}",
                algorithmSteps = listOf(
                    "Allocate new node memory using `malloc(sizeof(struct Node))`",
                    "Populate `newNode->data = item` and link `newNode->next = head`",
                    "Update `head = newNode`",
                    "For Delete End: Check if empty (`head == NULL`)",
                    "If single node: `free(head)` and reset `head = NULL`",
                    "Else traverse with `temp` and `prev` until `temp->next == NULL`",
                    "Set `prev->next = NULL` and invoke `free(temp)`"
                ),
                vivaQuestions = listOf(
                    "What happens if you do not call free(temp) after unlinking a node?" to "Memory leak occurs. The memory remains occupied on the heap and cannot be reclaimed by the OS until the process terminates.",
                    "What is the time complexity of insertFront vs deleteEnd?" to "insertFront is O(1) constant time; deleteEnd is O(n) because we must traverse the entire list to reach the second-to-last node."
                ),
                crashTraps = listOf(
                    "Dereferencing `temp->next` without verifying `temp != NULL` (Segmentation Fault)",
                    "Forgetting to update `prev->next = NULL` after freeing `temp` (Dangling pointer)",
                    "Forgetting `head = NULL` when deleting the sole node in the list"
                )
            ),
            LabProgramItem(
                programNumber = 5,
                title = "Program 5: Doubly Linked List with Header Node",
                shortTitle = "Prog 5: Doubly Linked List",
                language = "C",
                fullCode = """
                    #include <stdio.h>
                    #include <stdlib.h>

                    struct Node {
                        int ssn;
                        char name[20];
                        float sal;
                        struct Node* prev;
                        struct Node* next;
                    };

                    struct Node* head = NULL;

                    void insertEnd(int ssn, float sal) {
                        struct Node* newNode = (struct Node*)malloc(sizeof(struct Node));
                        newNode->ssn = ssn;
                        newNode->sal = sal;
                        newNode->next = NULL;
                        newNode->prev = NULL;

                        if (head == NULL) {
                            head = newNode;
                            return;
                        }
                        struct Node* temp = head;
                        while (temp->next != NULL) {
                            temp = temp->next;
                        }
                        temp->next = newNode;
                        newNode->prev = temp;
                    }

                    void deleteFront() {
                        if (head == NULL) {
                            printf("DLL is empty\n");
                            return;
                        }
                        struct Node* temp = head;
                        head = head->next;
                        if (head != NULL) {
                            head->prev = NULL;
                        }
                        printf("Deleted SSN: %d\n", temp->ssn);
                        free(temp);
                    }

                    void display() {
                        if (head == NULL) { printf("DLL is empty\n"); return; }
                        struct Node* temp = head;
                        int count = 0;
                        printf("\n--- DLL NODES ---\n");
                        while (temp != NULL) {
                            printf("[SSN: %d, Sal: %.2f] <-> ", temp->ssn, temp->sal);
                            count++;
                            temp = temp->next;
                        }
                        printf("NULL (Total: %d)\n", count);
                    }

                    int main() {
                        insertEnd(101, 55000);
                        insertEnd(102, 62000);
                        display();
                        deleteFront();
                        display();
                        return 0;
                    }
                """.trimIndent(),
                blueprintBlock = "struct Node {\n    int ssn;\n    float sal;\n    struct Node* prev;\n    struct Node* next;\n};\nstruct Node* head = NULL;",
                coreFunctionBlock = """
                    void insertEnd(int ssn, float sal) {
                        struct Node* newNode = (struct Node*)malloc(sizeof(struct Node));
                        newNode->ssn = ssn; newNode->sal = sal;
                        newNode->next = newNode->prev = NULL;
                        if (!head) { head = newNode; return; }
                        struct Node* temp = head;
                        while (temp->next) temp = temp->next;
                        temp->next = newNode;
                        newNode->prev = temp;
                    }
                """.trimIndent(),
                guardBlock = "if (head != NULL) head->prev = NULL; // Update new head prev pointer\nif (!newNode) return; // Malloc validation",
                driverBlock = "int main() {\n    // Demo of DLL operations\n}",
                algorithmSteps = listOf(
                    "Allocate `newNode` with both `prev` and `next` set to `NULL`",
                    "If empty (`head == NULL`), set `head = newNode`",
                    "Traverse to the tail node: `while (temp->next != NULL)`",
                    "Establish forward link: `temp->next = newNode`",
                    "Establish backward link: `newNode->prev = temp`",
                    "For deleteFront: Update `head = head->next` and `head->prev = NULL`"
                ),
                vivaQuestions = listOf(
                    "Why use a Doubly Linked List over a Singly Linked List?" to "Enables bidirectional traversal (forward and backward) and allows O(1) deletion of a node when its pointer is directly given.",
                    "What is the memory overhead of a DLL?" to "Each node stores two pointers (prev and next), using 16 extra bytes per node on 64-bit systems compared to SLL."
                ),
                crashTraps = listOf(
                    "Setting `head->prev = NULL` when `head` became `NULL` (null pointer dereference)",
                    "Forgetting the backward link `newNode->prev = temp`, breaking reverse traversal"
                )
            ),
            LabProgramItem(
                programNumber = 6,
                title = "Program 6: Binary Search Tree (Insert, Search, Inorder, Preorder, Postorder)",
                shortTitle = "Prog 6: Binary Search Tree",
                language = "C",
                fullCode = """
                    #include <stdio.h>
                    #include <stdlib.h>

                    struct BSTNode {
                        int data;
                        struct BSTNode* left;
                        struct BSTNode* right;
                    };

                    struct BSTNode* createNode(int val) {
                        struct BSTNode* n = (struct BSTNode*)malloc(sizeof(struct BSTNode));
                        n->data = val;
                        n->left = n->right = NULL;
                        return n;
                    }

                    struct BSTNode* insert(struct BSTNode* root, int val) {
                        if (root == NULL) return createNode(val);
                        if (val < root->data)
                            root->left = insert(root->left, val);
                        else if (val > root->data)
                            root->right = insert(root->right, val);
                        return root;
                    }

                    void inorder(struct BSTNode* root) {
                        if (root != NULL) {
                            inorder(root->left);
                            printf("%d ", root->data);
                            inorder(root->right);
                        }
                    }

                    int search(struct BSTNode* root, int key) {
                        if (root == NULL) return 0;
                        if (root->data == key) return 1;
                        if (key < root->data) return search(root->left, key);
                        return search(root->right, key);
                    }

                    int main() {
                        struct BSTNode* root = NULL;
                        root = insert(root, 50);
                        insert(root, 30);
                        insert(root, 70);
                        insert(root, 20);
                        insert(root, 40);

                        printf("Inorder Traversal (Sorted): ");
                        inorder(root);
                        printf("\nSearch 40: %s\n", search(root, 40) ? "FOUND" : "NOT FOUND");
                        return 0;
                    }
                """.trimIndent(),
                blueprintBlock = "struct BSTNode {\n    int data;\n    struct BSTNode *left, *right;\n};",
                coreFunctionBlock = """
                    struct BSTNode* insert(struct BSTNode* root, int val) {
                        if (root == NULL) return createNode(val);
                        if (val < root->data) root->left = insert(root->left, val);
                        else if (val > root->data) root->right = insert(root->right, val);
                        return root;
                    }

                    void inorder(struct BSTNode* root) {
                        if (!root) return;
                        inorder(root->left);
                        printf("%d ", root->data);
                        inorder(root->right);
                    }
                """.trimIndent(),
                guardBlock = "if (root == NULL) return createNode(val); // Base recursion case",
                driverBlock = "int main() {\n    // BST insertion and traversal demo\n}",
                algorithmSteps = listOf(
                    "Base case: if current `root == NULL`, create and return new node",
                    "If `val < root->data`, recursively call `insert(root->left, val)`",
                    "If `val > root->data`, recursively call `insert(root->right, val)`",
                    "Inorder traversal: Traverse Left Subtree &rarr; Print Root &rarr; Traverse Right Subtree",
                    "Search: Compare key against current node, prune left or right subtree"
                ),
                vivaQuestions = listOf(
                    "What special property does Inorder traversal of a BST have?" to "It always prints the elements in ascending sorted order.",
                    "What is the worst-case time complexity of search in a BST?" to "O(n) when the tree becomes skewed (degenerate like a linked list). In balanced BST, it is O(log n)."
                ),
                crashTraps = listOf(
                    "Missing `return root;` at the end of recursive insert function",
                    "Forgetting base case `if (root == NULL)` in traversals causing infinite stack recursion"
                )
            ),
            LabProgramItem(
                programNumber = 7,
                title = "Program 7: Graph Traversal using Breadth First Search (BFS)",
                shortTitle = "Prog 7: Graph BFS",
                language = "C",
                fullCode = """
                    #include <stdio.h>
                    #define MAX 10

                    int adj[MAX][MAX];
                    int visited[MAX];
                    int queue[MAX];
                    int front = 0, rear = -1;

                    void bfs(int start, int n) {
                        visited[start] = 1;
                        queue[++rear] = start;

                        printf("BFS Traversal: ");
                        while (front <= rear) {
                            int curr = queue[front++];
                            printf("%d ", curr);

                            for (int i = 0; i < n; i++) {
                                if (adj[curr][i] == 1 && !visited[i]) {
                                    visited[i] = 1;
                                    queue[++rear] = i;
                                }
                            }
                        }
                        printf("\n");
                    }

                    int main() {
                        int n = 4;
                        // Sample directed graph
                        adj[0][1] = 1; adj[0][2] = 1;
                        adj[1][2] = 1; adj[2][0] = 1;
                        adj[2][3] = 1; adj[3][3] = 1;

                        bfs(2, n);
                        return 0;
                    }
                """.trimIndent(),
                blueprintBlock = "#define MAX 10\nint adj[MAX][MAX];\nint visited[MAX];\nint queue[MAX];\nint front = 0, rear = -1;",
                coreFunctionBlock = """
                    void bfs(int start, int n) {
                        visited[start] = 1;
                        queue[++rear] = start;
                        while (front <= rear) {
                            int curr = queue[front++];
                            printf("%d ", curr);
                            for (int i = 0; i < n; i++) {
                                if (adj[curr][i] && !visited[i]) {
                                    visited[i] = 1;
                                    queue[++rear] = i;
                                }
                            }
                        }
                    }
                """.trimIndent(),
                guardBlock = "if (adj[curr][i] == 1 && !visited[i]) // Unvisited neighbor guard",
                driverBlock = "int main() {\n    // Graph matrix input and BFS call\n}",
                algorithmSteps = listOf(
                    "Mark starting vertex as visited and enqueue it",
                    "While queue is not empty (`front <= rear`):",
                    "Dequeue front vertex and print it",
                    "Inspect all adjacent vertices in adjacency matrix",
                    "For each unvisited neighbor (`!visited[i]`), mark visited and enqueue"
                ),
                vivaQuestions = listOf(
                    "Why does BFS use a Queue while DFS uses a Stack?" to "BFS explores vertices level-by-level (FIFO), requiring a queue. DFS explores as deep as possible along each branch (LIFO), requiring a stack or recursion.",
                    "What is the time complexity of BFS using an adjacency matrix?" to "O(V^2), because for each vertex we check all V columns of the matrix. With an adjacency list, it is O(V + E)."
                ),
                crashTraps = listOf(
                    "Forgetting to mark `visited[i] = 1` when pushing to queue, causing redundant duplicate visits and infinite queue overflow in cyclic graphs",
                    "Off-by-one error in 0-indexed vs 1-indexed vertex inputs"
                )
            ),
            LabProgramItem(
                programNumber = 8,
                title = "Program 8: Hash Table with Linear Probing",
                shortTitle = "Prog 8: Hash Table",
                language = "C",
                fullCode = """
                    #include <stdio.h>
                    #include <stdlib.h>
                    #define SIZE 10

                    int hashKey(int key) {
                        return key % SIZE;
                    }

                    void insert(int table[], int key) {
                        int idx = hashKey(key);
                        int start = idx;
                        while (table[idx] != -1) {
                            idx = (idx + 1) % SIZE;
                            if (idx == start) {
                                printf("Hash Table Full! Cannot insert %d\n", key);
                                return;
                            }
                        }
                        table[idx] = key;
                        printf("Key %d inserted at index %d\n", key, idx);
                    }

                    void display(int table[]) {
                        printf("\n--- HASH TABLE ---\n");
                        for (int i = 0; i < SIZE; i++) {
                            if (table[i] != -1) printf("[%d] -> %d\n", i, table[i]);
                            else printf("[%d] -> EMPTY\n", i);
                        }
                    }

                    int main() {
                        int table[SIZE];
                        for (int i = 0; i < SIZE; i++) table[i] = -1;

                        insert(table, 23);
                        insert(table, 43); // Collision at index 3 -> Linear probes to 4
                        insert(table, 13);
                        display(table);
                        return 0;
                    }
                """.trimIndent(),
                blueprintBlock = "#define SIZE 10\nint hashKey(int key) { return key % SIZE; }",
                coreFunctionBlock = """
                    void insert(int table[], int key) {
                        int idx = hashKey(key);
                        int start = idx;
                        while (table[idx] != -1) {
                            idx = (idx + 1) % SIZE;
                            if (idx == start) { printf("Table Full!\n"); return; }
                        }
                        table[idx] = key;
                    }
                """.trimIndent(),
                guardBlock = "if (idx == start) return; // Table full loop guard\ntable[i] = -1; // Empty slot sentinel",
                driverBlock = "int main() {\n    // Initialize table to -1 and insert keys\n}",
                algorithmSteps = listOf(
                    "Compute hash index: `idx = key % SIZE`",
                    "If `table[idx]` is empty (`-1`), insert key immediately",
                    "On collision: Advance to next index circularly `(idx + 1) % SIZE`",
                    "Repeat probing until empty slot is found or entire table is traversed (`idx == start`)",
                    "Store key at resolved slot"
                ),
                vivaQuestions = listOf(
                    "What is primary clustering in Linear Probing?" to "When multiple keys hash to the same or nearby slots, contiguous occupied blocks grow longer, increasing average search time.",
                    "What are common collision resolution alternatives to linear probing?" to "Quadratic probing, Double hashing, and Separate chaining using linked lists."
                ),
                crashTraps = listOf(
                    "Infinite while loop on full table if `idx == start` full-table check is omitted",
                    "Not initializing all slots to `-1` before operations"
                )
            )
        )
    }
}


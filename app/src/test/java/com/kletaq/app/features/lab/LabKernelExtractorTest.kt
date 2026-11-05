package com.kletaq.app.features.lab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LabKernelExtractorTest {

    private val sampleBookCode = """
        #include <stdio.h>
        #include <stdlib.h>
        #include <string.h>

        typedef struct {
            int id;
            char title[50];
            char author[50];
            float price;
            char status[15];
        } Book;

        Book *b = NULL;
        int n = 0;

        void create() {
            printf("Enter number of books: ");
            scanf("%d", &n);
            b = (Book *)realloc(b, n * sizeof(Book));
            if (!b) { printf("Memory allocation failed!\n"); exit(1); }
            for (int i = 0; i < n; i++) {
                printf("Book %d\nID: ", i+1);
                scanf("%d", &b[i].id);
                printf("Book Title: "); scanf(" %[^\n]", b[i].title);
                strcpy(b[i].status, "Available");
            }
        }

        void display() {
            if (!b) { printf("No records!\n"); return; }
            for (int i = 0; i < n; i++) {
                printf("%d\t%s\t%s\n", b[i].id, b[i].title, b[i].status);
            }
        }

        void search(int id) {
            for (int i = 0; i < n; i++) {
                if (b[i].id == id) {
                    printf("Found book: %s\n", b[i].title);
                    return;
                }
            }
            printf("Book not found!\n");
        }

        void issueReturn(int issue, int id) {
            for (int i = 0; i < n; i++) {
                if (b[i].id == id) {
                    if (issue) {
                        if (strcmp(b[i].status, "Issued") == 0) {
                            printf("Already issued!\n");
                        } else {
                            strcpy(b[i].status, "Issued");
                            printf("Issued!\n");
                        }
                    } else {
                        strcpy(b[i].status, "Available");
                        printf("Returned!\n");
                    }
                    return;
                }
            }
            printf("Book ID not found!\n");
        }

        int main() {
            int ch, id;
            do {
                printf("1.Create 2.Display 3.Search 4.Issue 5.Return 6.Exit: ");
                scanf("%d", &ch);
                switch(ch) {
                    case 1: create(); break;
                    case 2: display(); break;
                    case 3: scanf("%d", &id); search(id); break;
                    case 4: scanf("%d", &id); issueReturn(1, id); break;
                    case 5: scanf("%d", &id); issueReturn(0, id); break;
                    case 6: free(b); break;
                }
            } while (ch != 6);
            return 0;
        }
    """.trimIndent()

    @Test
    fun testDeconstructIntoKernels() {
        val (kernels, _) = LabKernelExtractor.extractKernelsAndDrills(
            fullCode = sampleBookCode,
            title = "Book Records (Struct, Dynamic Allocation)",
            language = "c"
        )

        assertTrue("Should extract at least 4 kernels", kernels.size >= 4)
        assertEquals("1. Headers, Struct & Global State", kernels[0].name)
        assertTrue(kernels[0].codeSnippet.contains("typedef struct"))
        assertTrue(kernels.any { it.name.contains("create") })
        assertTrue(kernels.any { it.name.contains("display") || it.name.contains("search") })
        assertTrue(kernels.any { it.name.contains("main") })
    }

    @Test
    fun testDrillsGeneratedWithoutHardcoding() {
        val (_, drills) = LabKernelExtractor.extractKernelsAndDrills(
            fullCode = sampleBookCode,
            title = "Book Records",
            language = "c",
            inputOutputSample = "Book 1\nID: 23\nTitle: ty\nAuthor: ui\nPrice: 900"
        )

        assertNotNull("Drills should not be null", drills)
        assertNotNull("Parsons puzzle should not be null", drills.parsonsPuzzle)
        val parsons = drills.parsonsPuzzle!!
        assertTrue("Parsons lines should be sliced from real code", parsons.correctLines.isNotEmpty())
        assertTrue("Parsons should have at least 4 lines", parsons.correctLines.size >= 4)
        assertFalse("Scrambled should differ from correct", parsons.scrambledLines == parsons.correctLines)

        assertNotNull("Cloze drill should not be null", drills.clozeDrill)
        val cloze = drills.clozeDrill!!
        assertTrue("Cloze code should contain masked tokens", cloze.codeTemplate.contains("[ ___"))
        assertTrue("Cloze should extract real structural tokens", cloze.blanks.isNotEmpty())
    }

    @Test
    fun testPdfStreamDecompression() {
        val pdfFile = java.io.File("scratch/shorten_dsa.pdf")
        if (!pdfFile.exists()) return
        val bytes = pdfFile.readBytes()

        val text = PdfStreamTextExtractor.extractText(bytes)
        assertTrue("Must extract PROGRAM 1", text.contains("PROGRAM 1"))
        assertTrue("Must extract PROGRAM 2", text.contains("PROGRAM 2"))
        assertTrue("Must extract typedef struct", text.contains("typedef struct"))

        val programs = LabManualAiParser.parseRawPdfText(text)
        assertEquals("Should parse at least 2 programs from PDF", 2, programs.size)

        val p1 = programs[0]
        assertEquals(1, p1.programNumber)
        assertTrue("Prog 1 code must contain create", p1.fullCode.contains("void create"))
        assertTrue("Prog 1 code must contain issueReturn", p1.fullCode.contains("issueReturn"))
        assertTrue("Prog 1 code must contain main", p1.fullCode.contains("int main"))
        assertTrue("Prog 1 must have kernels", p1.kernels.size >= 4)
        assertNotNull("Prog 1 must have drills", p1.drills)

        val p2 = programs[1]
        assertEquals(2, p2.programNumber)
        assertTrue("Prog 2 code must contain Term", p2.fullCode.contains("Term"))
        assertTrue("Prog 2 code must contain transpose", p2.fullCode.contains("transpose"))
        assertTrue("Prog 2 code must contain main", p2.fullCode.contains("int main"))
        assertTrue("Prog 2 must have kernels", p2.kernels.size >= 4)
        assertNotNull("Prog 2 must have drills", p2.drills)
    }
}

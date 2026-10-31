package com.kletaq.app.features.pdfstudio.data

enum class PdfToolType(
    val title: String,
    val subtitle: String
) {
    IMAGES_TO_PDF("Images to PDF", "Convert lecture notes & photos to a clean PDF"),
    COMPRESS_PDF("Compress PDF", "Reduce MB size for college portals & assignments"),
    MERGE_PDF("Merge PDFs", "Combine multiple question papers or notes into one"),
    SPLIT_PDF("Split & Extract", "Extract specific pages or assignment chapters"),
    PDF_TO_IMAGES("PDF to Images", "Export high-resolution images of each page"),
    INVERT_PDF("Dark Mode / Invert", "Night reading mode or ink-saving print mode"),
    COMPRESS_PHOTO("Compress Photo", "Shrink photo KB size for forms & portal uploads")
}

data class RecentPdfDocument(
    val id: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val toolType: PdfToolType,
    val timestamp: Long = System.currentTimeMillis()
)

data class PdfPageThumbnail(
    val pageIndex: Int,
    val width: Int,
    val height: Int,
    val isSelected: Boolean = false
)

enum class CompressionLevel(
    val label: String,
    val description: String,
    val maxDimension: Int,
    val jpegQuality: Int
) {
    AGGRESSIVE("Aggressive (Max Reduction)", "For strict 2MB/5MB portals. ~50% quality.", 1080, 50),
    BALANCED("Balanced (Recommended)", "Sharp text, compact file size. ~70% quality.", 1440, 70),
    LIGHT("Light Compression", "Preserves ultra-fine print. ~85% quality.", 1800, 85)
}

enum class PageMarginOption(
    val label: String,
    val marginPoints: Int
) {
    NONE("No Margin", 0),
    THIN("Thin (16pt)", 16),
    STANDARD("Standard (32pt)", 32)
}

enum class PageOrientationOption(
    val label: String
) {
    AUTO("Auto-Detect"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}


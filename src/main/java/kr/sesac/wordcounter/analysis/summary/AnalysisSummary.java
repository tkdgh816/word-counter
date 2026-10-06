package kr.sesac.wordcounter.analysis.summary;

import java.nio.file.Path;

public record AnalysisSummary(
        Path inputPath,
        FileAnalysisSummary fileSummary,
        WordAnalysisSummary wordSummary,
        long elapsedMilliseconds) {
}

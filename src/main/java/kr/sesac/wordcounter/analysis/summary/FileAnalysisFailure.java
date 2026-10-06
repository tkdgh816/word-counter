package kr.sesac.wordcounter.analysis.summary;

import java.nio.file.Path;

public record FileAnalysisFailure(
        Path filePath,
        String reason) {
}

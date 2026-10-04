package kr.sesac.wordcounter.report;

import kr.sesac.wordcounter.analysis.summary.AnalysisSummary;
import kr.sesac.wordcounter.analysis.summary.FileAnalysisFailure;
import kr.sesac.wordcounter.model.TokenCount;
import kr.sesac.wordcounter.text.writer.TsvWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ReportWriter {
    private ReportWriter() {
    }

    public static String generateFailureSummary(List<FileAnalysisFailure> failures) {
        StringBuilder summary = new StringBuilder();
        for (FileAnalysisFailure failure : failures) {
            summary.append(String.format("파일 분석 실패: %s%n", failure.filePath()));
            summary.append(String.format("원인: %s%n", failure.reason()));
        }
        return summary.toString();
    }

    public static String generateAnalysisSummary(AnalysisSummary analysisSummary) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("입력: %s%n", analysisSummary.inputPath()));
        summary.append(String.format("파일: 시도 %d개 / 성공 %d개 / 실패 %d개 / 건너뜀 %d개%n",
                analysisSummary.fileSummary().attemptedCount(),
                analysisSummary.fileSummary().successfulCount(),
                analysisSummary.fileSummary().failedCount(),
                analysisSummary.fileSummary().skippedCount()));
        summary.append(String.format("전체 단어: %d개 / 서로 다른 단어: %d개%n",
                analysisSummary.wordSummary().totalCount(),
                analysisSummary.wordSummary().uniqueCount()));
        summary.append(String.format("처리 시간: %dms%n", analysisSummary.elapsedMilliseconds()));
        return summary.toString();
    }

    public static void saveAsFile(Path output, List<TokenCount> tokenCounts) throws IOException {
        TsvWriter.save(output, TokenCount.class, tokenCounts, new String[]{"word", "count"});
    }
}

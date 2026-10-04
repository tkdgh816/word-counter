package kr.sesac.wordcounter.text.reader;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public enum TextReaderKind {
    TXT(TxtReader::new),
    CSV(() -> new CsvReader(List.of("text"))),
    TSV(() -> new TsvReader(List.of("document"))),
    HTML(() -> new HtmlReader("#content", "script, style, nav, header, footer"));

    private final Supplier<TextReader> supplier;

    TextReaderKind(Supplier<TextReader> supplier) {
        this.supplier = supplier;
    }

    public static TextReaderKind fromExtension(String extension) {
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "txt" -> TextReaderKind.TXT;
            case "csv" -> TextReaderKind.CSV;
            case "tsv" -> TextReaderKind.TSV;
            case "html", "htm" -> TextReaderKind.HTML;
            default -> throw new IllegalArgumentException("지원하지 않는 확장자입니다: " + extension);
        };
    }

    public TextReader createReader() {
        return supplier.get();
    }

    // 체크포인트로 파일 분석 결과 저장 시 분석 방법이 변경되었는지
    // 메타데이터에 기록하고 확인하기 위해 필요
    // TextReader의 분석 방식을 수정하였다면 버전도 바뀌어야 함
    public static final String VERSION = "2.0";
}

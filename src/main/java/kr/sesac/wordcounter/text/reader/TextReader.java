package kr.sesac.wordcounter.text.reader;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * 텍스트 기반 문서를 형식에 맞게 읽고 분석에 필요한 정보를 제공합니다.
 */
public interface TextReader {
    /**
     * 지정한 파일을 읽고 파싱한 결과를 하나씩 소비자에게 전달합니다.
     *
     */
    void parseText(Path filePath, Consumer<String> textConsumer);
}

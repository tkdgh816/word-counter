package kr.sesac.wordcounter.text.reader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.Selector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class HtmlReader implements TextReader {
    private final Evaluator excludedEvaluator;
    private final Evaluator targetEvaluator;

    public HtmlReader(String targetSelector, String excludedSelectors) {
        try {
            targetEvaluator = Selector.evaluatorOf(targetSelector);
            excludedEvaluator = Selector.evaluatorOf(excludedSelectors);
        } catch (Selector.SelectorParseException e) {
            throw new IllegalStateException("selector 파싱에 실패했습니다.", e);
        }
    }

    @Override
    public void parseText(Path filePath, Consumer<String> textConsumer) {
        try {
            // UTF8 형식으로 html 텍스트 가져오기
            String html = Files.readString(filePath, StandardCharsets.UTF_8);

            // html 파싱
            Document document = Jsoup.parse(html);

            // 본문 요소가 한 개인지 검사
            Elements match = document.select(targetEvaluator);
            if (match.size() != 1) {
                throw new TextReaderException(targetEvaluator + " 요소가 없거나 여러 개입니다");
            }

            // 원본에 영향을 주지 않도록 복사하고 제외 선택자 제거
            Element element = match.getFirst().clone();
            element.select(excludedEvaluator).remove();

            textConsumer.accept(element.text());
        } catch (IOException e) {
            throw new TextReaderException("HTML 문서를 읽을 수 없습니다.", e);
        }
    }
}

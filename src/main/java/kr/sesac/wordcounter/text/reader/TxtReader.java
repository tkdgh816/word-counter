package kr.sesac.wordcounter.text.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class TxtReader implements TextReader {
    @Override
    public void parseText(Path filePath, Consumer<String> textConsumer) {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;

            while ((line = reader.readLine()) != null) {
                textConsumer.accept(line);
            }
        } catch (IOException e) {
            throw new TextReaderException("TXT 문서를 읽을 수 없습니다.", e);
        }
    }
}

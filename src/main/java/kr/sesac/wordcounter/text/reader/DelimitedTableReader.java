package kr.sesac.wordcounter.text.reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class DelimitedTableReader implements TextReader {
    private final Set<String> targetHeaderNames;

    DelimitedTableReader(Collection<String> targetHeaderNames) {
        this.targetHeaderNames = targetHeaderNames.stream()
                .map(String::strip)
                .collect(Collectors.toSet());
    }

    abstract CSVFormat getFormat();

    @Override
    public void parseText(Path filePath, Consumer<String> textConsumer) {
        readSelectedRecords(filePath, (record, targetHeaderMap) -> {
            for (Integer targetHeaderIndex : targetHeaderMap.values()) {
                textConsumer.accept(record.get(targetHeaderIndex));
            }
        });
    }

    /**
     * 파일의 각 레코드에서 대상 헤더에 해당하는 셀만 추출하여 전달합니다.
     */
    public void readRecords(Path filePath, Consumer<Map<String, String>> recordConsumer) {
        readSelectedRecords(filePath, (record, targetHeaderMap) -> {
            Map<String, String> selectedRecord = new LinkedHashMap<>();

            for (var entry : targetHeaderMap.entrySet()) {
                selectedRecord.put(entry.getKey(), record.get(entry.getValue()));
            }

            recordConsumer.accept(Collections.unmodifiableMap(selectedRecord));
        });
    }

    private void readSelectedRecords(Path filePath, BiConsumer<CSVRecord, Map<String, Integer>> recordBiConsumer) {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser parser = getFormat().parse(reader)) {

            // 헤더 이름은 대소문자를 구분하며 앞뒤 공백을 제거해 비교
            // 헤더 이름은 비어 있지 않고 중복되지 않아야 함
            Map<String, Integer> rawHeaderMap = parser.getHeaderMap();
            Map<String, Integer> targetHeaderMap = getTargetHeaderMap(rawHeaderMap);

            try {
                for (CSVRecord record : parser) {
                    // 레코드의 셀 수가 헤더와 다른 경우
                    if (record.size() != rawHeaderMap.size()) {
                        throw new TextReaderException(
                                String.format("[%d행] %d번째 레코드의 셀 수(%d)가 헤더(%d)와 다릅니다.",
                                        parser.getCurrentLineNumber(),
                                        record.getRecordNumber(),
                                        record.size(),
                                        rawHeaderMap.size()));
                    }

                    recordBiConsumer.accept(record, targetHeaderMap);
                }
            } catch (UncheckedIOException e) {
                throw new TextReaderException(
                        String.format("[%d행] %d번째 레코드를 읽을 수 없습니다.",
                                parser.getCurrentLineNumber(),
                                parser.getRecordNumber() + 1),
                        e);
            }
        } catch (IOException e) {
            throw new TextReaderException("문서를 읽을 수 없습니다.", e);
        } catch (IllegalArgumentException e) {
            throw new TextReaderException("빈 헤더, 중복 헤더 등 잘못된 문서 형식입니다.", e);
        }
    }

    // 입력 헤더와 일치하는 파일 헤더 인덱스를 반환
    private Map<String, Integer> getTargetHeaderMap(Map<String, Integer> rawHeaderMap) {
        Map<String, Integer> targetHeaderMap = new LinkedHashMap<>();

        // 파일 헤더의 앞뒤 공백을 제거하고 입력 헤더와 일치하는 헤더 인덱스를 추가
        for (var headerEntry : rawHeaderMap.entrySet()) {
            String header = headerEntry.getKey().strip();
            if (targetHeaderNames.contains(header)) {
                targetHeaderMap.put(header, headerEntry.getValue());
            }
        }

        // 입력 헤더와 일치하는 파일 헤더가 없는지 확인
        if (targetHeaderMap.size() != targetHeaderNames.size()) {
            throw new TextReaderException(String.format("분석 열(%s)이 헤더에 없습니다.", targetHeaderNames));
        }

        return targetHeaderMap;
    }
}

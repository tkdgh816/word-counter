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
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser parser = getFormat().parse(reader)) {

            // 헤더 이름은 대소문자를 구분하며 앞뒤 공백을 제거해 비교
            // 헤더 이름은 비어 있지 않고 중복되지 않아야 함
            Map<String, Integer> rawHeaderMap = parser.getHeaderMap();
            List<Integer> targetHeaderIndexes = getTargetHeaderIndexes(rawHeaderMap);

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

                    for (Integer targetHeaderIndex : targetHeaderIndexes) {
                        textConsumer.accept(record.get(targetHeaderIndex));
                    }
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
        }
    }

    // 입력 헤더와 일치하는 파일 헤더 인덱스를 반환
    private List<Integer> getTargetHeaderIndexes(Map<String, Integer> rawHeaderMap) {
        List<Integer> targetHeaderIndexes = new ArrayList<>();

        // 파일 헤더의 앞뒤 공백을 제거하고 입력 헤더와 일치하는 헤더 인덱스를 추가
        for (var headerEntry : rawHeaderMap.entrySet()) {
            String header = headerEntry.getKey().strip();
            if (targetHeaderNames.contains(header)) {
                targetHeaderIndexes.add(headerEntry.getValue());
            }
        }

        // 입력 헤더와 일치하는 파일 헤더가 없는지 확인
        if (targetHeaderIndexes.size() != targetHeaderNames.size()) {
            throw new TextReaderException(String.format("분석 열(%s)이 헤더에 없습니다.", targetHeaderNames));
        }
        return targetHeaderIndexes;
    }


    public void readRecords(Path filePath, Consumer<CSVRecord> recordConsumer){
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser parser = getFormat().parse(reader)) {

            // 헤더 이름은 대소문자를 구분하며 앞뒤 공백을 제거해 비교
            // 헤더 이름은 비어 있지 않고 중복되지 않아야 함
            Map<String, Integer> rawHeaderMap = parser.getHeaderMap();

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

                    recordConsumer.accept(record);
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
        }
    }
}

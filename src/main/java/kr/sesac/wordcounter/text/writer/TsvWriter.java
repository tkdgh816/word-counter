package kr.sesac.wordcounter.text.writer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class TsvWriter {
    public static <T extends Record> void save(Path output, Class<T> recordType, List<T> records, String[] headers) throws IOException {
        if (Files.isDirectory(output)) {
            throw new IOException("출력 경로에 같은 이름의 디렉터리가 있습니다.");
        }

        if (!recordType.isRecord()) {
            throw new IllegalArgumentException("레코드 타입이 아닙니다.");
        }
        if (records.stream().anyMatch(record -> !recordType.isInstance(record))) {
            throw new IllegalArgumentException("records에 null 이거나 해당 타입이 아닌 요소가 있습니다.");
        }

        RecordComponent[] components = recordType.getRecordComponents();

        if (components.length != headers.length) {
            throw new IllegalArgumentException("헤더 개수와 레코드 컴포넌트 개수가 다릅니다.");
        }

        CSVFormat format = CSVFormat.TDF.builder().setHeader(headers).get();

        output = output.toAbsolutePath();
        Path parent = Files.createDirectories(output.getParent());

        // 임시 파일 생성해서 임시 파일에서 작업
        Path temp = Files.createTempFile(parent, output.getFileName().toString(), ".tmp");

        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
                for (T record : records) {
                    Object[] values = Arrays.stream(components)
                            .map(component -> {
                                try {
                                    return component.getAccessor().invoke(record);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new IllegalStateException("레코드 컴포넌트 값을 읽을 수 없습니다.", e);
                                }
                            })
                            .toArray();

                    csvPrinter.printRecord(values);
                }
            }

            try {
                Files.move(temp, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}

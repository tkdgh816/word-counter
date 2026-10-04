import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 심화 과제용 큰 입력 생성기. news-full.csv의 데이터 레코드를 N번 반복한 단일 파일과,
 * 같은 레코드를 P개 파일로 고르게 나눈 폴더를 data/local 아래에 만듭니다. data/local은 Git에 올라가지 않습니다.
 * 원본은 한 줄이 한 레코드인 CSV여야 합니다(제공된 news-full.csv가 그렇습니다).
 *
 * 프로젝트 루트에서 실행합니다.
 *   java tools/RepeatCsv.java          → 64번 반복, 16개 분할 (약 215MB)
 *   java tools/RepeatCsv.java 32 8     → 32번 반복, 8개 분할
 *   java tools/RepeatCsv.java 64 64    → 64번 반복, 64개 분할
 */
public class RepeatCsv {
    public static void main(String[] args) throws IOException {
        int repeat = args.length > 0 ? Integer.parseInt(args[0]) : 64;
        int parts = args.length > 1 ? Integer.parseInt(args[1]) : 16;
        Path source = Path.of(args.length > 2 ? args[2] : "data/klue-ynat/news-full.csv");

        if (repeat < 1 || parts < 1) {
            System.err.println("반복 횟수와 분할 수는 1 이상이어야 합니다.");
            System.exit(1);
        }
        if (!Files.isRegularFile(source)) {
            System.err.println("원본 파일이 없습니다: " + source + " (pom.xml이 있는 프로젝트 루트에서 실행하세요)");
            System.exit(1);
        }

        List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        String header = lines.get(0);
        List<String> records = lines.subList(1, lines.size());
        while (!records.isEmpty() && records.get(records.size() - 1).isEmpty()) {
            records = records.subList(0, records.size() - 1);
        }
        long total = (long) records.size() * repeat;

        Path outDir = Path.of("data/local/perf-" + repeat);
        Path manyDir = outDir.resolve("many");
        Files.createDirectories(manyDir);

        Path single = outDir.resolve("news-repeat-" + repeat + ".csv");
        try (BufferedWriter out = Files.newBufferedWriter(single, StandardCharsets.UTF_8)) {
            out.write(header);
            out.newLine();
            for (int i = 0; i < repeat; i++) {
                for (String record : records) {
                    out.write(record);
                    out.newLine();
                }
            }
        }

        // 반복한 레코드 전체를 P개 구간으로 고르게 나눕니다. 파일마다 헤더가 있습니다.
        int width = String.valueOf(parts).length();
        for (int p = 0; p < parts; p++) {
            long start = total * p / parts;
            long end = total * (p + 1) / parts;
            Path part = manyDir.resolve(String.format("news-%0" + Math.max(2, width) + "d.csv", p + 1));
            try (BufferedWriter out = Files.newBufferedWriter(part, StandardCharsets.UTF_8)) {
                out.write(header);
                out.newLine();
                for (long i = start; i < end; i++) {
                    out.write(records.get((int) (i % records.size())));
                    out.newLine();
                }
            }
        }

        System.out.printf("원본: %s (%,d건)%n", source, records.size());
        System.out.printf("생성: %s (%.1fMB, %,d건)%n", single, Files.size(single) / 1e6, total);
        System.out.printf("생성: %s (%d개 파일, 합치면 위 파일과 같은 내용)%n", manyDir, parts);
        System.out.println("두 입력의 분석 결과는 같아야 합니다. 원본이 news-full.csv이면 전체 단어 "
                + String.format("%,d", 321_084L * repeat) + "개, 종류 78,309개입니다.");
    }
}

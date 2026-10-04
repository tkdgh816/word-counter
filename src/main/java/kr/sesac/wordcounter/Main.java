package kr.sesac.wordcounter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 첫 실행용 코드입니다. TXT 내용을 읽은 뒤 TODO를 채우며 기능을 추가하세요.
 * 구현 기준은 docs/requirements.md에 있습니다.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        Path input = Path.of("samples/equivalent/basic.txt");

        System.out.println("문서 단어 분석기 - 시작 코드");
        System.out.println("입력 파일: " + input);
        System.out.println();

        // TODO 1: 단어별 출현 횟수를 저장할 자료구조를 준비하세요. (요구사항 4. 단어별 횟수 집계)
        try (BufferedReader reader =
                     Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                // TODO 2: line을 요구사항 3. 단어 처리 규칙대로 단어(토큰)로 나누세요.
                // TODO 3: 숫자만 있는 단어는 제외하고 단어별 횟수를 늘리세요.
            }
        }

        System.out.println();
        System.out.println("파일 읽기 성공. 다음 단계는 단어 분리와 카운팅입니다.");
        System.out.println("구현 후 전체 9개·6종인지 expected/basic-counts.tsv와 비교하세요.");
        // TODO 4: 원문 출력 대신 집계 결과를 출력하세요.
        // TXT 카운팅 완성 후 다른 형식, 메뉴, 오류 처리, 저장을 추가하세요.
    }
}

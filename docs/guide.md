# 개발 가이드

[README](../README.md) · [필수 요구사항](requirements.md) · [심화 요구사항](advanced.md) · [진행 기록과 발표](project-guide.md)

README의 '시작하기' 절을 따라 프로젝트를 열고 실행한 다음 읽는 문서입니다. **1~2절을 먼저 보고, 3~5절은 CSV·HTML을 추가할 때** 읽으면 됩니다. TODO 2에서 막히면 6절 힌트를 보세요. Maven 설정을 공부하거나 라이브러리 문서를 먼저 읽을 필요는 없습니다.

## 1. 실행이 안 될 때

| 증상 | 확인할 것 |
|---|---|
| Run 버튼이 없거나 JDK 오류 | Project SDK와 모듈 SDK가 JDK 21인지 확인 |
| Maven 프로젝트로 인식되지 않음 | `pom.xml`을 우클릭해 **Add as Maven Project** |
| 라이브러리가 빨간색으로 표시됨 | 인터넷 연결 후 Maven 도구 창에서 **Reload All Maven Projects** |
| `NoSuchFileException`으로 샘플을 못 찾음 | 실행 설정(Run → Edit Configurations)의 **Working directory**를 `pom.xml`이 있는 프로젝트 루트로 변경 |
| 한글이 깨짐 | 소스, 입력 파일, 실행 환경의 인코딩이 UTF-8인지 확인 |
| GitHub에서 404 또는 다운로드 불가 | 교육용 조직에 접근 가능한 계정으로 로그인했는지 확인 |

경로의 기준은 `Main.java`의 위치가 아니라 **프로그램의 작업 디렉터리**입니다. 이 과제에서는 `pom.xml`이 있는 폴더(프로젝트 루트)로 맞춥니다. 그래야 `samples/equivalent/basic.txt`, `expected/basic-counts.tsv`, `out/counts.tsv` 같은 상대 경로가 그대로 통합니다.

정상 실행되면 세 줄의 원문과 "파일 읽기 성공" 안내가 나옵니다. 나중에 메뉴를 만들면 **Run 창의 콘솔을 클릭해** 경로나 메뉴 번호를 입력합니다.

여기서 해결되지 않는 환경 문제는 수업에서 안내한 디스코드 채널에 올리세요.

## 2. 첫 구현: TODO 1~4

[Main.java](../src/main/java/kr/sesac/wordcounter/Main.java)의 시작 코드는 `Path`로 파일 위치를 지정하고, `Files.newBufferedReader`로 UTF-8 텍스트를 열어 `readLine()`으로 한 줄씩 읽습니다. 더 읽을 줄이 없으면 `null`이 나오고, `try (...)` 구문이 읽기 도구를 자동으로 닫습니다. 이 흐름을 유지하면서 `TODO` 주석 자리에 코드를 추가하세요. `TODO`는 "여기에 할 일이 남아 있다"는 표시일 뿐 Java 문법이 아닙니다.

1. TODO 1: 단어를 키, 횟수를 값으로 저장할 자료구조를 준비합니다.
2. TODO 2: 한 줄을 [단어 처리 규칙](requirements.md#3-단어-처리-규칙)에 따라 단어(토큰)로 나눕니다.
3. TODO 3: 유효한 단어가 나올 때마다 횟수를 늘립니다.
4. TODO 4: 원문 출력 대신 집계 결과를 출력해 [정답](../expected/basic-counts.tsv)과 비교합니다.

정답은 전체 단어 9개, 종류 6개입니다. 개수뿐 아니라 단어별 횟수도 확인하세요. 처음에는 `Main` 하나에 반복문으로 구현해도 됩니다. TXT 하나가 맞으면 **CSV·TSV·HTML → 조회·정렬·저장 → 여러 파일과 오류 처리 → 큰 데이터** 순서로 [필수 요구사항](requirements.md)을 따라갑니다.

## 3. 처음 쓰는 도구

| 도구 | 역할 |
|---|---|
| JDK 21 | Java 컴파일과 실행 |
| Maven | `pom.xml`을 읽어 라이브러리를 다운로드하고 빌드를 관리. IntelliJ가 대신 실행 |
| Apache Commons CSV 1.14.1 | CSV·TSV의 헤더, 따옴표, 여러 줄 값 해석 |
| jsoup 1.23.2 | HTML 요소를 찾고 그 안의 텍스트 추출 |

**라이브러리**는 다른 개발자가 만들어둔 기능을 내 코드에서 호출해 쓰도록 묶은 것입니다. `pom.xml`의 `<dependency>`(의존성) 항목에 어떤 라이브러리를 어느 버전으로 쓸지 적어 두면 Maven이 다운로드합니다. 두 라이브러리는 **이미 설정되어 있으니 다시 추가할 필요가 없고**, 처음에는 `pom.xml`을 수정하지 않아도 됩니다.

코드의 `import org.jsoup.Jsoup;`는 이미 다운로드한 클래스의 이름을 짧게 쓰기 위한 선언입니다. `import`만 적는다고 설치되지는 않습니다. `import`를 썼는데 클래스가 빨간색이면 1절의 Maven 새로고침을 확인하세요.

라이브러리가 하는 일은 파일 구조를 해석해 텍스트를 꺼내는 **파싱**까지입니다. 어떤 열·요소를 읽을지, 오류를 어떻게 처리할지, 어떤 단어를 세고 저장할지는 직접 구현합니다.

```text
CSV 파일 → Commons CSV로 text 열 추출 ┐
HTML 파일 → jsoup으로 본문 추출       ├→ 내 단어 분리 코드 → 내 집계 코드
TXT 파일 → Java 파일 읽기로 원문 추출 ┘
```

## 4. CSV 읽기 연습

CSV는 쉼표로, TSV는 탭으로 셀을 구분해 표를 텍스트로 저장한 형식입니다.

```csv
id,text,note
1,"Java, java!",연습
2,자바 공부,연습
```

첫 레코드 `id,text,note`가 **헤더**(열 이름), `1,"Java, java!",연습`이 **레코드**(데이터 한 건), `Java, java!`가 `text` 열의 **셀**입니다. `"Java, java!"` 안의 쉼표는 글의 일부라서 `split(",")`로 나누면 잘못 잘립니다. Commons CSV는 이 따옴표 규칙을 해석해 셀을 꺼내주는 라이브러리입니다. CSV에서는 따옴표 안에 줄바꿈도 들어갈 수 있어 파일의 한 줄과 레코드 한 건이 항상 같지는 않습니다.

`Main`과 같은 패키지(`kr.sesac.wordcounter`)에 `CsvPractice` 클래스를 만들어 이 클래스의 `main` 옆 Run으로 실행해보세요. 작업 디렉터리는 프로젝트 루트이고, 기존 `Main`은 그대로 두고 연습해도 됩니다.

```java
package kr.sesac.wordcounter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CsvPractice {
    public static void main(String[] args) throws IOException {
        var format = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (var reader = Files.newBufferedReader(
                    Path.of("samples/equivalent/basic.csv"), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                System.out.println(record.get("text"));
            }
        }
    }
}
```

- `CSVFormat.RFC4180`(CSV 표준 규칙 이름)은 CSV 읽기 규칙의 출발점이고, `builder()`로 설정을 고른 뒤 `get()`으로 완성합니다. 이 네 줄은 그대로 쓰면 됩니다.
- `setHeader()`는 첫 레코드를 열 이름으로 읽고, `setSkipHeaderRecord(true)`는 헤더를 데이터로 처리하지 않게 합니다.
- `record.get("text")`가 그 레코드의 `text` 셀입니다. 실행하면 기본 TXT와 같은 세 줄이 나오고 마지막 빈 셀은 빈 줄로 출력됩니다.
- `var`는 변수의 자료형을 컴파일러가 오른쪽 값에서 판단하게 하는 문법입니다.
- `try (...)`는 사용 후 닫아야 하는 자원을 자동으로 닫는 try-with-resources 문법입니다.
- `throws IOException`은 읽기 오류를 여기서 처리하지 않고 호출한 쪽으로 넘긴다는 뜻입니다.

이 예제에는 필수 열·셀 수 검사, 헤더 이름의 공백 처리, 오류 안내가 없으므로 [형식별 텍스트 추출](requirements.md#2-형식별-텍스트-추출) 규칙을 보고 보완해야 합니다. 형식 오류 처리는 6절 힌트를 참고하세요.

**TSV로 확장할 때**: 설정을 만드는 부분에서 `.get()` 앞에 `.setDelimiter('\t')`와 `.setQuote(null)`을 추가합니다. `\t`는 실제 탭 문자를 뜻합니다. 입력을 `samples/equivalent/basic.tsv`, 열을 `document`로 바꾸면 같은 내용으로 연습할 수 있습니다. 제공된 TSV는 셀 안에 탭·줄바꿈이 없으므로 한 줄씩 읽어 `split("\t", -1)`로 처리해도 됩니다. 두 번째 인수 `-1`은 줄 끝의 빈 셀도 남기기 위한 설정입니다. CSV의 줄바꿈 셀에는 이 방식을 쓸 수 없습니다.

[Commons CSV 공식 문서](https://commons.apache.org/proper/commons-csv/apidocs/index.html)는 다른 버전 기준으로 표시될 수 있습니다. 이 프로젝트는 1.14.1을 사용합니다.

## 5. HTML 읽기 연습

HTML은 웹 문서의 구조를 표현하는 형식입니다.

```html
<article id="content">
  <p>Java를 공부합니다.</p>
</article>
```

`<p>`와 `</p>`는 문단의 시작과 끝을 표시하는 **태그**, 태그와 내용을 합쳐 **요소**, `id="content"`는 요소에 이름을 붙이는 **속성**입니다. 집계할 글은 `Java를 공부합니다.`이며 태그 이름과 속성 값은 세지 않습니다. **CSS 선택자**는 원하는 요소를 찾는 표현이고, 이 과제의 `#content`는 `id`가 `content`인 요소를 뜻합니다. `script`·`style`처럼 태그 이름으로 찾을 수도 있습니다. CSS로 화면을 꾸미는 기능까지 배울 필요는 없습니다.

jsoup은 HTML을 Java 객체로 읽고 선택자로 요소를 찾아 텍스트를 꺼내는 라이브러리입니다. 저장된 파일을 읽으므로 인터넷 연결이나 크롤링은 필요 없습니다. `Main`과 같은 패키지에 `HtmlPractice` 클래스를 만들어 실행해보세요.

```java
package kr.sesac.wordcounter;

import java.io.IOException;
import java.nio.file.Path;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlPractice {
    public static void main(String[] args) throws IOException {
        Document document = Jsoup.parse(
                Path.of("samples/equivalent/basic.html").toFile(), "UTF-8");
        Elements matches = document.select("#content");
        if (matches.size() != 1) {
            throw new IOException("본문 요소는 정확히 하나여야 합니다.");
        }
        Element content = matches.first();
        content.select("script, style, nav, header, footer").remove();
        System.out.println(content.text());
    }
}
```

- `Document`는 문서 전체, `Element`는 요소 하나, `Elements`는 찾은 요소들의 모음입니다. `select()`는 선택자에 맞는 요소를 찾고, 쉼표로 나열한 선택자는 그중 하나에 해당하는 요소들을 모두 찾습니다.
- `remove()`는 그 요소와 내용을 문서 객체에서 지웁니다. **원본 HTML 파일은 바뀌지 않습니다.**
- `text()`는 태그를 뺀 텍스트를 꺼내고, `&nbsp;`, `&#74;` 같은 문자 참조도 해석합니다(`&#74;AVA`는 `JAVA`). 문단 사이가 공백으로 이어진 한 줄로 나올 수 있지만 이 샘플에서는 기본 TXT와 같은 단어를 얻습니다.

jsoup은 브라우저와 달리 JavaScript를 실행하지 않습니다. 과제 대상은 본문이 파일 안에 들어 있는 정적 HTML입니다. [jsoup 파일 읽기](https://jsoup.org/cookbook/input/load-document-from-file) · [텍스트 추출](https://jsoup.org/cookbook/extracting-data/attributes-text-html)

## 6. 막혔을 때 보는 힌트

<details>
<summary>단어별 횟수를 저장하려면?</summary>

`Map<String, Long>`처럼 단어를 키, 횟수를 값으로 사용할 수 있습니다. 처음 나온 단어는 1로 저장하고, 이미 있다면 기존 값에 1을 더합니다. `getOrDefault(단어, 0L)`로 현재 값을 꺼내 1을 더한 뒤 `put`하면 됩니다.

</details>

<details>
<summary>토큰 규칙을 코드로 표현하려면?</summary>

정규식은 찾고 싶은 문자열의 형태를 기호로 적는 방법입니다. `Pattern`과 `Matcher`는 Java에서 그 형태를 정의하고 문자열에서 찾는 도구입니다. `[...]`는 허용 문자 범위, `+`는 그 범위에 속한 문자가 한 글자 이상 연속됨을 뜻합니다. 예를 들어 영문 소문자만 허용하는 범위는 다음처럼 적습니다.

```java
Pattern tokenPattern = Pattern.compile("[a-z]+");
```

[단어 처리 규칙](requirements.md#3-단어-처리-규칙)에 맞게 대문자, 숫자, 한글 범위를 `[...]` 안에 직접 추가해보세요. 완성형 한글은 `가-힣`, 자음은 `ㄱ-ㅎ`, 모음은 `ㅏ-ㅣ` 범위를 사용합니다.

패턴을 문자열에 적용하려면 `Matcher`를 만들고, `find()`가 `true`를 돌려주는 동안 `group()`으로 일치한 부분을 꺼냅니다.

```java
Matcher matcher = tokenPattern.matcher(line);
while (matcher.find()) {
    String token = matcher.group();
    // 여기서 소문자로 바꾸고, 숫자만 있는 토큰은 건너뛰고, 횟수를 늘립니다.
}
```

`문자열.toLowerCase(Locale.ROOT)`로 영문을 소문자로 바꿉니다. `Locale.ROOT`는 컴퓨터의 언어·지역 설정에 따라 변환 결과가 달라지는 일을 피하기 위한 기준입니다. `Pattern`·`Matcher`는 `java.util.regex`에, `Locale`은 `java.util`에 있습니다. 정규식 대신 문자를 하나씩 검사해도 규칙을 만족하면 됩니다.

구분자를 기준으로 `split`하는 방식을 택했다면, 문자열이 구분자로 시작할 때 맨 앞에 생기는 빈 문자열을 반드시 걸러내세요. `samples/edge/leading-symbols.txt`가 이 경우를 확인하는 샘플입니다.

</details>

<details>
<summary>횟수순으로 정렬하려면?</summary>

`Map`에는 순서가 없으므로 정렬은 목록으로 옮겨서 합니다. `new ArrayList<>(counts.entrySet())`로 (단어, 횟수) 쌍의 목록을 만들고, `list.sort(...)`에 비교 기준을 넘깁니다. 기준은 횟수가 큰 것이 먼저, 횟수가 같으면 `getKey().compareTo(...)`가 작은 것이 먼저입니다. 각 쌍은 `Map.Entry`이고 `getKey()`가 단어, `getValue()`가 횟수입니다. 저장할 때도 이 정렬된 목록을 그대로 쓰면 됩니다.

</details>

<details>
<summary>폴더 안 파일 목록을 얻으려면?</summary>

`Files.isDirectory(path)`로 폴더인지 확인하고, `Files.list(path)`로 바로 아래 항목들을 얻습니다. 그중 `Files.isRegularFile`인 것만 골라 확장자를 검사하세요. `Files.list`는 순서를 보장하지 않으니 정렬(`sorted()`)한 뒤 처리하면 실행마다 같은 순서가 되고 오류 안내 순서도 일정해집니다. `Files.list`가 돌려주는 스트림은 `try (...)`로 닫습니다. 합산 결과는 순서와 관계없이 같아야 합니다.

</details>

<details>
<summary>콘솔 입력과 시간 측정에서 자주 막히는 것은?</summary>

`Scanner.nextInt()`는 Enter만 누른 것을 구분하지 못합니다. `nextLine()`으로 한 줄을 문자열로 읽어, 비어 있으면 기본값을 쓰고 아니면 `Integer.parseInt`로 바꾸세요. 숫자가 아니면 `NumberFormatException`이 나므로 잡아서 다시 입력받습니다.

`System.nanoTime()`의 차이는 나노초입니다. 1,000,000으로 나누면 밀리초(ms)가 됩니다.

</details>

<details>
<summary>깨진 CSV에서 프로그램이 멈추지 않게 하려면?</summary>

Commons CSV는 `for (CSVRecord record : parser)` 반복 중 만난 형식 오류를 `IOException`이 아니라 `UncheckedIOException`으로 던집니다. `catch (IOException e)`만 있으면 잡히지 않습니다. 파일 하나를 처리하는 부분에서 `UncheckedIOException`도 함께 잡아 그 파일을 실패로 기록하고 다음 파일로 넘어가세요. 예외의 `getCause()`에 원인이 들어 있어 "닫히지 않은 따옴표" 같은 안내 문구로 바꿀 수 있습니다.

</details>

<details>
<summary>실패한 파일의 단어가 결과에 섞이지 않게 하려면?</summary>

하나의 `Map`에 바로 더하면 오류가 나기 전까지 읽은 단어가 남습니다. 파일마다 새 `Map`에 집계하고, 예외 없이 끝난 파일의 결과만 전체 합계에 더하세요. 실패한 파일은 경로와 이유만 기록합니다.

</details>

## 7. 용어

| 용어 | 이 과제에서의 뜻 |
|---|---|
| 프로젝트 루트 | `pom.xml`, `src`, `samples`가 있는 가장 바깥 폴더 |
| 작업 디렉터리 | 프로그램이 상대 경로의 기준으로 쓰는 폴더. 이 과제에서는 프로젝트 루트 |
| 상대 경로·절대 경로 | 기준 폴더에서 찾아가는 경로(`samples/equivalent/basic.txt`)·파일의 전체 위치(`C:\practice\basic.txt`, `/Users/student/practice/basic.txt`) |
| 인코딩·UTF-8 | 문자를 파일의 바이트로 저장하고 읽는 규칙. 이 과제는 UTF-8로 읽고 저장 |
| BOM | 일부 텍스트 파일 맨 앞에 붙는 인코딩 표시. 제공된 입력에는 없음 |
| LF·CRLF | 줄바꿈을 저장하는 두 방식. 읽은 내용과 단어 횟수는 같아야 함 |
| 컴파일·빌드 | `.java`를 `.class`로 변환하는 일·컴파일 등을 거쳐 실행 결과물을 만드는 과정. `target` 폴더가 그 출력 위치 |
| 라이브러리·의존성 | 가져다 쓰는 기능 묶음·`pom.xml`에 적은, 프로젝트가 필요로 하는 라이브러리 |
| 파싱·파서 | 파일 구조를 해석해 필요한 내용을 꺼내는 일·그 일을 하는 코드 |
| 헤더·레코드·셀·구분자 | 열 이름을 적은 첫 레코드·데이터 한 건·한 레코드의 한 열에 있는 값·셀을 나누는 문자 |
| 태그·요소·속성·선택자 | `<p>`처럼 구조를 표시하는 기호·태그와 내용을 합친 HTML의 한 부분·요소에 붙는 추가 정보(`id="content"`)·요소를 찾는 표현(`#content`) |
| 토큰 | 단어 처리 규칙으로 잘라낸, 카운팅할 문자열 한 조각 |
| `.gitignore` | Git에 올릴 대상에서 제외할 파일·폴더를 적은 설정. 로컬 파일을 지우지는 않음 |

## 8. 터미널로 실행하려면 (선택)

이 과제는 IntelliJ Run으로 진행합니다. 터미널이 궁금할 때만 읽으세요.

<details>
<summary>Maven Wrapper 명령</summary>

`mvnw`(macOS·Linux)와 `mvnw.cmd`(Windows)는 이 프로젝트에 지정된 Maven을 다운로드해 실행하는 스크립트입니다. JDK 21은 준비되어 있어야 합니다. `pom.xml`이 있는 폴더에서 다음을 실행하면 컴파일 후 `Main`이 실행됩니다.

```sh
./mvnw -q compile exec:java        # macOS·Linux
.\mvnw.cmd -q compile exec:java    # Windows PowerShell
```

`-q`는 진행 메시지를 줄이는 옵션입니다. 터미널에서만 JDK 오류가 나면 `java -version`과 `JAVA_HOME` 환경 변수를 확인하고, macOS·Linux에서 실행 권한 오류가 나면 `chmod +x mvnw`를 실행하세요.

</details>

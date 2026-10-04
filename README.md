# 문서 단어 분석기

TXT·CSV·TSV·HTML 파일에서 텍스트를 읽어 **단어별 출현 횟수를 세는 Java 콘솔 프로그램**을 만드는 개인 프로젝트입니다.

- 기간: **2026년 9월 21일(월) 수업 후 ~ 10월 1일(목)**. 10월 1일에 발표합니다.
- 환경: **JDK 21, IntelliJ IDEA, UTF-8**
- 순서: [필수 요구사항](docs/requirements.md)을 먼저 완성하고, [심화 요구사항](docs/advanced.md)은 관심에 따라 선택합니다.

Maven, jsoup, Apache Commons CSV를 몰라도 괜찮습니다. 필요한 설정은 `pom.xml`에 들어 있고, 첫 단계인 TXT 카운팅은 Java 기본 기능만으로 구현합니다.

## 시작하기

1. 교육용 GitHub 계정으로 로그인한 뒤 **Code → Download ZIP**을 선택합니다.
2. ZIP을 풉니다. 폴더 이름이 `sesac-ddm-4-web-java-word-counter-…`처럼 길면 `java-word-counter`로 바꿔도 됩니다.
3. IntelliJ에서 **Open**으로 압축을 푼 폴더 안의 `pom.xml`을 열고 **Open as Project**를 선택합니다.
4. Project SDK를 **JDK 21**로 맞추고 Maven이 라이브러리를 다운로드할 때까지 기다립니다. 처음 한 번은 인터넷 연결이 필요하며, Maven을 따로 설치할 필요는 없습니다.
5. [Main.java](src/main/java/kr/sesac/wordcounter/Main.java)를 열어 `main` 옆 **Run**을 누릅니다.

정상 실행되면 [작은 TXT 샘플](samples/equivalent/basic.txt)의 세 줄이 출력됩니다.

```text
Java, java! 자바를 공부했다.
자료구조 자료구조 java17 123
ㅋㅋ JAVA
```

시작 코드에는 TXT 읽기까지 준비되어 있습니다. `TODO` 주석 자리에 단어 분리와 카운팅을 직접 구현하세요. **첫 목표는 이 파일에서 전체 단어 9개, 서로 다른 단어 6개를 얻는 것**입니다. [정답 파일](expected/basic-counts.tsv)과 비교합니다.

실행이 안 되거나 다음에 무엇을 할지 모르겠다면 [개발 가이드](docs/guide.md)를 보세요.

## 과제 안내 문서

과제의 상세 기준은 이 저장소의 Markdown 문서에서 확인합니다. 수업에서는 이 README의 시작 방법과 첫 목표를 확인한 뒤, 아래 순서로 안내합니다.

| 순서 | 문서 | 확인할 내용 |
|---|---|---|
| 1 | [필수 요구사항](docs/requirements.md) | 구현할 기능, 실행 결과 예시, 샘플과 정답으로 확인하는 방법 |
| 2 | [심화 요구사항](docs/advanced.md) | 필수 완성 후 선택하는 객체지향 구조 개선, 성능 개선, 병렬 처리, 중단 후 재개, 데이터 수집, DB 저장 등 |
| 3 | [진행 기록과 발표](docs/project-guide.md) | `PROGRESS.md` 작성, 발표용 PPT와 시연 준비, 발표 시간, GitHub 저장소 링크 전달과 리뷰, AI 활용 범위 |

직접 구현을 시작할 때는 **[개발 가이드](docs/guide.md) 2절과 필수 요구사항 3·4번부터 읽으면 됩니다.** 개발 가이드에는 실행 오류 해결, 첫 TODO 구현 순서, 처음 쓰는 도구와 연습 예제, 막혔을 때 힌트와 용어가 있습니다. 나머지는 각 단계에서 필요할 때 읽으세요.

## 폴더 구성

```text
java-word-counter/
├── pom.xml                      라이브러리·빌드 설정. 수정하지 않아도 됨
├── src/main/java/kr/sesac/wordcounter/Main.java
├── docs/                        위 문서 4개
├── samples/                     작은 입력 샘플 14개
├── expected/                    샘플과 실제 데이터의 정답
├── data/                        한국어 뉴스·대화·성능 실험용 입력. 출처는 data/SOURCES.md
├── PROGRESS.md                  진행 기록. 직접 채움
└── out/                         프로그램이 결과를 저장할 폴더. 실행 중 생성
```

`mvnw`, `.github`, `tools`는 몰라도 됩니다. 이 저장소는 비공개이며 `sesac-ddm-4-web` 조직에 접근할 수 있는 계정으로만 다운로드할 수 있습니다.

# 데이터 출처와 변환 기록

확인·준비일: 2026-09-19. 이 문서는 제공 데이터의 출처를 보존하기 위한 안내입니다. 분석에 사용할 파일·폴더는 [필수 요구사항의 큰 데이터 처리](../docs/requirements.md#10-큰-데이터-처리)와 [심화 요구사항의 성능 실습 데이터](../docs/advanced.md#성능-실습-데이터)에 있습니다.

## KLUE YNAT v1.1

- 제작: **KLUE benchmark 제작진** — [KLUE 원본 저장소와 인용 정보](https://github.com/KLUE-benchmark/KLUE).
- 원본: [YNAT v1.1 학습 데이터](https://github.com/KLUE-benchmark/KLUE/blob/3efd98708a40ff49251fddde35453f8fbb11f536/klue_benchmark/ynat-v1.1/ynat-v1.1_train.json), 45,678건. 원본의 `title` 필드를 사용했습니다.
- 고정 버전: `3efd98708a40ff49251fddde35453f8fbb11f536`.
- 라이선스: **Creative Commons Attribution-ShareAlike 4.0 International(CC BY-SA 4.0)**. [원본 라이선스](https://github.com/KLUE-benchmark/KLUE/blob/3efd98708a40ff49251fddde35453f8fbb11f536/License.md)와 [함께 보관한 전문](licenses/KLUE-CC-BY-SA-4.0.txt)을 확인하세요.

이 저장소의 `data/klue-ynat/**`, `data/performance/**` 및 `expected/news-*-counts.tsv`는 위 자료에서 변환한 데이터이며 **CC BY-SA 4.0**으로 제공합니다. 공유할 때 이 출처·라이선스·변환 기록을 함께 보관합니다.

변환 내용:

1. 원본 배열 순서를 유지하고 `title`을 CSV의 `text` 열로 옮겼습니다. `id`는 1부터 시작하는 행 번호이며 분석에서 제외합니다.
2. 첫 1,000건·10,000건 및 전체 45,678건을 각각 저장했습니다. 제목의 표기·공백·문장부호는 변경하지 않았습니다.
3. `klue-ynat/many`는 전체 자료를 16개 연속 구간으로 나누었습니다. 각 레코드는 이 폴더 전체에서 한 번만 나타납니다.
4. `formats`는 첫 1,000개 제목을 TXT·TSV·정적 HTML로 표현했습니다. HTML에는 `#content` 본문과 분석에서 제외할 메뉴·꼬리말을 넣었고, 원문 문자는 HTML에 맞게 이스케이프했습니다.
5. `performance`는 전체 제목 목록을 정확히 8번 반복한 365,424건입니다. `id`를 다시 부여했으며, `many`는 그 목록을 16개 구간으로 나눈 것입니다. 고유한 실제 데이터가 늘어난 것으로 해석하지 마세요.
6. `expected/news-*-counts.tsv`는 과제의 단어 처리 규칙과 정렬 규칙으로 계산한 단어별 정답입니다.

## Chatbot Data

- 제작: **songys/Chatbot_data 제작자** — [원본 저장소와 데이터 설명](https://github.com/songys/Chatbot_data).
- 원본: [ChatbotData.csv](https://github.com/songys/Chatbot_data/blob/4cf20d13fc46f5037fd1c531cd566e2dd9f72974/ChatbotData.csv), 11,823건.
- 고정 버전: `4cf20d13fc46f5037fd1c531cd566e2dd9f72974`.
- 라이선스: **MIT**, 원본 저작권 고지 `Copyright (c) 2018` 포함. [원본 라이선스](https://github.com/songys/Chatbot_data/blob/4cf20d13fc46f5037fd1c531cd566e2dd9f72974/LICENSE)와 [함께 보관한 전문](licenses/Chatbot-MIT.txt)을 확인하세요.

`data/chatbot/chatbot.csv`는 원본의 `Q`, `A`, `label` 값과 순서를 유지하면서 UTF-8(BOM 없음)·LF 줄바꿈의 CSV로 다시 저장했습니다. 분석 대상은 `Q`와 `A`이고, `label`은 제외합니다. `expected/chatbot-counts.tsv`는 두 열에 과제 단어 규칙을 적용한 정답입니다. 이 데이터와 파생 정답은 원본의 MIT 조건으로 제공하며 저작권·허가 고지를 함께 보관합니다.

## 재현 정보

[manifest.json](manifest.json)에 원본 URL·SHA-256, 변환 설명, 생성 파일의 바이트 수·레코드 수·SHA-256을 기록했습니다. SHA-256은 파일 내용의 변경 여부를 비교하는 값입니다. MB 표기는 1,000,000바이트 기준입니다.

[준비 스크립트](../tools/prepare_practice_data.py)는 고정된 원본 네 파일을 읽어 입력 데이터를 다시 만듭니다. 교육생이 실행할 필요는 없습니다. [강사용 재생성 안내](../tools/instructor-data.md)를 참고하세요. 외부의 원본 JSON·원본 CSV 중간 파일은 저장소에 중복 보관하지 않았습니다.

-- 컨테이너를 처음 만들 때 한 번 실행됩니다 (데이터베이스 wordcount).
-- utf8mb4_bin: 대소문자나 모양이 비슷한 문자를 서로 다른 것으로 구분하는 정렬 규칙. 기본 규칙을 쓰면 서로 다른 단어가 한 행으로 합쳐집니다.

CREATE TABLE IF NOT EXISTS word_counts (
    word  VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
    count BIGINT NOT NULL
) ENGINE = InnoDB;

-- 6단계(집계를 DB에 맡기기)에서 집계 전의 토큰을 그대로 쌓아 두는 임시 표
CREATE TABLE IF NOT EXISTS tokens (
    token VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL
) ENGINE = InnoDB;

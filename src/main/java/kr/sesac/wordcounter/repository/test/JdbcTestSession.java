package kr.sesac.wordcounter.repository.test;

import kr.sesac.wordcounter.repository.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcTestSession implements AutoCloseable {
    private final Connection connection;
    private final DbAggregationMode mode;

    // 1단계용
    private final PreparedStatement selectStatement;
    private final PreparedStatement updateStatement;
    private final PreparedStatement insertStatement;

    // 2~4단계용
    private final PreparedStatement upsertStatement;

    private boolean completed;

    JdbcTestSession(Connection connection, DbAggregationMode mode) throws RepositoryException {
        this.connection = connection;
        this.mode = mode;

        try {
            boolean autoCommit = switch (mode) {
                case STAGE_1, STAGE_2 -> true;
                case STAGE_3, STAGE_4 -> false;
            };

            connection.setAutoCommit(autoCommit);

            selectStatement = connection.prepareStatement("""
                    SELECT count
                    FROM word_counts
                    WHERE word = ?
                    """);

            updateStatement = connection.prepareStatement("""
                    UPDATE word_counts
                    SET count = count + 1
                    WHERE word = ?
                    """);

            insertStatement = connection.prepareStatement("""
                    INSERT INTO word_counts (word, count)
                    VALUES (?, 1)
                    """);


            upsertStatement = connection.prepareStatement("""
                    INSERT INTO word_counts (word, count)
                    VALUES (?, 1)
                    ON DUPLICATE KEY UPDATE count = count + 1
                    """);
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                e.addSuppressed(closeException);
            }

            throw new RepositoryException("JDBC 세션을 생성하지 못했습니다.", e);
        }
    }

    @Override
    public void close() throws RepositoryException {
        try {
            selectStatement.close();
            updateStatement.close();
            insertStatement.close();
            upsertStatement.close();

            // 3·4단계에서 complete() 없이 종료됐다면 부분 집계를 취소
            if (!connection.getAutoCommit() && !completed) {
                connection.rollback();
            }
            connection.close();
        } catch (SQLException e) {
            throw new RepositoryException("JDBC 세션을 종료하는 중 리소스 정리 또는 롤백에 실패했습니다.", e);
        }
    }

    public void add(String token) throws RepositoryException {
        switch (mode) {
            case STAGE_1 -> addUpdateInsert(token);
            case STAGE_2, STAGE_3 -> addUpsert(token);
            case STAGE_4 -> addUpsertBatch(token);
        }
    }

    private void addUpdateInsert(String token) throws RepositoryException {
        try {
            selectStatement.setString(1, token);
            try (ResultSet rs = selectStatement.executeQuery()) {
                if (rs.next()) {
                    updateStatement.setString(1, token);
                    updateStatement.executeUpdate();
                } else {
                    insertStatement.setString(1, token);
                    insertStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("토큰을 조회하거나 업데이트하지 못했습니다.", e);
        }
    }

    private void addUpsert(String token) throws RepositoryException {
        try {
            upsertStatement.setString(1, token);
            upsertStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("토큰을 조회하거나 업데이트하지 못했습니다.", e);
        }
    }

    private static final int PENDING_COUNT = 5000;
    private int pending = 0;

    private void addUpsertBatch(String token) throws RepositoryException {
        try {
            upsertStatement.setString(1, token);
            upsertStatement.addBatch();
            if (++pending == PENDING_COUNT) {
                upsertStatement.executeBatch();
                pending = 0;
            }
        } catch (SQLException e) {
            throw new RepositoryException("토큰을 배치에 추가하거나 배치를 실행하지 못했습니다.", e);
        }
    }

    public void complete() throws RepositoryException {
        if (completed) {
            return;
        }

        try {
            switch (mode) {
                case STAGE_3 -> connection.commit();
                case STAGE_4 -> {
                    upsertStatement.executeBatch();
                    connection.commit();
                }
            }

            completed = true;
        } catch (SQLException e) {
            throw new RepositoryException("배치 실행 또는 커밋을 완료하지 못했습니다.", e);
        }
    }
}

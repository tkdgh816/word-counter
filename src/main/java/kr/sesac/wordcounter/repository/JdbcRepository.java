package kr.sesac.wordcounter.repository;

import kr.sesac.wordcounter.model.TokenCount;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JdbcRepository implements TokenCountRepository {
    private static final String URL =
            "jdbc:mysql://localhost:3307/wordcount?rewriteBatchedStatements=true&allowPublicKeyRetrieval=true&useSSL=false";
    private static final String USER = "wordcount";
    private static final String PASSWORD = "wordcount";

    public JdbcRepository() {
    }

    @Override
    public void clear() {
        truncateTables();
    }

    public void checkConnection() {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM word_counts")) {
            rs.next();
            System.out.println("연결 성공. 저장된 단어 " + rs.getLong(1) + "개");
        } catch (SQLException e) {
            throw new RepositoryException("데이터베이스 연결 상태를 확인하지 못했습니다.", e);
        }
    }

    /**
     * 새 분석을 위해 단어 집계 테이블을 테이블을 초기화합니다.
     *
     * @throws RepositoryException 테이블을 초기화하지 못함
     */
    private void truncateTables() {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("TRUNCATE TABLE word_counts");
            statement.executeUpdate("TRUNCATE TABLE tokens");
        } catch (SQLException e) {
            throw new RepositoryException("테이블을 초기화하지 못했습니다.", e);
        }
    }

    @Override
    public long getUniqueCount() {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM word_counts")) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RepositoryException("서로 다른 단어의 개수를 조회하지 못했습니다.", e);
        }
    }

    @Override
    public void addAll(Collection<TokenCount> tokenCounts) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO word_counts (word, count)
                     VALUES (?, ?)
                     ON DUPLICATE KEY UPDATE count = count + VALUES(count)
                     """)) {
            connection.setAutoCommit(false);
            for (TokenCount tokenCount : tokenCounts) {
                statement.setString(1, tokenCount.token());
                statement.setLong(2, tokenCount.count());
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            throw new RepositoryException("단어 집계 결과를 저장하지 못했습니다.", e);
        }
    }

    @Override
    public List<TokenCount> findTop(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit은 0 이상이어야 합니다.");
        }

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT word, count
                     FROM word_counts
                     ORDER BY count DESC, word ASC
                     LIMIT ?
                     """)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                List<TokenCount> tokenCounts = new ArrayList<>();
                while (rs.next()) {
                    tokenCounts.add(new TokenCount(rs.getString(1), rs.getLong(2)));
                }
                return tokenCounts;
            }
        } catch (SQLException e) {
            throw new RepositoryException(String.format("상위 %d개의 단어 집계 결과를 조회하지 못했습니다.", limit), e);
        }
    }

    @Override
    public TokenCount findByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰이 유효하지 않습니다.");
        }

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT count
                     FROM word_counts
                     WHERE word = ?"""
             )) {
            statement.setString(1, token);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new TokenCount(token, rs.getLong(1));
                }
                return new TokenCount(token, 0L);
            }
        } catch (SQLException e) {
            throw new RepositoryException(String.format("토큰 '%s'의 집계 결과를 조회하지 못했습니다.", token), e);
        }
    }

    @Override
    public List<TokenCount> findAllSorted() {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("""
                     SELECT word, count
                     FROM word_counts
                     ORDER BY count DESC, word ASC
                     """)) {
            List<TokenCount> tokenCounts = new ArrayList<>();
            while (rs.next()) {
                tokenCounts.add(new TokenCount(rs.getString(1), rs.getLong(2)));
            }
            return tokenCounts;
        } catch (SQLException e) {
            throw new RepositoryException("정렬된 전체 단어 집계 결과를 조회하지 못했습니다.", e);
        }
    }
}

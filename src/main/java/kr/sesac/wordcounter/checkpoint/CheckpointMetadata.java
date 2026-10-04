package kr.sesac.wordcounter.checkpoint;

import kr.sesac.wordcounter.text.reader.TextReaderKind;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record CheckpointMetadata(String originalFilePath, String countsFilePath, String originalFileSha256,
                                 String readerVersion) {
    public static CheckpointMetadata create(Path originalPath) throws IOException {
        originalPath = originalPath.toAbsolutePath().normalize();;
        Path outputPath = Path.of("out", "checkpoint", sha256OfString(originalPath.toString()) + ".tsv");
        return new CheckpointMetadata(originalPath.toString(), outputPath.toString(), sha256OfFile(originalPath), TextReaderKind.VERSION);
    }

    private static String sha256OfString(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.");
        }
    }

    private static String sha256OfFile(Path path) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];

            try (InputStream stream = Files.newInputStream(path)) {
                int n;
                while ((n = stream.read(buffer)) != -1) {
                    md.update(buffer, 0, n);
                }
            }

            byte[] hash = md.digest();
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.");
        }
    }

    /**
     * 현재 파일과 분석 설정에 대해 이 체크포인트를 재사용할 수 있는지 확인합니다.
     * countsFilePath는 비교하지 않습니다.
     */
    public boolean isReusableFor(CheckpointMetadata other) {
        if (other == null) {
            return false;
        }

        return this.originalFilePath.equals(other.originalFilePath)
                && this.originalFileSha256.equals(other.originalFileSha256)
                && this.readerVersion.equals(other.readerVersion);
    }
}

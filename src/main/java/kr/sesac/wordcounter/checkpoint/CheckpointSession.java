package kr.sesac.wordcounter.checkpoint;

import kr.sesac.wordcounter.analysis.FileTokenCountResult;
import kr.sesac.wordcounter.model.TokenCount;
import kr.sesac.wordcounter.text.reader.TextReaderException;
import kr.sesac.wordcounter.text.reader.TsvReader;
import kr.sesac.wordcounter.text.writer.TsvWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CheckpointSession implements AutoCloseable {
    private static final String[] METADATA_HEADER_NAMES = {"originalFilePath", "countsFilePath", "originalFileSha256", "readerVersion"};
    private static final String[] CHECKPOINT_HEADER_NAMES = {"word", "count"};

    private static final Path metadataPath = Path.of("out", "checkpoint", "checkpoint.tsv");

    private final Map<String, CheckpointMetadata> metadataByOriginalPath;

    private final Object lock = new Object();

    private CheckpointSession(Map<String, CheckpointMetadata> metadataByOriginalPath) {
        this.metadataByOriginalPath = metadataByOriginalPath;
    }

    public static CheckpointSession open() {
        Map<String, CheckpointMetadata> metadataByOriginalPath = new HashMap<>();
        TsvReader metadataReader = new TsvReader(List.of(METADATA_HEADER_NAMES));
        try {
            metadataReader.readRecords(metadataPath, record -> {
                CheckpointMetadata metadata = new CheckpointMetadata(
                        record.get(METADATA_HEADER_NAMES[0]),
                        record.get(METADATA_HEADER_NAMES[1]),
                        record.get(METADATA_HEADER_NAMES[2]),
                        record.get(METADATA_HEADER_NAMES[3]));
                metadataByOriginalPath.put(metadata.originalFilePath(), metadata);
            });
            return new CheckpointSession(metadataByOriginalPath);
        } catch (TextReaderException | IllegalArgumentException e) {
            return new CheckpointSession(new HashMap<>());
        }
    }

    public Optional<FileTokenCountResult> restore(Path filePath) {
        try {
            filePath = filePath.toAbsolutePath().normalize();
            CheckpointMetadata currentMetadata = CheckpointMetadata.create(filePath);

            synchronized (lock) {
                CheckpointMetadata savedMetadata = metadataByOriginalPath.get(filePath.toString());
                if (savedMetadata != null && savedMetadata.isReusableFor(currentMetadata)) {
                    return getCheckPoint(savedMetadata);
                }
            }
        } catch (IOException e) {
        }
        return Optional.empty();
    }

    public void save(FileTokenCountResult fileTokenCountResult) {
        try {
            CheckpointMetadata metadata = CheckpointMetadata.create(fileTokenCountResult.getFilePath());
            String originalFilePathString = metadata.originalFilePath();
            Path countsFilePath = Path.of(metadata.countsFilePath());

            TsvWriter.save(countsFilePath, TokenCount.class, fileTokenCountResult.getTokenCounts(), CHECKPOINT_HEADER_NAMES);
            synchronized (lock) {
                CheckpointMetadata previousMetadata = metadataByOriginalPath.put(originalFilePathString, metadata);
                try {
                    saveMetadata();
                } catch (IOException e) {
                    // 메타데이터 저장 실패한 경우
                    if (previousMetadata == null) {
                        metadataByOriginalPath.remove(originalFilePathString);
                    } else {
                        metadataByOriginalPath.put(originalFilePathString, previousMetadata);
                    }
                    throw new CheckpointException("체크포인트를 만들 수 없습니다.", e);
                }
            }


        } catch (IOException e) {
            throw new CheckpointException("체크포인트를 만들 수 없습니다.", e);
        }

    }

    private Optional<FileTokenCountResult> getCheckPoint(CheckpointMetadata metadata) {
        try {
            Path countsFile = Path.of(metadata.countsFilePath());
            if (!Files.exists(countsFile)) {
                return Optional.empty();
            }

            CheckpointFileTokenCounter tokenCounter = new CheckpointFileTokenCounter(Path.of(metadata.originalFilePath()));
            TsvReader checkpointReader = new TsvReader(List.of(CHECKPOINT_HEADER_NAMES));
            checkpointReader.readRecords(countsFile, record -> {
                String token = record.get(CHECKPOINT_HEADER_NAMES[0]);
                long count = Long.parseLong(record.get(CHECKPOINT_HEADER_NAMES[1]));

                tokenCounter.add(token, count);
            });

            return Optional.of(tokenCounter);
        } catch (InvalidPathException | NumberFormatException | TextReaderException e) {
            return Optional.empty();
        }
    }

    private void saveMetadata() throws IOException {
        TsvWriter.save(metadataPath, CheckpointMetadata.class, metadataByOriginalPath.values().stream().toList(), METADATA_HEADER_NAMES);
    }

    @Override
    public void close() {
        try {
            synchronized (lock) {
                saveMetadata();
            }
        } catch (IOException e) {
            throw new CheckpointException("메타데이터 파일을 저장하지 못했습니다.", e);
        }
    }
}

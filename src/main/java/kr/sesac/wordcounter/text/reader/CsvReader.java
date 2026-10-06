package kr.sesac.wordcounter.text.reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.DuplicateHeaderMode;

import java.util.Collection;

public class CsvReader extends DelimitedTableReader {
    private static final CSVFormat FORMAT = CSVFormat.RFC4180.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
            .get();


    public CsvReader(Collection<String> targetHeaderNames) {
        super(targetHeaderNames);
    }

    @Override
    CSVFormat getFormat() {
        return FORMAT;
    }
}

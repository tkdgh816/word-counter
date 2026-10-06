package kr.sesac.wordcounter.text.reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.DuplicateHeaderMode;

import java.util.Collection;

public class TsvReader extends DelimitedTableReader {
    private static final CSVFormat FORMAT = CSVFormat.TDF.builder()
            .setQuote(null) // 따옴표를 일반 문자로 취급
            .setHeader()
            .setSkipHeaderRecord(true)
            .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
            .get();


    public TsvReader(Collection<String> targetHeaderNames) {
        super(targetHeaderNames);
    }

    @Override
    CSVFormat getFormat() {
        return FORMAT;
    }
}

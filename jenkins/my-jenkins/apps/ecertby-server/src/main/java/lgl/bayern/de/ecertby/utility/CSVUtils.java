package lgl.bayern.de.ecertby.utility;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import lgl.bayern.de.ecertby.dto.CatalogValueDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

public class CSVUtils {
    private CSVUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static CsvToBean<CatalogValueDTO> getCatalogValueCtB(MultipartFile file) throws IOException {
        return getCatalogValueCtB(file, ';', '"', 1);
    }

    public static CsvToBean<CatalogValueDTO> getCatalogValueCtB(MultipartFile file, char parserSeparator, char parserQuote, int parserSkipLines) throws IOException {
        Reader reader = new InputStreamReader(file.getInputStream());
        CsvToBean<CatalogValueDTO> ctb = new CsvToBean<>();
        ColumnPositionMappingStrategy<CatalogValueDTO> mappingStrategy = new ColumnPositionMappingStrategy<>();
        mappingStrategy.setType(CatalogValueDTO.class);
        ctb.setMappingStrategy(mappingStrategy);
        ctb.setCsvReader(new CSVReaderBuilder(reader).withCSVParser(new CSVParserBuilder().withSeparator(parserSeparator).withQuoteChar(parserQuote).build()).withSkipLines(parserSkipLines).build());
        return ctb;
    }

    public static void writeLine(StringBuilder stringBuilder, List<String> row) {
        boolean first = true;
        // Surround with quotation marks so that commas are escaped
        for (final String value : row) {
            // Add the separator before each value, ignoring the first one
            if (!first) stringBuilder.append(';');
            stringBuilder.append('"').append(sanitizeFormat(value)).append('"');
            first = false;
        }
        stringBuilder.append("\n");
    }

    private static String sanitizeFormat(String value) {
        String result = value;
        // Escape quotation inside value by doubling it, otherwise it's treated as a grouper
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;
    }
}

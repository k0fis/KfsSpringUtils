package kfs.csv;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author pavedrim
 */
public class TimestampConvertor implements CsvStrConvertor {

    private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");

    @Override
    public String export(Object obj) {
        if (obj == null) {
            return "";
        }
        if (obj instanceof Date) {
            return sdf.format((Date) obj);
        }
        throw new CsvException("Cannot convert " + obj.getClass().getSimpleName());
    }

}

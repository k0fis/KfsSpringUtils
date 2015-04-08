package kfs.csv;


/**
 *
 * @author pavedrim
 */
public class CsvException extends RuntimeException {

    public CsvException(String msg, Throwable ex) {
        super(msg, ex);
    }

    public CsvException(String msg) {
        super(msg);
    }
}

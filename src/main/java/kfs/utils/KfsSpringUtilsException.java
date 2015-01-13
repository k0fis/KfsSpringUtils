package kfs.utils;

/**
 *
 * @author pavedrim
 */
public class KfsSpringUtilsException extends RuntimeException {

    public KfsSpringUtilsException(String msg, Throwable ex) {
        super(msg, ex);
    }

    public KfsSpringUtilsException(String msg) {
        super(msg);
    }
}

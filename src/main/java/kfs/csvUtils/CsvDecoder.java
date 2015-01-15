package kfs.csvUtils;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import kfs.field.KfsField;
import kfs.utils.KfsSpringUtilsException;

/**
 *
 * @author pavedrim
 */
public class CsvDecoder {

    private final Map<Integer, KfsField> fieldMap;
    private final Map<KfsField, DateFormat> tsDecoder;
    private final Map<KfsField, Integer> fieldLength;

    public CsvDecoder(Class cls) {
        fieldMap = new HashMap<Integer, KfsField>();
        tsDecoder = new HashMap<KfsField, DateFormat>();
        fieldLength = new HashMap<KfsField, Integer>();
        Field fields[] = cls.getDeclaredFields();
        for (Field field : fields) {
            CsvPos pos = field.getAnnotation(CsvPos.class);
            if (pos != null) {
                KfsField kf = new KfsField(cls, field);
                fieldMap.put(pos.value(), kf);
                CsvTsFormat fmt = field.getAnnotation(CsvTsFormat.class);
                if (fmt != null) {
                    tsDecoder.put(kf, new SimpleDateFormat(fmt.value()));
                }
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    fieldLength.put(kf, column.length());
                }
            }
        }
    }

    public <T> T readEdw(String[] line, Class<T> cls, T ret) {
        for (int inx = 0; inx < line.length; inx++) {
            if (line[inx].isEmpty()) {
                continue;
            }
            KfsField m = fieldMap.get(inx);
            if (m != null) {
                Class pcls = m.getFieldType();
                if (String.class.equals(pcls)) {
                    Integer cl = fieldLength.get(m);
                    if (cl != null) {
                        if (line[inx].length() > cl) {
                            throw new KfsSpringUtilsException("Column oversize, cannot set "
                                    +m.getName()+", inx: "+inx+" with value: " + line[inx]);
                        }
                    }
                    m.setVal(ret, line[inx]);
                } else if (Double.class.equals(pcls)) {
                    m.setVal(ret, Double.parseDouble(line[inx]));
                } else if (Integer.class.equals(pcls)) {
                    m.setVal(ret, Integer.parseInt(line[inx]));
                } else if (Long.class.equals(pcls)) {
                    m.setVal(ret, Long.parseLong(line[inx]));
                } else if (Timestamp.class.equals(pcls)) {
                    DateFormat f = tsDecoder.get(m);
                    if (f == null) {
                        throw new KfsSpringUtilsException("Cannot decode string for \"" + m.getName()
                                + "\" - value on " + inx + " position: \"" + line[inx] + "\"");
                    }
                    Date d;
                    try {
                        d = f.parse(line[inx]);
                    } catch (ParseException ex) {
                        throw new KfsSpringUtilsException("Cannot decode string for \"" + m.getName()
                                + "\" - value on " + inx + " position: \"" + line[inx] + "\"");
                    }
                    Timestamp ts = new Timestamp(d.getTime());
                    m.setVal(ret, ts);
                }
            }

        }
        return ret;
    }

    public CharSequence toString(String[] line, Object obj) {

        StringBuilder sb = new StringBuilder();
        for (int inx = 0; inx < line.length; inx++) {
            sb.append(String.format("\n%30s", line[inx]));
            KfsField m = fieldMap.get(inx);
            if (m != null) {
                sb.append(" - ").append(String.format("%30s", m.getVal(obj)))//
                        .append(" : ").append(m.getName().substring(3));
            }
        }
        sb.append("\n");
        return sb;
    }
}

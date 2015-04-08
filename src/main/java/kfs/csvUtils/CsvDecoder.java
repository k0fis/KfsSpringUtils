package kfs.csvUtils;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import kfs.field.KfsField;
import kfs.utils.KfsSpringUtilsException;

/**
 *
 * @author pavedrim
 * @param <T>
 */
public class CsvDecoder<T> {

    private final Map<Integer, KfsField> fieldMap;
    private final Map<KfsField, DateFormat> tsDecoder;
    private final Map<KfsField, Integer> fieldLength;
    private List<Integer> sortedInxList = null;

    public CsvDecoder(Class<T> cls) {
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

    public T readObject(String[] line, T ret) {
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
                                    + m.getName() + ", inx: " + inx + " with value: " + line[inx]);
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

    public List<Integer> getSortedInxList() {
        if (sortedInxList == null) {
            sortedInxList = new ArrayList<Integer>(fieldMap.keySet());
            Collections.sort(sortedInxList, new Comparator<Integer>() {

                @Override
                public int compare(Integer o1, Integer o2) {
                    return o1.compareTo(o2);
                }
            });
        }
        return sortedInxList;
    }

    private static StringBuilder addString(StringBuilder sb, String rets, String sep, String quota) {
        if (rets.contains(sep) || rets.contains(quota)) {
            if (rets.contains(quota)) {
                rets = rets.replaceAll(quota, quota + quota);
            }
            return sb.append(quota).append(rets).append(quota);
        } else {
            return sb.append(rets);
        }
    }

    public CharSequence getCsvHeader(String sep, String quota) {
        StringBuilder sb = new StringBuilder();
        boolean f = true;
        for (Integer inx : getSortedInxList()) {
            if (f) {
                f = false;
            } else {
                sb.append(sep);
            }
            addString(sb, fieldMap.get(inx).getName(), sep, quota);
        }
        return sb;
    }

    public CharSequence toCsv(T obj, String sep, String quota) {
        StringBuilder sb = new StringBuilder();
        boolean f = true;
        for (Integer inx : getSortedInxList()) {
            if (f) {
                f = false;
            } else {
                sb.append(sep);
            }
            Object ret = fieldMap.get(inx).getVal(obj);
            if (ret == null) {
                ret = "";
            }
            if (ret.getClass().isAssignableFrom(Date.class)) {
                DateFormat tsd = tsDecoder.get(fieldMap.get(inx));
                if (tsd != null) {
                    ret = tsd.format((Date) ret);
                }
            }
            addString(sb, ret.toString(), sep, quota);
        }
        return sb;
    }

    public CharSequence toString(String[] line, T obj) {
        StringBuilder sb = new StringBuilder();
        int ynx = 0;
        for (Integer inx : getSortedInxList()) {
            sb.append(String.format("\n%30s", line[ynx++]));
            KfsField m = fieldMap.get(inx);
            if (m != null) {
                sb.append(" - ").append(String.format("%30s", m.getVal(obj)))//
                        .append(" : ").append(m.getName());
            }
        }
        sb.append("\n");
        return sb;
    }
}

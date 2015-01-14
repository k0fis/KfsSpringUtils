package kfs.csvUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import kfs.utils.KfsSpringUtilsException;

/**
 *
 * @author pavedrim
 */
public class CsvDecoder {

    private final Map<Class, Map<Integer, Method>> csvMaps;
    private final Map<Method, DateFormat> tsDecoder;

    public CsvDecoder(Class... preps) {
        csvMaps = new HashMap<Class, Map<Integer, Method>>();
        tsDecoder = new HashMap<Method, DateFormat>();
        for (Class c : preps) {
            getMap(c);
        }

    }

    private synchronized Map<Integer, Method> getMap(Class cls) {
        if (csvMaps.containsKey(cls)) {
            return csvMaps.get(cls);
        }
        Map<Integer, Method> map = new HashMap<Integer, Method>();
        Field fields[] = cls.getDeclaredFields();
        for (Field field : fields) {
            CsvPos pos = field.getAnnotation(CsvPos.class);
            if (pos != null) {
                Method m = findSetter(cls, field.getName(), field.getType());
                map.put(pos.value(), m);
                CsvTsFormat fmt = field.getAnnotation(CsvTsFormat.class);
                if (fmt != null) {
                    tsDecoder.put(m, new SimpleDateFormat(fmt.value()));
                }
            }
        }
        csvMaps.put(cls, map);
        return map;
    }

    private void setVal(Method setter, Object ret, Object value) {
        try {
            setter.invoke(ret, value);
        } catch (IllegalAccessException ex) {
            throw new KfsSpringUtilsException("Cannot set " 
                    + ret.getClass().getSimpleName() + "."
                    + setter.getName(), ex);
        } catch (IllegalArgumentException ex) {
            throw new KfsSpringUtilsException("Cannot set " 
                    + ret.getClass().getSimpleName() + "."
                    + setter.getName(), ex);
        } catch (InvocationTargetException ex) {
            throw new KfsSpringUtilsException("Cannot set " 
                    + ret.getClass().getSimpleName() + "."
                    + setter.getName(), ex);
        }
    }

    private Method findGetter(Class<?> theClass, String property, Class paramType) {
        return findSGetter("get", theClass, property);
    }

    private Method findSetter(Class<?> theClass, String property, Class paramType) {
        return findSGetter("set", theClass, property, paramType);
    }

    private Method findSGetter(String prefix, Class<?> theClass, String property, Class... paramType) {
        String setter = prefix + String.format("%C%s", property.charAt(0), property.substring(1));
        try {
            return theClass.getMethod(setter, paramType);
        } catch (NoSuchMethodException ex) {
            throw new KfsSpringUtilsException("Cannot find method " + setter 
                    + " for field " + property, ex);
        }
    }

    public <T> T readEdw(String[] line, Class<T> cls, T ret) {
        Map<Integer, Method> setterMap = getMap(cls);
        for (int inx = 0; inx < line.length; inx++) {
            if (line[inx].isEmpty()) {
                continue;
            }
            Method m = setterMap.get(inx);
            if (m != null) {
                Class pcls = m.getParameterTypes()[0];
                if (String.class.equals(pcls)) {
                    setVal(m, ret, line[inx]);
                } else if (Double.class.equals(pcls)) {
                    setVal(m, ret, Double.parseDouble(line[inx]));
                } else if (Integer.class.equals(pcls)) {
                    setVal(m, ret, Integer.parseInt(line[inx]));
                } else if (Long.class.equals(pcls)) {
                    setVal(m, ret, Long.parseLong(line[inx]));
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
                    setVal(m, ret, ts);
                }
            }

        }
        return ret;
    }

    public CharSequence toString(String[] line, Object obj) {

        Map<Integer, Method> map = new HashMap<Integer, Method>();
        if (obj != null) {
            Field fields[] = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                CsvPos pos = field.getAnnotation(CsvPos.class);
                if (pos != null) {
                    map.put(pos.value(), findGetter(obj.getClass(), field.getName(), field.getType()));
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int inx = 0; inx < line.length; inx++) {
            sb.append(String.format("\n%30s", line[inx]));
            Method m = map.get(inx);
            if (m != null) {
                try {
                    sb.append(" - ").append(String.format("%30s", m.invoke(obj)))//
                            .append(" : ").append(m.getName().substring(3));
                } catch (Exception ex) {
                    throw new KfsSpringUtilsException("Cannot Call Method " + m.getName(), ex);
                }
            }
        }
        sb.append("\n");
        return sb;
    }
}

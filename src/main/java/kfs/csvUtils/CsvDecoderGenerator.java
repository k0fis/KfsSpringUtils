package kfs.csvUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import kfs.utils.KfsSpringUtilsException;

/**
 *
 * @author pavedrim
 */
public class CsvDecoderGenerator {

    private final Class cls;
    private final Map<Integer, Method> csvMaps;
    private final Map<Method, String> tsDecoder;
    private final Map<Method, Integer> sizeMap;

    public CsvDecoderGenerator(Class cls) {
        this.cls = cls;
        tsDecoder = new HashMap<Method, String>();
        csvMaps = new HashMap<Integer, Method>();
        sizeMap = new HashMap<Method, Integer>();
        Field fields[] = cls.getDeclaredFields();
        for (Field field : fields) {
            CsvPos pos = field.getAnnotation(CsvPos.class);
            if (pos != null) {
                Method m = findSetter(cls, field.getName(), field.getType());
                csvMaps.put(pos.value(), m);
                CsvTsFormat fmt = field.getAnnotation(CsvTsFormat.class);
                if (fmt != null) {
                    tsDecoder.put(m, fmt.value());
                }
                if (field.getType().equals(String.class)) {
                    Column col = field.getAnnotation(Column.class);
                    if (col != null) {
                        sizeMap.put(m, col.length());
                    }
                }
            }
        }
    }

    public CharSequence getParserJavaText(String packageName, Class exception) {
        StringBuilder sb = new StringBuilder();

        sb.append("package ").append(packageName).append(";\n\n")
                .append("import java.sql.Timestamp;\n")
                .append("import java.text.ParseException;\n")
                .append("import java.text.SimpleDateFormat;\n")
                .append("import ").append(exception.getName()).append(";\n")
                .append("import ").append(cls.getName()).append(";\n")
                .append("\n/**\n * @author pavedrim\n */\n" + "public class ")
                .append(cls.getSimpleName())
                .append("Decoder {\n\n");

        for (Method m : tsDecoder.keySet()) {
            sb.append("    private static final SimpleDateFormat sdf")
                    .append(m.getName().substring(3))
                    .append(" = new SimpleDateFormat(\"")
                    .append(tsDecoder.get(m)).append("\");\n");
        }
        sb.append("\n");
        sb.append("    public ").append(cls.getSimpleName()).append(" get").append(cls.getSimpleName())
                .append("( String []line) {\n");
        sb.append("        ").append(cls.getSimpleName()).append(" ret = new ").append(cls.getSimpleName()).append("();\n");
        ArrayList<Integer> keys = new ArrayList<Integer>(csvMaps.keySet());
        Collections.sort(keys);
        for (Integer inx : keys) {
            Method m = csvMaps.get(inx);
            Class pcls = m.getParameterTypes()[0];
            if (String.class.equals(pcls)) {
                Integer size = sizeMap.get(m);
                if (size != null) {
                sb.append("        if (line[").append(inx).append("].length() > ").append(size).append(") {\n");
                sb.append("            throw new ").append(exception.getSimpleName()).append("(\"Cannot set ").append(m.getName().substring(3)).append(", oversized: \"+ line[").append(inx).append("].length() +\" , max: ").append(size).append(" \");\n");
                sb.append("        }");
                
                }
                sb.append("        ret.").append(csvMaps.get(inx).getName()).append("( line[").append(inx).append("]);\n");
            } else if (Double.class.equals(pcls)) {
                sb.append("        if (line[").append(inx).append("].length() > 0) {\n");
                sb.append("            try {\n");
                sb.append("                ret.").append(csvMaps.get(inx).getName()).append("(Double.parseDouble( line[").append(inx).append("]));\n");
                sb.append("            } catch (NumberFormatException ex) {\n");
                sb.append("                throw new ").append(exception.getSimpleName()).append("(\"Cannot decode ").append(m.getName().substring(3)).append(" with value: \" + line[").append(inx).append("], ex);\n");
                sb.append("            }\n");
                sb.append("        }\n");
            } else if (Integer.class.equals(pcls)) {
                sb.append("        if (line[").append(inx).append("].length() > 0) {\n");
                sb.append("            try {\n");
                sb.append("                ret.").append(csvMaps.get(inx).getName()).append("(Integer.parseInt( line[").append(inx).append("]));\n");
                sb.append("            } catch (NumberFormatException ex) {\n");
                sb.append("                throw new ").append(exception.getSimpleName()).append("(\"Cannot decode ").append(m.getName().substring(3)).append(" with value: \" + line[").append(inx).append("], ex);\n");
                sb.append("            }\n");
                sb.append("        }\n");
            } else if (Long.class.equals(pcls)) {
                sb.append("        if (line[").append(inx).append("].length() > 0) {\n");
                sb.append("            try {\n");
                sb.append("                ret.").append(csvMaps.get(inx).getName()).append("(Long.parseLong( line[").append(inx).append("]));\n");
                sb.append("            } catch (NumberFormatException ex) {\n");
                sb.append("                throw new ").append(exception.getSimpleName()).append("(\"Cannot decode ").append(m.getName().substring(3)).append(" with value: \" + line[").append(inx).append("], ex);\n");
                sb.append("            }\n");
                sb.append("        }\n");
            } else if (Timestamp.class.equals(pcls)) {
                sb.append("        if (line[").append(inx).append("].length() > 0) {\n");
                sb.append("            try {\n");
                sb.append("                ret.").append(csvMaps.get(inx).getName()).append("( new Timestamp(sdf").append(m.getName().substring(3)).append(".parse( line[").append(inx).append("]).getTime()));\n");
                sb.append("            } catch (ParseException ex) {\n");
                sb.append("                throw new ").append(exception.getSimpleName()).append("(\"Cannot decode ").append(m.getName().substring(3)).append(" with value: \" + line[").append(inx).append("], ex);\n");
                sb.append("            }\n");
                sb.append("        }\n");
            }
        }
        sb.append("        return ret;\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb;
    }

    private Method findSetter(Class<?> theClass, String property, Class paramType) {
        return findSGetter("set", theClass, property, paramType);
    }

    private Method findSGetter(String prefix, Class<?> theClass, String property, Class... paramType) {
        String setter = prefix + String.format("%C%s", property.charAt(0), property.substring(1));
        try {
            return theClass.getMethod(setter, paramType);
        } catch (NoSuchMethodException ex) {
            throw new KfsSpringUtilsException("Cannot find method " + setter + " for field " + property, ex);
        }
    }
}

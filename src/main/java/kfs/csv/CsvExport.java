package kfs.csv;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.Transient;
import kfs.field.KfsField;

/**
 *
 * @author pavedrim
 */
public class CsvExport implements Comparator<CsvExport.CsveItem> {

    public static class CsveItem {

        static final Map<Class<? extends CsvStrConvertor>, CsvStrConvertor> convs = new HashMap();

        final Field field;
        final List<KfsField> fieldInner;
        final String sortName;
        final String sqlName;
        final CsvStrConvertor conv;

        CsveItem(Class cls, Field field, String csvDefInner, String sqlName,
                String sorintg, Class<? extends CsvStrConvertor> convClass) {
            this.field = field;
            fieldInner = new ArrayList<KfsField>();
            fieldInner.add(new KfsField(cls, field));
            Field lf = field;
            if (csvDefInner.length() > 0) {
                String[] inns = csvDefInner.split("\\.");
                for (String innerName : inns) {
                    if (innerName.length() > 0) {
                        Field inf;
                        try {
                            inf = lf.getType().getDeclaredField(innerName);
                        } catch (NoSuchFieldException ex) {
                            throw new CsvException("Cannot find inner field "
                                    + lf.getType().getSimpleName() + "." + innerName + " defined in "
                                    + cls.getSimpleName() + "." + field.getName(), ex);
                        } catch (SecurityException ex) {
                            throw new CsvException("Cannot find inner field "
                                    + lf.getType().getSimpleName() + "." + innerName + " defined in "
                                    + cls.getSimpleName() + "." + field.getName(), ex);
                        }
                        if (inf == null) {
                            throw new CsvException("Cannot find inner field "
                                    + lf.getType().getSimpleName() + "." + innerName + " defined in "
                                    + cls.getSimpleName() + "." + field.getName());
                        }
                        fieldInner.add(new KfsField(lf.getType(), inf));
                        lf = inf;
                    }
                }
            }
            if (sqlName.length() > 0) {
                this.sqlName = sqlName;
            } else {
                String cn = "";
                if (field.isAnnotationPresent(Column.class)) {
                    Column c = field.getAnnotation(Column.class);
                    cn = c.name();
                }
                if (cn.length() <= 0) {
                    cn = field.getName();
                }
                this.sqlName = cn;
            }
            if (sorintg.length() > 0) {
                this.sortName = sorintg;
            } else {
                this.sortName = field.getName();
            }
            if (convClass.equals(CsvStrConvertor.class)) {
                this.conv = null;
            } else {
                if (convs.containsKey(convClass)) {
                    this.conv = convs.get(convClass);
                } else {
                    try {
                        this.conv = convClass.newInstance();
                    } catch (InstantiationException ex) {
                        throw new CsvException("Cannot init " + convClass.getSimpleName(), ex);
                    } catch (IllegalAccessException ex) {
                        throw new CsvException("Cannot init " + convClass.getSimpleName(), ex);
                    }
                    convs.put(convClass, conv);
                }
            }
        }

        Object getValue(Object obj) {
            Object ret = obj;
            for (KfsField kf : fieldInner) {
                if (ret == null) {
                    return ret;
                }
                ret = kf.getVal(ret);
            }
            if ((ret != null) && (conv != null)) {
                ret = conv.export(obj);
            }
            return ret;
        }
    }

    private final Class cls;
    private final List<CsveItem> items;
    private final char sep;
    private final char encaps;
    private final boolean forceEncaps;

    public CsvExport(String exportName, Class cls, char sep, char encaps, boolean forceEncaps) {
        this.cls = cls;
        this.sep = sep;
        this.encaps = encaps;
        this.forceEncaps = forceEncaps;
        this.items = new ArrayList<CsveItem>();
        for (Field df : cls.getDeclaredFields()) {
            Csv csvDef = null;
            if (df.isAnnotationPresent(Csv.class)) {
                Csv c = df.getAnnotation(Csv.class);
                if (c.name().equals(exportName)) {
                    csvDef = c;
                }
            }
            if (csvDef == null) {
                if (df.isAnnotationPresent(CsvMulti.class)) {
                    CsvMulti cm = df.getAnnotation(CsvMulti.class);
                    if (cm != null) {
                        for (Csv c : cm.value()) {
                            if (c.name().equals(exportName)) {
                                csvDef = c;
                                break;
                            }
                        }
                    }
                }
            }
            if (csvDef != null) {
                items.add(new CsveItem(cls, df, csvDef.inner(), csvDef.sqlname(),
                        csvDef.sorting(), csvDef.conv()));
            }
            if (items.size() > 0) {
                Collections.sort(items, this);
            }
        }
        if (items.size() <= 0) {
            // use persistence api for definition
            Class<? extends CsvStrConvertor> convClass;
            for (Field df : cls.getDeclaredFields()) {
                if (df.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                if (df.getType().isAssignableFrom(Date.class)) {
                    convClass = TimestampConvertor.class;
                } else {
                    convClass = CsvStrConvertor.class;
                }
                String csvDefInner = "";
                if (df.isAnnotationPresent(CsvDbInner.class)) {
                    csvDefInner = df.getAnnotation(CsvDbInner.class).value();
                }
                String sqlName = "";
                if (df.isAnnotationPresent(CsvDbName.class)) {
                    sqlName = df.getAnnotation(CsvDbName.class).value();
                }
                items.add(new CsveItem(cls, df, csvDefInner, sqlName, "", convClass));
            }
        }

    }

    @Override
    public int compare(CsveItem o1, CsveItem o2) {
        return o1.sortName.compareTo(o2.sortName);
    }

    public CharSequence csvExport(Object obj) {
        StringBuilder sb = new StringBuilder();
        boolean f = true;
        for (CsveItem item : items) {
            if (f) {
                f = false;
            } else {
                sb.append(sep);
            }
            Object o = item.getValue(obj);
            if (o != null) {
                String s = o.toString();
                if (forceEncaps || (s.indexOf(encaps) >= 0) || (s.indexOf(sep) >= 0)) {
                    String se = Character.toString(encaps);
                    s = s.replaceAll(se, se + se);
                    sb.append(encaps).append(s).append(encaps);
                } else {
                    sb.append(s);
                }

            }
        }
        return sb;
    }

    public CharSequence getOracleSqlLdrControlFile(String tposfix) {
        String schema = "";
        String tableName = cls.getSimpleName();
        if (cls.isAnnotationPresent(Table.class)) {
            Table t = (Table) cls.getAnnotation(Table.class);
            if (!t.schema().isEmpty()) {
                schema = t.schema();
            }
            if (!t.name().isEmpty()) {
                tableName = t.name();
            }
        }
        return getOracleSqlLdrControlFile(tableName+tposfix, schema);
    }

    private String getColumnName(Field field, Column col) {
        if (col != null) {
            return col.name();
        }
        return field.getName();
    }

    public CharSequence getOracleSqlLdrControlFile(String tableName, String schema) {
        if (!schema.isEmpty()) {
            schema += ".";
        }
        StringBuilder sb = new StringBuilder();
        sb//
                .append("load data append into table ").append(schema)//
                .append(tableName).append(" fields terminated by \"")//
                .append(sep).append("\" ")
                .append(forceEncaps ? "" : "optionally ")//
                .append("enclosed by '")//
                .append(encaps)//
                .append("' TRAILING NULLCOLS (");
        boolean f = true;
        for (CsveItem item : items) {
            if (f) {
                f = false;
            } else {
                sb.append(",");
            }
            sb.append("\n");

            sb.append(item.sqlName);
            if (Timestamp.class.equals(item.field.getType())) {
                sb.append(" \"TO_TIMESTAMP(:").append(item.sqlName).append(", 'yyyy-mm-dd hh24:mi:ss.FF')\"");
            }
        }

        sb.append("\n)");
        return sb;
    }
}

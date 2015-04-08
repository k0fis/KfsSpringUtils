package kfs.csv;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Table;
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

        CsveItem(Class cls, Csv csvDef, Field field) {
            this.field = field;
            fieldInner = new ArrayList<KfsField>();
            fieldInner.add(new KfsField(cls, field));
            Field lf = field;
            if (csvDef.inner().length() > 0) {
                String[] inns = csvDef.inner().split("\\.");
                for (String innerName : inns) {
                    if (innerName.length() > 0) {
                        Field inf;
                        try {
                            inf = lf.getType().getField(innerName);
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
            if (csvDef.sqlname().length() > 0) {
                this.sqlName = csvDef.sqlname();
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
            if (csvDef.sorting().length() > 0) {
                this.sortName = csvDef.sorting();
            } else {
                this.sortName = field.getName();
            }
            if (csvDef.conv().equals(CsvStrConvertor.class)) {
                this.conv = null;
            } else {
                if (convs.containsKey(csvDef.conv())) {
                    this.conv = convs.get(csvDef.conv());
                } else {
                    try {
                        this.conv = csvDef.conv().newInstance();
                    } catch (InstantiationException ex) {
                        throw new CsvException("Cannot init " + csvDef.conv().getSimpleName(), ex);
                    } catch (IllegalAccessException ex) {
                        throw new CsvException("Cannot init " + csvDef.conv().getSimpleName(), ex);
                    }
                    convs.put(csvDef.conv(), conv);
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
                items.add(new CsveItem(cls, csvDef, df));
            }
        }
        if (items.size() > 0) {
            Collections.sort(items, this);
        } else {
            throw new CsvException("Cannot find CSV definition in class: " + cls.getSimpleName());
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

    public CharSequence getOracleSqlLdrControlFile() {
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
        return getOracleSqlLdrControlFile(tableName, schema);
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

package kfs.springutils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Query;
import kfs.anno.KfsSearchCriteria;
import kfs.field.KfsField;

/**
 *
 * @author pavedrim
 * @param <T>
 */
public class KfsQueryBuilder<T> {

    private final String prefix;
    private final List<KfsField> fieldList;
    private final Map<KfsField, KfsSearchCriteria> criterias;

    public KfsQueryBuilder(Class<T> cls, String prefix) {
        this.prefix = prefix;
        criterias = new HashMap<KfsField, KfsSearchCriteria>();
        fieldList = new ArrayList<KfsField>();
        for (Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(KfsSearchCriteria.class)) {
                KfsSearchCriteria cc = field.getAnnotation(KfsSearchCriteria.class);
                KfsField kf = new KfsField(cls, field);
                fieldList.add(kf);
                criterias.put(kf, cc);
            }
        }
    }

    private Object getValue(KfsField kf, T obj) {
        Object val = kf.getVal(obj);
        if (val == null) {
            return null;
        }
        if (val instanceof String) {
            String s = (String) val;
            if (s.length() <= 0) {
                return null;
            }
        }
        return val;
    }

    public CharSequence getWhere(T obj) {
        StringBuilder sb = new StringBuilder();
        int inx = 0;
        for (KfsField kf : fieldList) {
            Object val = getValue(kf, obj);
            if (val == null) {
                continue;
            }
            if (inx == 0) {
                sb.append(" WHERE ");
            } else {
                sb.append(" AND ");
            }
            KfsSearchCriteria crit = criterias.get(kf);
            String name = crit.name();
            if (name.length() <= 0) {
                name = kf.getName();
            }
            if (crit.like()) {
                sb.append(" lower(").append(prefix).append(".").append(name)
                        .append(") like :").append(name).append(" ");
            } else {
                sb.append(" ").append(prefix).append(".").append(name)
                        .append(" ").append(crit.operator())
                        .append(" :").append(kf.getName()).append(" ");
            }
            inx++;
        }
        return sb;
    }

    public void setParameters(T obj, Query query) {
        for (KfsField kf : fieldList) {
            Object val = getValue(kf, obj);
            if (val != null) {
                KfsSearchCriteria crit = criterias.get(kf);
                query.setParameter(kf.getName(), val);
                if (crit.like()) {
                    query.setParameter(kf.getName(), ((String) val).toLowerCase());
                } else {
                    query.setParameter(kf.getName(), val);
                }
            }
        }
    }
}

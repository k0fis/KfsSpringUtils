package kfs.field;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import kfs.utils.KfsSpringUtilsException;

/**
 *
 * @author pavedrim
 */
public class KfsField {

    private Object data;
    private final Class cls;
    private final Field field;
    private Method setFieldMethod;
    private Method getFieldMethod;

    public KfsField(Class cls, Field field) {
        this.cls = cls;
        this.field = field;
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

    public String getName() {
        return field.getName();
    }
    
    public Class getFieldType() {
        return field.getType();
    }
    
    public Field getField() {
        return field;
    }

    protected Method getSetFieldMethod() {
        if (setFieldMethod == null) {
            setFieldMethod = findSGetter("set", cls, field.getName(), field.getType());
        }
        return setFieldMethod;
    }

    protected Method getGetFieldMethod() {
        if (getFieldMethod == null) {
            getFieldMethod = findSGetter("get", cls, field.getName());
        }
        return getFieldMethod;
    }
    
    public void setVal(Object ret, Object value) {
        try {
            getSetFieldMethod().invoke(ret, value);
        } catch (IllegalAccessException ex) {
            throw new KfsSpringUtilsException("Cannot set " 
                    + field.getName() +" with value: "
                    + value, ex);
        } catch (IllegalArgumentException ex) {
            throw new KfsSpringUtilsException("Cannot set " 
                    + field.getName() +" with value: "
                    + value, ex);
        } catch (InvocationTargetException ex) {
            throw new KfsSpringUtilsException("Cannot set " 
                    + field.getName() +" with value: "
                    + value, ex);
        }
    }    

    public Object getVal(Object ret) {
        try {
            return getGetFieldMethod().invoke(ret);
        } catch (IllegalAccessException ex) {
            throw new KfsSpringUtilsException("Cannot get " + field.getName(), ex);
        } catch (IllegalArgumentException ex) {
            throw new KfsSpringUtilsException("Cannot get " + field.getName(), ex);
        } catch (InvocationTargetException ex) {
            throw new KfsSpringUtilsException("Cannot get " + field.getName(), ex);
        }
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

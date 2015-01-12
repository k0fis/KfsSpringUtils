package kfs.springutils;

/**
 *
 * @author pavedrim
 * @param <T>
 * @param <I>
 */
public interface BaseDao<T, I> {

    public void insert(T date);

    public void update(T data);

    public void delete(T data);

    public T findById(I id);

    public T find(T data);

}

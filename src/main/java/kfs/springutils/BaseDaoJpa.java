package kfs.springutils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author pavedrim
 * @param <T> Domain Class
 * @param <I> Domain object id class, Long or string
 */
public abstract class BaseDaoJpa<T, I> implements BaseDao<T, I> {

    @PersistenceContext()
    protected EntityManager em;
    
    protected abstract Class<T> getDataClass();
    
    protected abstract I getId(T data);
    
    @Override
    public void insert(T date) {
        em.persist(date);
    }

    @Override
    public void update(T data) {
        em.merge(data);
    }

    @Override
    public void delete(T data) {
        em.remove(find(data));
    }

    @Override
    public T findById(I id) {
        return em.find(getDataClass(), id);
    }

    @Override
    public T find(T data) {
        return em.find(getDataClass(), getId(data));
    }

}

package kfs.domain;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import kfs.kfsvaalib.kfsForm.KfsField;
import kfs.kfsvaalib.kfsForm.KfsMField;
import kfs.kfsvaalib.kfsTable.KfsTablePos;
import kfs.kfsvaalib.kfsTable.Pos;
import org.hibernate.annotations.GenericGenerator;

/**
 *
 * @author pavedrim
 */
@MappedSuperclass
public class BaseCounter {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(unique = true, nullable = false)
    @KfsTablePos(@Pos(value = 10, name = "t0"))
    @KfsMField({
        @KfsField(name = "i", pos = 10, isRequired = true),
        @KfsField(name = "u", pos = 10, isRequired = true, readOnly = true)
    })                
    private String name;
    
    @KfsTablePos(@Pos(value = 20, name = "t0"))
    @KfsMField({
        @KfsField(name = "i", pos = 20),
        @KfsField(name = "u", pos = 20)
    })                
    private String note;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BaseCounter other = (BaseCounter) obj;
        return !((this.id == null) ? (other.id != null) : !this.id.equals(other.id));
    }
    
    
}

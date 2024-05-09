package lgl.bayern.de.ecertby.model;

import com.querydsl.core.annotations.QuerySupertype;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@QuerySupertype
@MappedSuperclass
@OptimisticLocking(type = OptimisticLockType.VERSION)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  private String id;

}

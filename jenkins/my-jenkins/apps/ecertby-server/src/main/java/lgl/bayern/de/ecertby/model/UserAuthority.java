package lgl.bayern.de.ecertby.model;

import com.eurodyn.qlack.fuse.aaa.model.UserGroup;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Data
@Entity
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UserAuthority extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_USER_DETAIL", nullable = false)
    private UserDetail userDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_AUTHORITY")
    private Authority authority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_USER_GROUP")
    private UserGroup userGroup;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SEARCH_CRITERIA")
    private SearchCriteria searchCriteria;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_ROLE_IN_PROCESS")
    private CatalogValue roleInProcess;
}

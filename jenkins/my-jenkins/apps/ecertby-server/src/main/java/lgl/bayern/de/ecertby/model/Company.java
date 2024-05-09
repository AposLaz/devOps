package lgl.bayern.de.ecertby.model;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import java.time.Instant;
import java.util.Set;

@Data
@Entity
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Company extends BaseEntity {
    @NotNull
    private String name;

    @NotNull
    private String address;

    private String email;

    private String telephone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_PRE_RESP_AUTHORITY", nullable = false)
    private Authority preResponsibleAuthority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_POST_RESP_AUTHORITY", nullable = false)
    private Authority postResponsibleAuthority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_RESPONSIBLE_AUTHORITY", nullable = false)
    private Authority responsibleAuthority;

    @NotNull
    private boolean active;

    @NotNull
    private boolean deleted;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="FK_COMPANY", nullable = false)
    private Set<CompanyDepartment> department;

    private Instant registrationDate;
    private Instant deregistrationDate;
}

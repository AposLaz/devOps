package lgl.bayern.de.ecertby.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

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
public class CompanyProfile extends BaseEntity {
    @NotNull
    private String profileName;

    @NotNull
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_COMPANY", nullable = false)
    private Company company;

    @NotNull
    private boolean active;

    @OneToMany(fetch = FetchType.LAZY , cascade = CascadeType.ALL , orphanRemoval = true)
    @JoinColumn(name = "fk_company_profile", nullable = false)
    private Set<CompanyProfileCountry> targetCountry;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL} , orphanRemoval = true)
    @JoinColumn(name="fk_company_profile", nullable = false)
    private Set<CompanyProfileProduct> product;
}

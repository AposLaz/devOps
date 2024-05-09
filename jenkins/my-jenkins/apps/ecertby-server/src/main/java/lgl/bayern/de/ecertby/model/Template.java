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

import java.time.Instant;
import java.util.List;
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
public class Template extends BaseEntity {

    @NotNull
    private String templateName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_target_country", nullable = false)
    private TargetCountry targetCountry;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_product", nullable = false)
    private CatalogValue product;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="fk_template_department", nullable = false)
    private Set<TemplateDepartment> department;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="fk_template_keyword", nullable = false)
    private Set<TemplateKeyword> keyword;

    private Instant validFrom;

    private Instant validTo;

    @NotNull
    private boolean active;

    @NotNull
    private boolean release;

    private String comment;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="fk_template")
    private Set<TemplateElement> templateElementSet;

    /**
     * The html elements.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "template", cascade = {CascadeType.ALL})
    @OrderBy("SORT_ORDER ASC")
    private List<HtmlElement> htmlElements;

    @Override
    public String toString() {
        return "Template{" +
                "id=" + getId() +
                ", templateName=" + templateName +
                ", targetCountry=" + targetCountry +
                ", product='" + ((product.toString() != null) ? product.toString() : "") + '\'' +
                ", department='" + ((department.toString() != null) ? department.toString() : "") + '\'' +
                ", validFrom=" + validFrom +
                ", validTo='" +  validTo + '\'' +
                ", active='" + active + '\'' +
                ", release=" + release +
                ", comment='" +  comment + '\'' +
                '}';
    }

}

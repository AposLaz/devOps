package lgl.bayern.de.ecertby.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.model.util.ElementType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Attribute extends BaseEntity {

    @NotNull
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ElementType elementType;

    private boolean isRequired;

    private boolean selectedForRelease;

    private boolean companyRelated;

    private String defaultValue;
    private String defaultTextAreaValue;

    private String dateFormat;

    private String decimalSeparator;

    private String decimalDigits;

    private String thousandSeparator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_CATALOG")
    private Catalog catalog;

    @NotNull
    private String htmlElementName;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "attribute", orphanRemoval = true)
    private List<AttributeRadioOption> radioOptionList = new ArrayList<>();

}

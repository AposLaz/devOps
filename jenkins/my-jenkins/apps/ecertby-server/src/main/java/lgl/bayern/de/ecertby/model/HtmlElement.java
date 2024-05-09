package lgl.bayern.de.ecertby.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.model.util.ElementType;
import lgl.bayern.de.ecertby.model.util.Font;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

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
public class HtmlElement extends BaseEntity {

    @NotNull
    private String name;

    private String tooltip;

    /**
     * The template.
     */
    @ManyToOne
    @JoinColumn(name = "FK_TEMPLATE", nullable = false)
    private Template template;

    /**
     * The template element.
     */
    @ManyToOne
    @JoinColumn(name = "FK_TEMPLATE_ELEMENT")
    private TemplateElement templateElement;

    /**
     * The catalog.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_CATALOG")
    private Catalog catalog;

    /**
     * The attribute.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_ATTRIBUTE")
    private Attribute attribute;

    /**
     * The default value.
     */
    @Column(name = "DEFAULT_VALUE")
    private String defaultValue;

    /**
     * The sort order.
     */
    @Column(name = "SORT_ORDER", nullable = false)
    private int sortOrder;

    /**
     * The max chars.
     */
    @Column(name = "MAX_CHARS")
    private int maxChars;

    /**
     * The date format.
     */
    @Column(name = "DATE_FORMAT")
    private String dateFormat;

    /**
     * The decimal separator.
     */
    @Column(name = "DECIMAL_SEPARATOR")
    private String decimalSeparator;

    /**
     * The decimal digits.
     */
    @Column(name = "DECIMAL_DIGITS")
    private String decimalDigits;

    /**
     * The thousand separator.
     */
    @Column(name = "THOUSAND_SEPARATOR")
    private String thousandSeparator;

    /**
     * The required.
     */
    @Column(name = "IS_REQUIRED")
    private boolean required;

    /**
     * The bold.
     */
    @Column(name = "IS_BOLD")
    private boolean bold;

    /**
     * The italics.
     */
    @Column(name = "IS_ITALICS")
    private boolean italics;

    /**
     * The underline.
     */
    @Column(name = "IS_UNDERLINE")
    private boolean underline;


    /**
     * The visible.
     */
    @Column(name = "IS_VISIBLE")
    private boolean visible;

    /**
     * The font size.
     */
    private short fontSize;

    /**
     * The font.
     */
    @Enumerated(EnumType.STRING)
    private Font font;

    /**
     * The company related.
     */
    @Column(name = "COMPANY_RELATED")
    private boolean companyRelated;

    /**
     * The selected for release.
     */
    @Column(name = "SELECTED_FOR_RELEASE")
    private boolean selectedForRelease;

    /**
     * The layout.
     */
    private short layout;

    /**
     * The radio buttons.
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="fk_html_element", nullable = false)
    @OrderBy("SORT_ORDER ASC")
    private List<HtmlElementRadioButton> radioButtons;

    /**
     * The elementType.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    private ElementType elementType;

    @Override
    public String toString() {
        return "HtmlElement{" +
                "id=" + getId() +
                ", name=" + name +
                ", tooltip=" + tooltip +
                ", attribute=" + attribute.toString() +
                ", defaultValue=" + defaultValue +
                ", sortOrder='" +  sortOrder + '\'' +
                ", maxChars='" + maxChars + '\'' +
                ", dateFormat=" + dateFormat +
                ", decimalSeparator='" +  decimalSeparator + '\'' +
                ", decimalDigits=" + decimalDigits +
                ", thousandSeparator='" +  thousandSeparator + '\'' +
                ", required='" + required + '\'' +
                ", bold=" + bold +
                ", italics='" +  italics + '\'' +
                ", fontSize=" + fontSize +
                ", companyRelated='" +  companyRelated + '\'' +
                ", selectedForRelease='" + selectedForRelease + '\'' +
                ", elementType='" +  elementType + '\'' +
                '}';
    }

}

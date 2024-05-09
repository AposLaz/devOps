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

@Data
@Entity
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class HtmlElementRadioButton extends BaseEntity {

    /**
     * The name.
     */
    @NotNull
    private String name;

/*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_HTML_ELEMENT", nullable = false)
    private HtmlElement htmlElement;
*/

    /**
     * The template element value.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_TEMPLATE_ELEMENT_VALUE")
    private TemplateElementValue templateElementValue;

    /**
     * The sort order.
     */
    @Column(name = "SORT_ORDER", nullable = false)
    private int sortOrder;

    /**
     * The attrubute radio button.
     */
  /*  @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ATTRIBUTE_RADIO_BUTTON_ID")
	private AttributeRadioButton attributeRadioButton;
*/
    @Override
    public String toString() {
        return "HtmlElement{" +
                "id=" + getId() +
                ", name=" + name +
                ", templateElementValue=" + templateElementValue +
                ", sortOrder='" + sortOrder + '\'' +
                '}';
    }

}

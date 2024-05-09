package lgl.bayern.de.ecertby.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.model.util.GroupType;
import lgl.bayern.de.ecertby.repository.SearchCriteriaRepository;
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
public class SearchCriteriaGroup extends BaseEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "GROUP_TYPE" , nullable= false)
    private GroupType groupType;


}

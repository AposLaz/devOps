package lgl.bayern.de.ecertby.dto;

import com.eurodyn.qlack.fuse.aaa.annotation.ResourceId;
import com.eurodyn.qlack.fuse.fd.dto.ThreadMessageDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class FeatureBoardThreadDTO extends ThreadMessageDTO {
    private String firstNameFilter;
    private String lastNameFilter;
    private String emailFilter;
    private String authorityFilter;
    private String companyFilter;
    private Long positiveReviews;
    private Long negativeReviews;
    private boolean published;
    private List<FeatureBoardThreadDTO> nestedComments;
    private boolean userUpvoted;
    private boolean userDownvoted;
    private String anonymous;
    private String userViewOptions;
    @ResourceId
    private String resourceId;

}

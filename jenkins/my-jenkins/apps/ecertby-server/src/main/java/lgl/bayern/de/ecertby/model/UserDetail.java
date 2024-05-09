package lgl.bayern.de.ecertby.model;

import com.eurodyn.qlack.fuse.aaa.model.User;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import lgl.bayern.de.ecertby.config.AppConstants;
import lgl.bayern.de.ecertby.model.util.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

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
public class UserDetail extends UserDetailProfile {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_aaa_user", nullable = false)
    private User user;

    @NotNull
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type" , nullable= false)
    private UserType userType;

    @Formula("case user_type when 'ADMIN_USER' then '"+ AppConstants.EnumTranslations.ADMIN_USER
            +"' when 'AUTHORITY_USER' then '"+ AppConstants.EnumTranslations.AUTHORITY_USER
            +"' when 'COMPANY_USER' then '"+ AppConstants.EnumTranslations.COMPANY_USER +"' end")
    private String userTypeText;

    private String refreshToken;

    private int refreshCounter;


    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name="FK_USER_DETAIL", nullable = false, updatable=false, insertable=false)
    private Set<UserAuthority> userAuthoritySet;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name="FK_USER_DETAIL", nullable = false, updatable=false, insertable=false)
    private Set<UserCompany> userCompanySet;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_SEARCH_CRITERIA")
    private SearchCriteria searchCriteria;

    @Override
    public String toString() {
        return "UserDetail{" +
                "user=" + user +
                ", active=" + active +
                ", userType=" + userType +
                ", userTypeText='" + userTypeText + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", refreshCounter=" + refreshCounter +
                '}';
    }

}

package lgl.bayern.de.ecertby.model;

import com.querydsl.core.annotations.QuerySupertype;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Set;

@Data
@QuerySupertype
@MappedSuperclass
@OptimisticLocking(type = OptimisticLockType.VERSION)
@EntityListeners(AuditingEntityListener.class)
public class UserDetailProfile extends BaseEntity{

    private String salutation;

    private String firstName;

    private String lastName;

    private String username;

    @NotNull
    private String email;

    private String emailExtern;

    private String telephone;

    private String mobileNumber;

    private String mobileDienstnummer;

    private String additionalContactInfo;


    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name="fk_user_detail", nullable = false)
    private Set<UserDepartment> department;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_company", nullable = true)
    private Company primaryCompany;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_authority", nullable = true)
    private Authority primaryAuthority;

}

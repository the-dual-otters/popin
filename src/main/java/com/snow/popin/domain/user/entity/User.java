package com.snow.popin.domain.user.entity;

import com.snow.popin.domain.auth.constant.AuthProvider;
import com.snow.popin.domain.category.entity.Category;
import com.snow.popin.domain.category.entity.UserInterest;
import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.domain.user.constant.UserStatus;
import com.snow.popin.global.common.BaseEntity;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String nickname;

    private String phone;

    @Column(name = "auth_provider")
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserInterest> interests = new ArrayList<>();

    @Builder
    public User(String email, String password, String name, String nickname,
                String phone, AuthProvider authProvider, Role role, UserStatus status) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
        this.authProvider = authProvider;
        this.role = role != null ? role : Role.USER;
        this.status = status != null ? status : UserStatus.ACTIVE;
    }

    public User(String email, String password, String name, String nickname,
                String phone, AuthProvider authProvider, Role role) {
        this(email, password, name, nickname, phone, authProvider, role, UserStatus.ACTIVE);
    }

    // 비즈니스 로직 메소드들
    public void updateProfile(String name, String nickname, String phone) {
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
    }

    public void updateInterests(List<Category> newCategories) {
        this.interests.clear();
        for (Category category : newCategories) {
            this.interests.add(new UserInterest(this, category));
        }
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    // Role 업데이트
    public void updateRole(Role newRole){
        this.role = newRole;
    }


    public void updateStatus(UserStatus status){
        this.status = status;
    }

    /**
     * 사용자의 관심 카테고리 이름 목록 조회
     */
    public List<String> getInterestCategoryNames() {
        return this.interests.stream()
                .map(userInterest -> userInterest.getCategory().getName())
                .collect(Collectors.toList());
    }

}
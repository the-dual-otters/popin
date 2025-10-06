package com.snow.popin.domain.auth.dto;

import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@NoArgsConstructor
@Builder
public class SignupRequest {

    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 2, max = 10, message = "이름은 2자 이상 10자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 15, message = "닉네임은 2자 이상 15자 이하여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다.")
    private String nickname;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String passwordConfirm;

    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "휴대폰 번호는 010-0000-0000 형식으로 입력해주세요.")
    private String phone;

    // 관심사 (카테고리 ID 리스트)
    private List<Long> interestCategoryIds;

    // 선택한 관심사 태그들 (임시로 문자열 리스트로 받기)
    private List<String> interests;

    // @Builder를 사용할 때 필요한 생성자를 명시적으로 정의
    @Builder
    public SignupRequest(String name, String nickname, String email,
                         String password, String passwordConfirm, String phone,
                         List<Long> interestCategoryIds, List<String> interests) {
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
        this.phone = phone;
        this.interestCategoryIds = interestCategoryIds;
        this.interests = interests;
    }

    // 비밀번호 확인 검증
    public boolean isPasswordMatching() {
        return password != null && password.equals(passwordConfirm);
    }
}
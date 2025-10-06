package com.snow.popin.domain.user.service;

import com.snow.popin.domain.user.dto.UserResponseDto;
import com.snow.popin.domain.user.dto.UserUpdateRequestDto;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import com.snow.popin.global.util.UserUtil;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserUtil userUtil;
    private final PasswordEncoder passwordEncoder;

    public User findById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
    }

    public User findByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
    }
    public Long getUserIdByUsername(String username) {
        User u = userRepository.findByEmail(username).orElse(null);
        return u != null ? u.getId() : null;
    }


    public UserResponseDto getCurrentUserInfo() {
        User user = userUtil.getCurrentUser();
        return new UserResponseDto(user);
    }

    @Transactional
    public UserResponseDto updateCurrentUser(UserUpdateRequestDto dto) {
        User user = userUtil.getCurrentUser();
        user.updateProfile(dto.getName(), dto.getNickname(), dto.getPhone());
        return new UserResponseDto(user);
    }



    @Transactional
    public void changePassword(Long userId, String newPassword){

        User user = findById(userId);
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.changePassword(encodedPassword);

    }


}

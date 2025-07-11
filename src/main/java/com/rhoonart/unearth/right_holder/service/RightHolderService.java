package com.rhoonart.unearth.right_holder.service;

import com.rhoonart.unearth.right_holder.dto.RightHolderListResponseDto;
import com.rhoonart.unearth.right_holder.dto.RightHolderRegisterRequestDto;
import com.rhoonart.unearth.right_holder.entity.HolderType;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.user.entity.User;
import com.rhoonart.unearth.user.entity.Role;
import com.rhoonart.unearth.user.repository.UserRepository;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.ResponseCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RightHolderService {
    private final RightHolderRepository rightHolderRepository;
    private final SongInfoRepository songInfoRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<RightHolderListResponseDto> findRightHolders(HolderType holderType, String holderName,
            LocalDate contractDate, Pageable pageable) {
        Page<RightHolder> page = rightHolderRepository.search(holderType, holderName, contractDate, pageable);
        return page.map(rh -> RightHolderListResponseDto.of(
                rh.getId(),
                rh.getHolderType(),
                rh.getHolderName(),
                rh.getContractStart(),
                rh.getContractEnd(),
                songInfoRepository.countByRightHolder(rh)));
    }

    @Transactional
    public void register(RightHolderRegisterRequestDto dto) {
        // 1. username 중복 체크
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BaseException(ResponseCode.INVALID_INPUT, "이미 사용 중인 아이디입니다.");
        }
        // 1-2. holderName(권리사 이름) 중복 체크
        if (rightHolderRepository.existsByHolderName(dto.getHolderName())) {
            throw new BaseException(ResponseCode.INVALID_INPUT, "이미 등록된 권리사 이름입니다.");
        }
        // 2. User 생성 및 저장 (권리자 role)
        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword())) // 비밀번호 암호화
                .role(Role.RIGHT_HOLDER)
                .isLoginEnabled(true)
                .build();
        userRepository.save(user);
        // 3. RightHolder 생성 및 저장
        RightHolder rightHolder = RightHolder.builder()
                .user(user)
                .holderType(com.rhoonart.unearth.right_holder.entity.HolderType.valueOf(dto.getHolderType()))
                .holderName(dto.getHolderName())
                .contractStart(dto.getContractStart())
                .contractEnd(dto.getContractEnd())
                .businessNumber(dto.getBusinessNumber())
                .build();
        rightHolderRepository.save(rightHolder);
    }
}
package com.rhoonart.unearth.right_holder.service;

import com.rhoonart.unearth.common.util.DataAuthorityService;
import com.rhoonart.unearth.right_holder.dto.RightHolderListResponseDto;
import com.rhoonart.unearth.right_holder.dto.RightHolderRegisterRequestDto;
import com.rhoonart.unearth.right_holder.dto.RightHolderUpdateRequestDto;
import com.rhoonart.unearth.right_holder.dto.RightHolderSongListResponseDto;
import com.rhoonart.unearth.right_holder.entity.HolderType;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.crawling.repository.CrawlingPeriodRepository;
import com.rhoonart.unearth.crawling.repository.CrawlingDataRepository;
import com.rhoonart.unearth.user.dto.UserDto;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RightHolderService {
    private final RightHolderRepository rightHolderRepository;
    private final SongInfoRepository songInfoRepository;
    private final CrawlingDataRepository crawlingDataRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataAuthorityService dataAuthorityService;

    public Page<RightHolderListResponseDto> findRightHolders(HolderType holderType, String holderName,
            LocalDate contractDate, Pageable pageable) {
        Page<RightHolder> page = rightHolderRepository.search(holderType, holderName, contractDate, pageable);
        return page.map(rh -> {
            // 계약 종료일까지 남은 일수 계산
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), rh.getContractEnd());

            return RightHolderListResponseDto.of(
                    rh.getId(),
                    rh.getHolderType(),
                    rh.getHolderName(),
                    rh.getContractStart(),
                    rh.getContractEnd(),
                    rh.getBusinessNumber(),
                    songInfoRepository.countByRightHolder(rh),
                    daysLeft,
                    rh.getUser().isLoginEnabled());
        });
    }

    public List<String> findAllForDropdown() {
        return rightHolderRepository.findAllHolderNames();
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

    public RightHolder findById(String rightHolderId) {
        return rightHolderRepository.findById(rightHolderId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다."));
    }

    public RightHolder findByUserId(String userId) {
        return rightHolderRepository.findByUserId(userId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다."));
    }

    @Transactional
    public void update(String rightHolderId, RightHolderUpdateRequestDto dto) {
        // 1. 권리자 존재 여부 확인
        RightHolder rightHolder = rightHolderRepository.findById(rightHolderId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다."));

        // 2. holderName 중복 체크 (자신 제외)
        if (!rightHolder.getHolderName().equals(dto.getHolderName()) &&
                rightHolderRepository.existsByHolderName(dto.getHolderName())) {
            throw new BaseException(ResponseCode.INVALID_INPUT, "이미 등록된 권리사 이름입니다.");
        }

        // 3. businessNumber 중복 체크 (자신 제외)
        if (!rightHolder.getBusinessNumber().equals(dto.getBusinessNumber()) &&
                rightHolderRepository.existsByBusinessNumber(dto.getBusinessNumber())) {
            throw new BaseException(ResponseCode.INVALID_INPUT, "이미 등록된 사업자 번호입니다.");
        }

        // 4. username 중복 체크 (권리자명이 변경되는 경우)
        if (!rightHolder.getHolderName().equals(dto.getHolderName())) {
            if (userRepository.existsByUsername(dto.getHolderName())) {
                throw new BaseException(ResponseCode.INVALID_INPUT, "이미 사용 중인 아이디입니다.");
            }
        }

        // 5. 권리자 정보 업데이트
        rightHolder.updateInfo(
                HolderType.valueOf(dto.getHolderType()),
                dto.getHolderName(),
                dto.getContractStart(),
                dto.getContractEnd(),
                dto.getBusinessNumber());

        // 6. User의 username도 함께 업데이트
        User user = rightHolder.getUser();
        user.updateUsername(dto.getHolderName());
        userRepository.save(user);

        rightHolderRepository.save(rightHolder);
    }

    @Transactional
    public void extendContract(String rightHolderId, String newEndDate) {
        // 권리자 존재 여부 확인
        RightHolder rightHolder = rightHolderRepository.findById(rightHolderId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다."));

        // 새로운 계약 종료일 파싱
        LocalDate newEndDateParsed;
        try {
            newEndDateParsed = LocalDate.parse(newEndDate);
        } catch (Exception e) {
            throw new BaseException(ResponseCode.INVALID_INPUT, "올바르지 않은 날짜 형식입니다.");
        }

        // 현재 계약 종료일보다 늦은 날짜인지 확인
        if (newEndDateParsed.isBefore(rightHolder.getContractEnd())
                || newEndDateParsed.isEqual(rightHolder.getContractEnd())) {
            throw new BaseException(ResponseCode.INVALID_INPUT, "새로운 계약 종료일은 현재 계약 종료일보다 늦어야 합니다.");
        }

        // 계약 종료일 업데이트
        rightHolder.updateContractEnd(newEndDateParsed);
        rightHolderRepository.save(rightHolder);
    }

    @Transactional
    public void toggleLoginStatus(String rightHolderId, boolean isLoginEnabled) {
        // 권리자 존재 여부 확인
        RightHolder rightHolder = rightHolderRepository.findById(rightHolderId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다."));

        // User의 로그인 상태 업데이트
        User user = rightHolder.getUser();
        user.updateLoginEnabled(isLoginEnabled);
        userRepository.save(user);
    }
}
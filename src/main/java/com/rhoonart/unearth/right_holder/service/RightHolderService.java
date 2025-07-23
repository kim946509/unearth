package com.rhoonart.unearth.right_holder.service;

import com.rhoonart.unearth.common.util.ValidateInput;
import com.rhoonart.unearth.right_holder.dto.RightHolderListResponseDto;
import com.rhoonart.unearth.right_holder.dto.RightHolderRegisterRequestDto;
import com.rhoonart.unearth.right_holder.dto.RightHolderUpdateRequestDto;
import com.rhoonart.unearth.right_holder.entity.HolderType;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.user.entity.User;
import com.rhoonart.unearth.user.entity.Role;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.user.service.UserSignUpService;
import com.rhoonart.unearth.user.service.UserUpdateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RightHolderService {
    private final RightHolderRepository rightHolderRepository;
    private final SongInfoRepository songInfoRepository;
    private final UserSignUpService userSignUpService;
    private final RightHolderCreateService rightHolderCreateService;
    private final UserUpdateService userUpdateService;
    private final RightHolderUpdateService rightHolderUpdateService;

    /**
     * 권리자 등록
     * @param dto
     */
    @Transactional
    public void register(RightHolderRegisterRequestDto dto) {
        // 1. user 생성
        User user = userSignUpService.signUp(dto.getUsername(),dto.getPassword(),Role.RIGHT_HOLDER);

        // 2. 권리자 생성
        RightHolder rightHolder = rightHolderCreateService.createRightHolder(dto, user);
    }

    public Page<RightHolderListResponseDto> findRightHolders(String holderTypeStr,String contractDateStr, String holderName, int page, int size) {

        // 빈 문자열이면 null로 변환
        HolderType holderType = null;
        if (holderTypeStr != null && !holderTypeStr.isBlank()) {
            holderType = HolderType.valueOf(holderTypeStr);
        }

        if (holderName != null && holderName.isBlank())
            holderName = null;

        LocalDate contractDate = null;
        if (contractDateStr != null && !contractDateStr.isBlank()) {
            contractDate = java.time.LocalDate.parse(contractDateStr);
        }

        // size 제한: 10, 30, 50만 허용
        size = ValidateInput.restrictPageSize(size);
        Pageable pageable = PageRequest.of(page, size);

        Page<RightHolder> searchResult = rightHolderRepository.search(holderType, holderName, contractDate, pageable);
        return searchResult.map(rh -> {
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

        // 2. 권리자 정보 업데이트
        rightHolderUpdateService.update(rightHolder, dto);

        // 3. username 업데이트
        userUpdateService.updateUsername(rightHolder.getUser(),dto.getHolderName());
    }



    @Transactional
    public void rightHolderLoginStatusUpdate(String rightHolderId, boolean isLoginEnabled) {
        // 권리자 존재 여부 확인
        RightHolder rightHolder = rightHolderRepository.findById(rightHolderId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다."));

        userUpdateService.updateLoginEnabled(rightHolder.getUser(), isLoginEnabled);
    }
}
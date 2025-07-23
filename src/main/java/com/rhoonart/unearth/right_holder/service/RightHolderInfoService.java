package com.rhoonart.unearth.right_holder.service;

import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RightHolderInfoService {

    private final RightHolderRepository rightHolderRepository;

    /**
     * 권리자 이름 중복 체크
     */
    public boolean isDuplicateRightHolderName(String rightHolderName) {
        return rightHolderRepository.existsByHolderName(rightHolderName);
    }

    /**
     * 권리자 사업자 등록번호 중복 체크
     */
    public boolean isDuplicateBusinessNumber(String businessNumber) {
        return rightHolderRepository.existsByBusinessNumber(businessNumber);
    }

    public Optional<RightHolder> findRightHolderById(String rightHolderId) {
        return rightHolderRepository.findById(rightHolderId);
    }

    /**
     * 권리자 이름으로 권리자 조회
     */
    public Optional<RightHolder> findRightHolderByName(String holderName) {
        return rightHolderRepository.findByHolderName(holderName);
    }
}

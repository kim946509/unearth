package com.rhoonart.unearth.right_holder.service;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RightHolderUtilService {

    private final RightHolderRepository rightHolderRepository;

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
}

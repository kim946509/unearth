package com.rhoonart.unearth.right_holder.service;

import com.rhoonart.unearth.right_holder.dto.RightHolderRegisterRequestDto;
import com.rhoonart.unearth.right_holder.entity.HolderType;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.user.entity.User;
import com.rhoonart.unearth.user.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RightHolderCreateService {

    private final RightHolderRepository rightHolderRepository;
    private final RightHolderInfoService rightHolderInfoService;

    @Transactional
    public RightHolder createRightHolder(RightHolderRegisterRequestDto dto, User user) {

        if(rightHolderInfoService.isDuplicateRightHolderName(dto.getHolderName())){
            throw new BadRequestException("이미 등록된 사업자명입니다.");
        }

        if(rightHolderInfoService.isDuplicateBusinessNumber(dto.getBusinessNumber())){
            throw new BadRequestException("이미 등록된 사업자 등록번호입니다.");
        }

        // 2. RightHolder 생성 및 저장
        RightHolder rightHolder = RightHolder.builder()
                .user(user)
                .holderType(HolderType.valueOf(dto.getHolderType()))
                .holderName(dto.getHolderName())
                .contractStart(dto.getContractStart())
                .contractEnd(dto.getContractEnd())
                .businessNumber(dto.getBusinessNumber())
                .build();

        return rightHolderRepository.save(rightHolder);
    }
}

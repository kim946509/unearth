package com.rhoonart.unearth.right_holder.service;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.right_holder.dto.RightHolderUpdateRequestDto;
import com.rhoonart.unearth.right_holder.entity.HolderType;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.user.exception.BadRequestException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RightHolderUpdateService {

    private final RightHolderInfoService rightHolderInfoService;
    private final RightHolderRepository rightHolderRepository;

    @Transactional
    public RightHolder update(RightHolder rightHolder, RightHolderUpdateRequestDto dto){

        // 권리자 이름 중복 체크
        if (!rightHolder.getHolderName().equals(dto.getHolderName()) &&
                rightHolderInfoService.isDuplicateRightHolderName(dto.getHolderName())) {
            throw new BadRequestException("이미 등록된 권리사 이름입니다.");
        }

        // 사업자 등록번호 중복 체크
        if( !rightHolder.getBusinessNumber().equals(dto.getBusinessNumber()) &&
                rightHolderInfoService.isDuplicateBusinessNumber(dto.getBusinessNumber())) {
            throw new BadRequestException("이미 등록된 사업자 등록번호입니다.");
        }

        rightHolder.updateInfo(
                HolderType.valueOf(dto.getHolderType()),
                dto.getHolderName(),
                dto.getContractStart(),
                dto.getContractEnd(),
                dto.getBusinessNumber()
        );

        return rightHolderRepository.save(rightHolder);
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
}

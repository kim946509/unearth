package com.rhoonart.unearth.right_holder.service;

import com.rhoonart.unearth.right_holder.dto.RightHolderListResponseDto;
import com.rhoonart.unearth.right_holder.entity.HolderType;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RightHolderService {
    private final RightHolderRepository rightHolderRepository;
    private final SongInfoRepository songInfoRepository;

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
}
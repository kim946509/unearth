package com.rhoonart.unearth.right_holder.service;

import com.rhoonart.unearth.right_holder.dto.RightHolderListResponseDto;
import com.rhoonart.unearth.right_holder.dto.RightHolderRegisterRequestDto;
import com.rhoonart.unearth.right_holder.dto.RightHolderSongListResponseDto;
import com.rhoonart.unearth.right_holder.entity.HolderType;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.crawling.repository.CrawlingPeriodRepository;
import com.rhoonart.unearth.crawling.repository.CrawlingDataRepository;
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
    private final CrawlingPeriodRepository crawlingPeriodRepository;
    private final CrawlingDataRepository crawlingDataRepository;
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

    public Page<RightHolderSongListResponseDto> findSongsByRightHolder(String rightHolderId, String search,
            Pageable pageable) {
        // 권리자 존재 여부 확인
        RightHolder rightHolder = rightHolderRepository.findById(rightHolderId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다."));

        // 권리자별 노래 조회
        Page<SongInfo> songPage = songInfoRepository.findByRightHolderIdWithSearch(rightHolderId, search, pageable);

        // DTO 변환
        return songPage.map(song -> {
            // 크롤링 데이터 존재 여부 확인
            boolean hasCrawlingData = crawlingDataRepository.existsBySongId(song.getId());

            return RightHolderSongListResponseDto.builder()
                    .songId(song.getId())
                    .rightHolderName(song.getRightHolder().getHolderName())
                    .artistKo(song.getArtistKo())
                    .albumKo(song.getAlbumKo())
                    .titleKo(song.getTitleKo())
                    .youtubeUrl(song.getYoutubeUrl())
                    .hasCrawlingData(hasCrawlingData)
                    .build();
        });
    }

    public RightHolder findById(String rightHolderId) {
        return rightHolderRepository.findById(rightHolderId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다."));
    }
}
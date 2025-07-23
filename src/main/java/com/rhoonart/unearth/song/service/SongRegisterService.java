package com.rhoonart.unearth.song.service;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.service.RightHolderInfoService;
import com.rhoonart.unearth.song.dto.SongInfoRegisterRequestDto;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongRegisterService {

    private final SongInfoRepository songInfoRepository;
    private final RightHolderInfoService rightHolderInfoService;

    @Transactional
    public void register(SongInfoRegisterRequestDto dto) {
        // 1. songId(멜론) 중복 체크
        if (songInfoRepository.existsByMelonSongId(dto.getMelonSongId())) {
            throw new BaseException(ResponseCode.INVALID_INPUT, "이미 등록된 멜론 곡 ID입니다.");
        }

        // 2. artist_ko와 title_ko 중복 체크
        if (songInfoRepository.existsByArtistKoAndTitleKo(dto.getArtistKo(), dto.getTitleKo())) {
            throw new BaseException(ResponseCode.INVALID_INPUT,
                    String.format("이미 등록된 곡입니다: %s - %s", dto.getArtistKo(), dto.getTitleKo()));
        }

        // 3. 권리자명으로 RightHolder 조회
        RightHolder rightHolder = rightHolderInfoService.findRightHolderByName(dto.getRightHolderName())
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다."));

        // 4. 엔티티 생성/저장
        SongInfo song = SongInfo.builder()
                .artistKo(dto.getArtistKo())
                .artistEn(dto.getArtistEn())
                .albumKo(dto.getAlbumKo())
                .albumEn(dto.getAlbumEn())
                .titleKo(dto.getTitleKo())
                .titleEn(dto.getTitleEn())
                .youtubeUrl(dto.getYoutubeUrl())
                .melonSongId(dto.getMelonSongId())
                .rightHolder(rightHolder)
                .build();
        songInfoRepository.save(song);
    }
}

package com.rhoonart.unearth.song.service;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.service.RightHolderInfoService;
import com.rhoonart.unearth.right_holder.service.RightHolderService;
import com.rhoonart.unearth.song.dto.SongInfoUpdateRequestDto;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SongUpdateService {

    private final SongInfoService songInfoService;
    private final SongInfoRepository songInfoRepository;
    private final RightHolderInfoService rightHolderInfoService;

    @Transactional
    public void update(String songId, SongInfoUpdateRequestDto dto) {
        // 1. 음원 존재 여부 확인
        SongInfo song = songInfoService.getSongInfoById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        // 2. melonSongId 중복 체크 (자신 제외) - null 체크 추가
        String currentMelonSongId = song.getMelonSongId();
        String newMelonSongId = dto.getMelonSongId();

        // null 체크를 포함한 안전한 비교
        boolean melonSongIdChanged = (currentMelonSongId == null && newMelonSongId != null) ||
                (currentMelonSongId != null && !currentMelonSongId.equals(newMelonSongId));

        if (melonSongIdChanged && newMelonSongId != null &&
                songInfoRepository.existsByMelonSongId(newMelonSongId)) {
            throw new BaseException(ResponseCode.INVALID_INPUT, "이미 등록된 멜론 곡 ID입니다.");
        }

        // 3. artist_ko와 title_ko 중복 체크 (자신 제외)
        if (!song.getArtistKo().equals(dto.getArtistKo()) || !song.getTitleKo().equals(dto.getTitleKo())) {
            if (songInfoRepository.existsByArtistKoAndTitleKoExcludingId(dto.getArtistKo(), dto.getTitleKo(), songId)) {
                throw new BaseException(ResponseCode.INVALID_INPUT,
                        String.format("이미 등록된 곡입니다: %s - %s", dto.getArtistKo(), dto.getTitleKo()));
            }
        }

        // 4. 권리자명으로 RightHolder 조회
        RightHolder rightHolder = rightHolderInfoService.findRightHolderByName(dto.getRightHolderName())
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다."));

        // 5. 음원 정보 업데이트
        song.updateInfo(
                dto.getArtistKo(),
                dto.getArtistEn(),
                dto.getAlbumKo(),
                dto.getAlbumEn(),
                dto.getTitleKo(),
                dto.getTitleEn(),
                dto.getYoutubeUrl(),
                dto.getMelonSongId(),
                rightHolder);

        songInfoRepository.save(song);
    }

}

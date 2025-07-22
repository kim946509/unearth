package com.rhoonart.unearth.right_holder.service;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.util.DataAuthorityService;
import com.rhoonart.unearth.crawling.repository.CrawlingDataRepository;
import com.rhoonart.unearth.right_holder.dto.RightHolderSongListResponseDto;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RightHolderSongService {

    private final SongInfoRepository songInfoRepository;
    private final DataAuthorityService dataAuthorityService;
    private final CrawlingDataRepository crawlingDataRepository;

    public Page<RightHolderSongListResponseDto> findSongsByRightHolder(UserDto userDto, String rightHolderId, String search,
                                                                       Boolean hasCrawlingData, Pageable pageable) {
        // 사용자 권한 확인
        if(!dataAuthorityService.isAccessRightHolderData(userDto, rightHolderId)){
            throw new BaseException(ResponseCode.FORBIDDEN, "권리자 데이터에 접근할 수 없습니다.");
        }

        // 권리자별 노래 조회 (크롤링 데이터 필터링 포함)
        Page<SongInfo> songPage = songInfoRepository.findByRightHolderIdWithSearchAndCrawlingFilter(
                rightHolderId, search, hasCrawlingData, pageable);

        // DTO 변환
        return songPage.map(song -> {
            // 크롤링 데이터 존재 여부 확인 (필터링된 결과이므로 hasCrawlingData가 true인 경우는 이미 확인됨)
            boolean songHasCrawlingData =
                    hasCrawlingData != null && hasCrawlingData || crawlingDataRepository.existsBySongId(song.getId());

            return RightHolderSongListResponseDto.builder()
                    .songId(song.getId())
                    .rightHolderName(song.getRightHolder().getHolderName())
                    .artistKo(song.getArtistKo())
                    .albumKo(song.getAlbumKo())
                    .titleKo(song.getTitleKo())
                    .youtubeUrl(song.getYoutubeUrl())
                    .hasCrawlingData(songHasCrawlingData)
                    .build();
        });
    }
}

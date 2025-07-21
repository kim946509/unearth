package com.rhoonart.unearth.song.util;

import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 유틸리티 클래스: 권리자 음원 소유 여부 확인
@Component
@RequiredArgsConstructor
public class SongOwnershipUtil {

    private final SongInfoRepository songInfoRepository;
    private final RightHolderRepository rightHolderRepository;

    public boolean isSongOwnedByUser(String songId, String rightHolderId ) {

        SongInfo songInfo = songInfoRepository.findByIdAndRightHolderId(songId, rightHolderId)
                .orElse(null); // 음원이 권리자에 속하는지 확인

        if( songInfo == null) {
            return false; // 음원이 권리자에 속하지 않음
        }

        return true;
    }
}

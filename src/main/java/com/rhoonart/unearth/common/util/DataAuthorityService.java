package com.rhoonart.unearth.common.util;

import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.service.SongInfoService;
import com.rhoonart.unearth.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataAuthorityService {

    private final SongInfoService songInfoService;

    public boolean isAccessSongData(UserDto user, SongInfo songInfo) {
        // 관리자 권한 확인
        if (user.isAdmin()) {
            return true;
        }

        if(songInfo!=null && songInfo.getRightHolder().getId().equals(user.getRightHolderId())){
            return true; // 권리자가 소유한 음원 데이터에 접근 가능
        }

        return false; // 권리자가 소유하지 않은 음원 데이터에 접근 불가
    }

    public boolean isAccessSongData(UserDto user, String songId){
        SongInfo songInfo = songInfoService.getSongInfoById(songId).orElse(null);
        return isAccessSongData(user, songInfo); // 권리자가 소유하지 않은 음원 데이터에 접근 불가
    }

    public boolean isAccessRightHolderData(UserDto user, String rightHolderId) {
        // 관리자 권한 확인
        if (user.isAdmin()) {
            return true;
        }

        // 권리자 ID가 일치하는지 확인
        return user.getRightHolderId().equals(rightHolderId);
    }
}

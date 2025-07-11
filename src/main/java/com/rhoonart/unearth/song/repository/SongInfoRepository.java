package com.rhoonart.unearth.song.repository;

import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SongInfoRepository extends JpaRepository<SongInfo, String> {
    int countByRightHolder(RightHolder rightHolder);
}
package com.umc.footprint.src.common.repository;

import com.umc.footprint.src.footprints.model.entity.Footprint;
import com.umc.footprint.src.common.model.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Integer> {

    List<Photo> findPhotoByFootprint(Footprint footprint);

    List<Photo> findAllByUserIdx(int userIdx);

    List<Photo> findAllByFootprintAndStatus(Footprint footprint, String status);

}

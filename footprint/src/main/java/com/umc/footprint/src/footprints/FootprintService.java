package com.umc.footprint.src.footprints;

import com.umc.footprint.config.BaseException;
import static com.umc.footprint.config.BaseResponseStatus.*;

import com.umc.footprint.config.EncryptProperties;
import com.umc.footprint.src.AwsS3Service;
import com.umc.footprint.src.footprints.model.GetFootprintRes;
import com.umc.footprint.src.footprints.model.PatchFootprintReq;
import com.umc.footprint.src.model.*;
import com.umc.footprint.src.repository.*;
import com.umc.footprint.src.walks.WalkService;
import com.umc.footprint.utils.AES128;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FootprintService {
    private final FootprintDao footprintDao;
    private final FootprintProvider footprintProvider;
    private final WalkRepository walkRepository;
    private final FootprintRepository footprintRepository;
    private final PhotoRepository photoRepository;
    private final TagRepository tagRepository;
    private final HashtagRepository hashtagRepository;
    private final WalkService walkService;
    private final UserRepository userRepository;
    private final AwsS3Service awsS3Service;
    private final EncryptProperties encryptProperties;

    @Autowired
    public FootprintService(FootprintDao footprintDao, FootprintProvider footprintProvider, WalkRepository walkRepository, FootprintRepository footprintRepository, PhotoRepository photoRepository, TagRepository tagRepository, HashtagRepository hashtagRepository, WalkService walkService, UserRepository userRepository, AwsS3Service awsS3Service, EncryptProperties encryptProperties) {
        this.footprintDao = footprintDao;
        this.footprintProvider = footprintProvider;
        this.walkRepository = walkRepository;
        this.footprintRepository = footprintRepository;
        this.photoRepository = photoRepository;
        this.tagRepository = tagRepository;
        this.hashtagRepository = hashtagRepository;
        this.walkService = walkService;
        this.userRepository = userRepository;
        this.awsS3Service = awsS3Service;
        this.encryptProperties = encryptProperties;
    }

    // 발자국 수정 (Patch)
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void modifyFootprint(PatchFootprintReq patchFootprintReq, int walkIdx, int footprintIdx, String userId) throws BaseException {
        try {
            Integer userIdx = userRepository.findByUserId(userId).getUserIdx();
            Footprint footprintByNumber = getFootprintByNumber(walkIdx, footprintIdx, userIdx);

            // 발자국 수정 과정
            // 1. 본문 수정
            if(patchFootprintReq.getWrite() != null) {
                footprintByNumber.recordDecrypt(new AES128(encryptProperties.getKey()).encrypt(patchFootprintReq.getWrite()));

                footprintRepository.save(footprintByNumber);
            }

            // 2. 사진 수정
            // DB에 저장되어 있는 파일 리스트
            List<Photo> photoList = photoRepository.findAllByFootprintAndStatus(footprintByNumber, "ACTIVE");
            // 전달되어온 파일 리스트
            List<MultipartFile> photos = patchFootprintReq.getPhotos();

            if (photos != null) { // 본문만 수정(사진 수정 X)하는 경우 photos 자체가 null이 됨

                if (photoList.isEmpty()) { // 발자국에 저장된 사진이 존재하지 않음
                    if (!("".equals(photos.get(0).getOriginalFilename()))) { // 전달된 파일이 하나라도 존재
                        uploadImg(photos, userIdx, footprintByNumber); // 새로운 사진들 업로드
                    }
                } else { // 발자국에 저장된 기존 사진들이 존재
                    for (Photo photo : photoList) {
                        photo.changeStatus("INACTIVE");
                    }
                    photoRepository.saveAll(photoList);

                    if (photos.size() != 1 || !"".equals(photos.get(0).getOriginalFilename())) { // 전달된 파일이 없음 > 사진을 지우고 싶다는 의미 > 기존 사진 삭제만 진행
                        uploadImg(photos, userIdx, footprintByNumber); // 새로운 사진들 업로드
                    }
                }
            }

            // 3. 태그 수정
            List<Hashtag> hashtagList = new ArrayList<>();
            List<Tag> tagList = new ArrayList<>();

            if(patchFootprintReq.getTagList() != null) {
                for(String tag : patchFootprintReq.getTagList()){
                    hashtagList.add(
                            Hashtag.builder()
                            .hashtag(new AES128(encryptProperties.getKey()).encrypt(tag))
                            .build()
                    );
                }
                List<Hashtag> savedHashtagList = hashtagRepository.saveAll(hashtagList);

                if (!footprintByNumber.getTagList().isEmpty()) {
                    for (Tag tag : footprintByNumber.getTagList()) {
                        tag.changeStatus("INACTIVE");
                    }
                    tagRepository.saveAll(footprintByNumber.getTagList());
                }

                for (Hashtag hashtag : savedHashtagList) {
                    Tag tag = Tag.builder()
                            .userIdx(userIdx)
                            .status("ACTIVE")
                            .build();
                    tag.setFootprint(footprintByNumber);
                    tag.setHashtag(hashtag);
                    tagList.add(tag);
                    footprintByNumber.addTagList(tag);
                }
                tagRepository.saveAll(tagList);

            }
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }
    }


    // 발자국 삭제 (PATCH) - 사진 삭제, 태그 삭제, 발자국 기록 삭제
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void deleteFootprint(int footprintIdx) throws BaseException {
        try {
            int activeFootprint = footprintDao.activeFootprint(footprintIdx);
            int footprintExist = footprintDao.footprintExist(footprintIdx);

            if (footprintExist == 0) { // 발자국이 존재하지 않을 때
                throw new BaseException(NO_EXIST_FOOTPRINT);
            }
            else if (activeFootprint == 0) { // 이미 삭제된 발자국
                throw new BaseException(DELETED_FOOTPRINT);
            }

            footprintDao.deletePhotos(footprintIdx);
            footprintDao.deleteHashtags(footprintIdx);
            footprintDao.deleteFootprint(footprintIdx); // 발자국 삭제 성공 - 1, 실패 - 0
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // 이미지 URL 생성 > S3 업로드 > Photo 테이블 삽입
    @Transactional(rollbackFor = Exception.class)
    public List<String> uploadImg(List<MultipartFile> photos, int userIdx, Footprint footprint) throws BaseException {
        List<String> urlList = new ArrayList<>(); // URL 저장할 리스트
        List<Photo> photoList = new ArrayList<>();

        // 이미지 URL 생성 및 S3 업로드
        try {
            for (MultipartFile file : photos) {
                String imgUrl = awsS3Service.uploadFile(file);
                urlList.add(new AES128(encryptProperties.getKey()).encrypt(imgUrl));
                Photo photo = Photo.builder()
                        .imageUrl(imgUrl)
                        .status("ACTIVE")
                        .userIdx(userIdx)
                        .build();
                photo.setFootprint(footprint);
                photoList.add(photo);
            }
        } catch (Exception exception){
            throw new BaseException(DATABASE_ERROR);
        }

        // Photo 테이블에 insert
        photoRepository.saveAll(photoList);

        return urlList;
    }

    // 발자국 조회
    public List<GetFootprintRes> getFootprints(int walkIdx, String userId) throws BaseException {
        try {
            Integer userIdx = userRepository.findByUserId(userId).getUserIdx();

            Walk walkByNumber = walkService.getWalkByNumber(walkIdx, userIdx);

            List<GetFootprintRes> getFootprintRes = new ArrayList<>();

            List<Footprint> footprintList = footprintRepository.findAllByWalkAndStatus(walkByNumber, "ACTIVE");

            /* 발자국 조회시 복호화를 위한 코드 : write, photo, tag 복호화 필요 */

            for (Footprint footprint : footprintList) {
                List<String> decryptPhotoList = new ArrayList<>();
                List<String> decryptTagList = new ArrayList<>();

                List<Photo> photoList = photoRepository.findAllByFootprintAndStatus(footprint, "ACTIVE");
                for (Photo photo : photoList) {
                    decryptPhotoList.add(new AES128(encryptProperties.getKey()).decrypt(photo.getImageUrl()));
                }
                for (Tag tag : footprint.getTagList()) {
                    decryptTagList.add(new AES128(encryptProperties.getKey()).decrypt(tag.getHashtag().getHashtag()));
                }
                getFootprintRes.add(GetFootprintRes.builder()
                        .footprintIdx(footprint.getFootprintIdx())
                        .recordAt(footprint.getRecordAt())
                        .write(new AES128(encryptProperties.getKey()).decrypt(footprint.getRecord()))
                        .photoList(decryptPhotoList)
                        .tagList(decryptTagList)
                        .onWalk(footprint.getOnWalk())
                        .build());
            }

            if (getFootprintRes.isEmpty()){
                throw new BaseException(NO_FOOTPRINT_IN_WALK); // 산책 기록에 발자국이 없을 때
            }
            return getFootprintRes;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public Footprint getFootprintByNumber(int walkNumber, int footprintNumber, Integer userIdx) throws BaseException {
        Walk walkByNumber = walkService.getWalkByNumber(walkNumber, userIdx);
        PageRequest pageRequest = PageRequest.of(footprintNumber - 1, 1);
        try {
            Page<Footprint> footprintPage = footprintRepository.findByWalkAndStatusOrderByRecordAtAsc(walkByNumber, "ACTIVE", pageRequest);
            if (footprintPage.getTotalElements() == 0) {
                throw new BaseException(DELETED_FOOTPRINT);
            }
            Footprint footprint = footprintPage.get().collect(Collectors.toList()).get(0);
            return footprint;
        } catch (Exception exception) {
            throw new BaseException(INVALID_FOOTPRINTIDX);
        }
    }
}

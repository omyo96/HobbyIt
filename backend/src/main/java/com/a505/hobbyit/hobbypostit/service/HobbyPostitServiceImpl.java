package com.a505.hobbyit.hobbypostit.service;

import com.a505.hobbyit.common.file.FileUploader;
import com.a505.hobbyit.hobby.domain.Hobby;
import com.a505.hobbyit.hobby.domain.HobbyRepository;
import com.a505.hobbyit.hobby.exception.NoSuchHobbyException;
import com.a505.hobbyit.hobbymember.domain.HobbyMemberRepository;
import com.a505.hobbyit.hobbymember.exception.NoSuchHobbyMemberException;
import com.a505.hobbyit.hobbypostit.domain.HobbyPostit;
import com.a505.hobbyit.hobbypostit.domain.HobbyPostitRepository;
import com.a505.hobbyit.hobbypostit.dto.HobbyPostitResponse;
import com.a505.hobbyit.hobbypostit.exception.UnAuthorizedHobbyPostitException;
import com.a505.hobbyit.hobbypostitrecord.domain.HobbyPostitRecord;
import com.a505.hobbyit.hobbypostitrecord.domain.HobbyPostitRecordRepository;
import com.a505.hobbyit.member.domain.Member;
import com.a505.hobbyit.member.domain.MemberRepository;
import com.a505.hobbyit.member.exception.NoSuchMemberException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HobbyPostitServiceImpl implements HobbyPostitService {

    private final HobbyPostitRepository hobbyPostitRepository;
    private final MemberRepository memberRepository;
    private final HobbyRepository hobbyRepository;
    private final HobbyMemberRepository hobbyMemberRepository;
    private final HobbyPostitRecordRepository hobbyPostitRecordRepository;
    private final FileUploader fileUploader;

    @Transactional
    @Override
    public void save(Long memberId, Long hobbyId, LocalDate date, MultipartFile multipartFile) {
        LocalDateTime curDateTime = LocalDateTime.now();
        if (!date.isEqual(curDateTime.toLocalDate()) || (curDateTime.getHour() == 23 && 55 < curDateTime.getMinute()))
            throw new UnAuthorizedHobbyPostitException("방명록 작성 가능 시간은 당일 00시 00분 ~ 23시 55분입니다.");
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchMemberException("회원 정보 오류"));
        Hobby hobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new NoSuchHobbyException("소모임 정보 오류"));
        hobbyMemberRepository.findByMemberAndHobby(member, hobby)
                .orElseThrow(() -> new NoSuchHobbyMemberException("소모임 가입 정보 오류"));

        String imgUrl = fileUploader.upload(multipartFile, hobbyId);
        HobbyPostit hobbyPostit = HobbyPostit.builder().member(member).hobby(hobby).imgUrl(imgUrl).build();
        hobbyPostitRepository.save(hobbyPostit);
        if (hobbyPostitRecordRepository
                .findByHobbyAndYearAndMonth(hobby, date.getYear(), date.getMonthValue())
                .isEmpty()
        ) {
            hobbyPostitRecordRepository.save(
                    HobbyPostitRecord.builder()
                            .hobby(hobby)
                            .year(curDateTime.getYear())
                            .month(curDateTime.getMonthValue())
                            .day(curDateTime.getDayOfMonth())
                            .build()
            );
        }
    }

    @Override
    public List<HobbyPostitResponse> findHobbyPostits(Long memberId, Long hobbyId, LocalDate date) {
//        if (!LocalDate.now().isAfter(date))
//            throw new UnAuthorizedHobbyPostitException("방명록을 조회할 수 있는 날짜가 아닙니다.");
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchMemberException("회원 정보 오류"));
        Hobby hobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new NoSuchHobbyException("소모임 정보 오류"));
        hobbyMemberRepository.findByMemberAndHobby(member, hobby)
                .orElseThrow(() -> new NoSuchHobbyMemberException("소모임 가입 정보 오류"));

        return hobbyPostitRepository.findByHobbyAndRegDtBetween(
                hobby,
                LocalDateTime.of(date, LocalTime.of(0, 0, 0)),
                LocalDateTime.of(date, LocalTime.of(23, 59, 59))
        ).stream().map((hobbyPostit -> new HobbyPostitResponse().of(hobbyPostit))).toList();
    }
}
package com.kangwon.ai_asistant_be.voidce.service;

import com.kangwon.ai_asistant_be.voidce.domain.Member;
import com.kangwon.ai_asistant_be.voidce.domain.dto.MemberDTO;
import com.kangwon.ai_asistant_be.voidce.domain.dto.repostiory.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public void save(MemberDTO memberDTO) {
        Optional<Member> optionalMember = memberRepository.findByfamilyNameAndGivenName(memberDTO.getFamilyName(), memberDTO.getGivenName());

        Member member;
        if (optionalMember.isPresent()) {
            member = optionalMember.get();
            // 기존 멤버 정보 업데이트
            member.setPhoneNumber(memberDTO.getPhoneNumber());
            member.setAddress(memberDTO.getAddress());
            member.setBirth(memberDTO.getBirth());
            member.setGender(memberDTO.getGender());
            member.setHeight(memberDTO.getHeight());
            member.setWeight(memberDTO.getWeight());
            member.setBloodType(memberDTO.getBloodType());
            member.setRhNegative(member.isRhNegative());
            // 더 많은 필드를 업데이트할 수 있습니다.
        } else {
            member = memberDTO.from(memberDTO);
        }

        memberRepository.save(member);
    }
}
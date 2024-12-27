package com.kangwon.ai_asistant_be.voidce.domain.dto;

import com.kangwon.ai_asistant_be.voidce.domain.Member;
import lombok.Data;

@Data
public class MemberDTO {
    private String familyName;
    private String givenName;
    private String birth;
    private String phoneNumber;
    private String address;
    private String gender;
    private int height;
    private int weight;
    private String bloodType;
    private boolean rhNegative;

    public Member from(MemberDTO memberDTO) {
        Member member = new Member();
        member.setFamilyName(memberDTO.getFamilyName());
        member.setGivenName(memberDTO.getGivenName());
        member.setBirth(memberDTO.getBirth());
        member.setGender(memberDTO.getGender());
        member.setHeight(memberDTO.getHeight());
        member.setWeight(memberDTO.getWeight());
        member.setBloodType(memberDTO.getBloodType());
        member.setRhNegative(memberDTO.isRhNegative());
        return member;
    }
}

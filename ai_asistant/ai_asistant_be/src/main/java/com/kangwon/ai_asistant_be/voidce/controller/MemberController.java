package com.kangwon.ai_asistant_be.voidce.controller;

import com.kangwon.ai_asistant_be.voidce.domain.dto.MemberDTO;
import com.kangwon.ai_asistant_be.voidce.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/save")
    public void save(@RequestBody MemberDTO memberDTO) {
        memberService.save(memberDTO);
    }
}

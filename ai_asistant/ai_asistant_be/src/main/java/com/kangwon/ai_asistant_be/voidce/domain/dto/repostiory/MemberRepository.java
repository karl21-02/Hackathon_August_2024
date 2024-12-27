package com.kangwon.ai_asistant_be.voidce.domain.dto.repostiory;

import com.kangwon.ai_asistant_be.voidce.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByfamilyNameAndGivenName(String familyName, String givenName);
}
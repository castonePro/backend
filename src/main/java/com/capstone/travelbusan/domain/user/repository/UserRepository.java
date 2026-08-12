package com.capstone.travelbusan.domain.user.repository;

import com.capstone.travelbusan.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    //이메일에 해당하는 유저 정보가 존재하는지 확인
    Optional<User> findByEmail(String email);

    // 닉네임 중복 체크
    boolean existsByNickname(String nickname);
}
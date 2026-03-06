package com.blur.userservice.identity.repository;

import com.blur.userservice.identity.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, String> {
}

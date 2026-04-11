package com.studio.crm_system.repository;

import com.studio.crm_system.entity.ClientReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientReviewRepository extends JpaRepository<ClientReview, Long> {

	List<ClientReview> findByClientIdOrderByCreatedAtDesc(Long clientId);

	@Query("SELECT AVG(r.score) FROM ClientReview r WHERE r.client.id = :clientId")
	Double getAverageScoreByClientId(@Param("clientId") Long clientId);
}

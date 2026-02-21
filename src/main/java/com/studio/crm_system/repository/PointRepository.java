package com.studio.crm_system.repository;

import com.studio.crm_system.entity.Point;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointRepository extends JpaRepository<Point, Long> {

	List<Point> findByIsDeletedFalseOrderByNameAsc();
}

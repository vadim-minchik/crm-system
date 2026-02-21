package com.studio.crm_system.service;

import com.studio.crm_system.entity.Point;
import com.studio.crm_system.repository.PointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PointService {

	@Autowired
	private PointRepository pointRepository;

	public List<Point> findAllActive() {
		return pointRepository.findByIsDeletedFalseOrderByNameAsc();
	}

	public Optional<Point> findById(Long id) {
		return pointRepository.findById(id);
	}

	public Optional<Point> findByIdAndNotDeleted(Long id) {
		return pointRepository.findById(id)
				.filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()));
	}

	@Transactional
	public String create(String name, String address) {
		if (name == null || name.isBlank()) return "name_required";
		Point p = new Point();
		p.setName(name.trim());
		p.setAddress(address != null && !address.isBlank() ? address.trim() : null);
		p.setIsDeleted(false);
		pointRepository.save(p);
		return null;
	}

	@Transactional
	public String update(Long id, String name, String address) {
		Point p = pointRepository.findById(id).orElse(null);
		if (p == null) return "not_found";
		if (name == null || name.isBlank()) return "name_required";
		p.setName(name.trim());
		p.setAddress(address != null && !address.isBlank() ? address.trim() : null);
		pointRepository.save(p);
		return null;
	}

	@Transactional
	public String softDelete(Long id) {
		Point p = pointRepository.findById(id).orElse(null);
		if (p == null) return "not_found";
		p.setIsDeleted(true);
		pointRepository.save(p);
		return null;
	}
}

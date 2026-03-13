package com.studio.crm_system.service;

import com.studio.crm_system.entity.Client;
import com.studio.crm_system.entity.ClientReview;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.ClientRepository;
import com.studio.crm_system.repository.ClientReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClientReviewService {

	@Autowired
	private ClientReviewRepository clientReviewRepository;

	@Autowired
	private ClientRepository clientRepository;

	private static final int DEFAULT_RATING = 10;
	private static final int MIN_SCORE = 1;
	private static final int MAX_SCORE = 10;

	public List<ClientReview> findByClientIdOrderByCreatedAtDesc(Long clientId) {
		return clientReviewRepository.findByClientIdOrderByCreatedAtDesc(clientId);
	}

	public Optional<ClientReview> findById(Long id) {
		return clientReviewRepository.findById(id);
	}

	/**
	 * Добавить отзыв и пересчитать рейтинг клиента (среднее по отзывам).
	 * Рейтинг вручную не редактируется — только через отзывы.
	 */
	@Transactional
	public ClientReview addReview(Long clientId, User author, int score, String comment) {
		if (score < MIN_SCORE || score > MAX_SCORE) {
			throw new IllegalArgumentException("Оценка должна быть от " + MIN_SCORE + " до " + MAX_SCORE);
		}
		Client client = clientRepository.findById(clientId).orElseThrow(() -> new IllegalArgumentException("Клиент не найден"));
		ClientReview review = new ClientReview();
		review.setClient(client);
		review.setAuthor(author);
		review.setScore(score);
		review.setComment(comment != null ? comment.trim() : "");
		clientReviewRepository.save(review);
		recalculateClientRating(clientId);
		return review;
	}

	/**
	 * Удалить отзыв и пересчитать рейтинг клиента.
	 */
	@Transactional
	public void deleteReview(Long reviewId) {
		ClientReview review = clientReviewRepository.findById(reviewId).orElse(null);
		if (review == null) return;
		Long clientId = review.getClient().getId();
		clientReviewRepository.delete(review);
		recalculateClientRating(clientId);
	}

	/**
	 * Пересчитать рейтинг клиента как среднее по всем отзывам (1–10).
	 * Если отзывов нет — ставим 10.
	 */
	public void recalculateClientRating(Long clientId) {
		Client client = clientRepository.findById(clientId).orElse(null);
		if (client == null) return;
		Double avg = clientReviewRepository.getAverageScoreByClientId(clientId);
		int newRating = (avg != null && !avg.isNaN())
			? (int) Math.round(avg)
			: DEFAULT_RATING;
		newRating = Math.max(MIN_SCORE, Math.min(MAX_SCORE, newRating));
		client.setRating(newRating);
		clientRepository.save(client);
	}
}

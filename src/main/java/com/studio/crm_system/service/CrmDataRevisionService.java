package com.studio.crm_system.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Монотонный счётчик изменений в БД (после коммита транзакций с save/delete через репозитории).
 * Используется клиентом для определения устаревшей страницы при работе нескольких пользователей.
 */
@Service
public class CrmDataRevisionService {

	private final AtomicLong revision = new AtomicLong(0);

	public long getRevision() {
		return revision.get();
	}

	public void bump() {
		revision.incrementAndGet();
	}
}

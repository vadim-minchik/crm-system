package com.studio.crm_system.web;

import java.util.Objects;

/**
 * Проверка версии сущности из формы относительно актуальной в БД (оптимистичная блокировка).
 */
public final class OptimisticLockSupport {

	private OptimisticLockSupport() {}

	/**
	 * @param expectedVersion версия, которую пользователь видел при открытии формы (null — форма без поля версии)
	 * @param currentVersion  актуальная версия из БД
	 * @return true, если сохранять нельзя (запись уже изменена другим пользователем)
	 */
	public static boolean isStale(Long expectedVersion, Long currentVersion) {
		if (expectedVersion == null || currentVersion == null) {
			return true;
		}
		return !Objects.equals(expectedVersion, currentVersion);
	}
}

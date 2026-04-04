package com.studio.crm_system.web;

import java.util.Objects;


public final class OptimisticLockSupport {

	private OptimisticLockSupport() {}

	
	public static boolean isStale(Long expectedVersion, Long currentVersion) {
		if (expectedVersion == null || currentVersion == null) {
			return true;
		}
		return !Objects.equals(expectedVersion, currentVersion);
	}
}

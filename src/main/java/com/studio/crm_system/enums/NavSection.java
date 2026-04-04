package com.studio.crm_system.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Разделы CRM в меню и соответствующие права Spring Security.
 */
public enum NavSection {
	STAFF("STAFF", "MENU_STAFF", "/staff", "Штат сотрудников"),
	CALLBACKS("CALLBACKS", "MENU_CALLBACKS", "/callbacks", "Перезвон"),
	CLIENTS("CLIENTS", "MENU_CLIENTS", "/clients", "База клиентов"),
	INVENTORY("INVENTORY", "MENU_INVENTORY", "/inventory", "Склад техники"),
	POINTS("POINTS", "MENU_POINTS", "/points", "Точки выдачи"),
	RENTALS("RENTALS", "MENU_RENTALS", "/rentals", "Журнал прокатов"),
	DOCUMENTS("DOCUMENTS", "MENU_DOCUMENTS", "/documents", "Документы"),
	STATISTICS("STATISTICS", "MENU_STATISTICS", "/statistics", "Статистика");

	private static final Map<String, NavSection> BY_CODE = Arrays.stream(values())
			.collect(Collectors.toMap(NavSection::getCode, Function.identity()));

	private final String code;
	private final String authority;
	private final String entryPath;
	private final String titleRu;

	NavSection(String code, String authority, String entryPath, String titleRu) {
		this.code = code;
		this.authority = authority;
		this.entryPath = entryPath;
		this.titleRu = titleRu;
	}

	public String getCode() {
		return code;
	}

	public String getAuthority() {
		return authority;
	}

	public String getEntryPath() {
		return entryPath;
	}

	public String getTitleRu() {
		return titleRu;
	}

	public static NavSection fromCode(String raw) {
		if (raw == null)
			return null;
		String c = raw.trim().toUpperCase();
		return BY_CODE.get(c);
	}
}

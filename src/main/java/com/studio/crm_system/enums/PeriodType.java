package com.studio.crm_system.enums;

/**
 * Периодичность повторяющегося расхода.
 */
public enum PeriodType {
	DAILY("Раз в день"),
	WEEKLY("Раз в неделю"),
	MONTHLY("Раз в месяц");

	private final String label;

	PeriodType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}

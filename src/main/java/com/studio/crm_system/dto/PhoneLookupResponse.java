package com.studio.crm_system.dto;

public record PhoneLookupResponse(boolean matched, Long clientId, String surname, String name, String patronymic) {
}

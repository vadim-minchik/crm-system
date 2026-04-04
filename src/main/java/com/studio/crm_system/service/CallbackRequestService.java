package com.studio.crm_system.service;

import com.studio.crm_system.dto.PhoneLookupResponse;
import com.studio.crm_system.entity.CallbackRequest;
import com.studio.crm_system.entity.Client;
import com.studio.crm_system.entity.Equipment;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.CallbackRequestRepository;
import com.studio.crm_system.repository.ClientRepository;
import com.studio.crm_system.repository.EquipmentRepository;
import com.studio.crm_system.security.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CallbackRequestService {

	private static final int MAX_LEN = 2000;
	private static final int MAX_NAME = 50;

	@Autowired
	private CallbackRequestRepository callbackRequestRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private EquipmentRepository equipmentRepository;

	@Autowired
	private InputValidator inputValidator;

	public List<CallbackRequest> findAll() {
		return findAll(false);
	}

	
	public List<CallbackRequest> findAll(boolean prioritizeRemindDayBefore) {
		List<CallbackRequest> list = callbackRequestRepository.findAllWithEquipmentOrderByCreatedAtDesc();
		if (!prioritizeRemindDayBefore) {
			return list;
		}
		return list.stream()
				.sorted(Comparator.comparing(CallbackRequestService::remindListPriority)
						.thenComparing(CallbackRequest::getRemindAt, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(CallbackRequest::getCreatedAt, Comparator.reverseOrder()))
				.collect(Collectors.toList());
	}

	
	private static int remindListPriority(CallbackRequest cb) {
		if (cb.getRemindAt() == null) {
			return 99;
		}
		LocalDate d = cb.getRemindAt().toLocalDate();
		LocalDate today = LocalDate.now();
		if (d.equals(today)) {
			return 0;
		}
		if (d.equals(today.plusDays(1))) {
			return 1;
		}
		return 2;
	}

	public PhoneLookupResponse lookupByPhone(String phoneRaw) {
		if (phoneRaw == null || phoneRaw.isBlank()) {
			return new PhoneLookupResponse(false, null, null, null, null);
		}
		String phone = inputValidator.cleanPhone(phoneRaw.trim());
		if (phone.isBlank()) {
			return new PhoneLookupResponse(false, null, null, null, null);
		}
		Optional<Client> opt = clientRepository.findFirstByPhoneNumberAndIsDeletedFalse(phone);
		if (opt.isEmpty()) {
			return new PhoneLookupResponse(false, null, null, null, null);
		}
		Client c = opt.get();
		return new PhoneLookupResponse(true, c.getId(), c.getSurname(), c.getName(), c.getPatronymic());
	}

	@Transactional
	public String create(String phoneRaw, Long linkedClientId, String surname, String name, String patronymic,
			Long equipmentId, LocalDateTime dateFrom, LocalDateTime dateTo, LocalDateTime remindAt, String comment,
			User creator) {
		if (phoneRaw == null || phoneRaw.isBlank()) {
			return "phone_required";
		}
		String phone = inputValidator.cleanPhone(phoneRaw.trim());
		if (phone.isBlank()) {
			return "phone_required";
		}

		Client client = null;
		if (linkedClientId != null) {
			Client byId = clientRepository.findByIdAndIsDeletedFalse(linkedClientId).orElse(null);
			if (byId != null && phone.equals(byId.getPhoneNumber())) {
				client = byId;
			}
		}
		if (client == null) {
			client = clientRepository.findFirstByPhoneNumberAndIsDeletedFalse(phone).orElse(null);
		}

		CallbackRequest cb = new CallbackRequest();
		cb.setPhoneNumber(phone);
		cb.setCreatedAt(LocalDateTime.now());
		if (creator != null) {
			cb.setCreator(creator);
			cb.setCreatedBy(creator.getLogin());
		}

		if (client != null) {
			cb.setClient(client);
		} else {
			cb.setSurname(trimToNull(surname, MAX_NAME));
			cb.setName(trimToNull(name, MAX_NAME));
			cb.setPatronymic(trimToNull(patronymic, MAX_NAME));
		}

		if (equipmentId != null) {
			Equipment eq = equipmentRepository.findByIdAndIsDeletedFalse(equipmentId).orElse(null);
			if (eq == null) {
				return "equipment_invalid";
			}
			cb.setEquipment(eq);
		}

		cb.setDateFrom(dateFrom);
		cb.setDateTo(dateTo);
		cb.setRemindAt(remindAt);
		cb.setComment(trimToNull(comment, MAX_LEN));

		if (cb.getDateFrom() != null && cb.getDateTo() != null && !cb.getDateTo().isAfter(cb.getDateFrom())) {
			return "bad_period";
		}

		callbackRequestRepository.save(cb);
		return null;
	}

	@Transactional
	public String delete(Long id, Long version) {
		if (id == null) {
			return "not_found";
		}
		Optional<CallbackRequest> opt = callbackRequestRepository.findById(id);
		if (opt.isEmpty()) {
			return "not_found";
		}
		CallbackRequest cb = opt.get();
		if (version == null || !Objects.equals(cb.getVersion(), version)) {
			return "version_conflict";
		}
		callbackRequestRepository.delete(cb);
		return null;
	}

	private static String trimToNull(String s, int maxLen) {
		if (s == null)
			return null;
		String t = s.trim();
		if (t.isEmpty())
			return null;
		return t.length() > maxLen ? t.substring(0, maxLen) : t;
	}
}

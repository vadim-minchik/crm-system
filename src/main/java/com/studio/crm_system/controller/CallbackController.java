package com.studio.crm_system.controller;

import com.studio.crm_system.dto.PhoneLookupResponse;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.CallbackRequestService;
import com.studio.crm_system.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/callbacks")
public class CallbackController {

	@Autowired
	private CallbackRequestService callbackRequestService;

	@Autowired
	private RentalService rentalService;

	@Autowired
	private UserRepository userRepository;

	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername()
				: principal.toString();
		return userRepository.findByLogin(username).orElse(null);
	}

	@GetMapping
	public String list(@RequestParam(required = false) String view, Model model) {
		User user = getCurrentUser();
		if (user == null)
			return "redirect:/login";
		boolean tomorrowFocus = "tomorrow".equals(view);
		model.addAttribute("username", user.getLogin());
		model.addAttribute("currentUserRole", user.getRole());
		model.addAttribute("tomorrowFocus", tomorrowFocus);
		model.addAttribute("callbacks", callbackRequestService.findAll(tomorrowFocus));
		model.addAttribute("equipmentSelectOptions", rentalService.getEquipmentOptionsForSelect());
		return "html/callbacks";
	}

	@GetMapping(value = "/api/lookup", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public PhoneLookupResponse lookup(@RequestParam("phone") String phone) {
		User user = getCurrentUser();
		if (user == null)
			return new PhoneLookupResponse(false, null, null, null, null);
		return callbackRequestService.lookupByPhone(phone);
	}

	@PostMapping("/add")
	public String add(@RequestParam String phoneNumber,
			@RequestParam(required = false) Long linkedClientId,
			@RequestParam(required = false) String surname,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) String patronymic,
			@RequestParam(required = false) String equipmentId,
			@RequestParam(required = false) String dateFrom,
			@RequestParam(required = false) String dateTo,
			@RequestParam(required = false) String remindAt,
			@RequestParam(required = false) String comment,
			@RequestParam(required = false) String returnView) {
		User user = getCurrentUser();
		if (user == null)
			return "redirect:/login";

		LocalDateTime from = parseDateTime(dateFrom);
		LocalDateTime to = parseDateTime(dateTo);
		LocalDateTime remindAtParsed = parseDateTime(remindAt);

		Long equipmentIdParsed = parseOptionalLong(equipmentId);
		if (equipmentIdParsed == null && equipmentId != null && !equipmentId.isBlank()) {
			return redirectCallbacks("error=equipment_invalid", returnView);
		}

		String err = callbackRequestService.create(phoneNumber, linkedClientId, surname, name, patronymic,
				equipmentIdParsed, from, to, remindAtParsed, comment, user);
		if (err != null) {
			return redirectCallbacks("error=" + err, returnView);
		}
		return redirectCallbacks("success=created", returnView);
	}

	@PostMapping("/delete")
	public String delete(@RequestParam Long id, @RequestParam Long version,
			@RequestParam(required = false) String returnView) {
		User user = getCurrentUser();
		if (user == null)
			return "redirect:/login";
		String err = callbackRequestService.delete(id, version);
		if (err != null)
			return redirectCallbacks("error=" + err, returnView);
		return redirectCallbacks("success=deleted", returnView);
	}

	private static String redirectCallbacks(String query, String returnView) {
		String base = "redirect:/callbacks?" + query;
		if ("tomorrow".equals(returnView)) {
			base += "&view=tomorrow";
		}
		return base;
	}

	private static LocalDateTime parseDateTime(String s) {
		if (s == null || s.isBlank())
			return null;
		try {
			return LocalDateTime.parse(s);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static Long parseOptionalLong(String s) {
		if (s == null || s.isBlank())
			return null;
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}

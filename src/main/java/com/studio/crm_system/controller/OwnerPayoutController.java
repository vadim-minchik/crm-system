package com.studio.crm_system.controller;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.OwnerShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequestMapping("/statistics/owners")
public class OwnerPayoutController {

	@Autowired private OwnerShareService ownerShareService;
	@Autowired private UserRepository userRepository;

	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = (principal instanceof UserDetails)
				? ((UserDetails) principal).getUsername()
				: principal.toString();
		return userRepository.findByLogin(username).orElse(null);
	}

	@PostMapping("/payout")
	public String recordPayout(@RequestParam Long equipmentOwnerId,
	                           @RequestParam String amount,
	                           @RequestParam(required = false) String note,
	                           @RequestParam(required = false) String redirect) {
		User user = getCurrentUser();
		if (user == null)
			return "redirect:/login";
		BigDecimal amt;
		try {
			amt = new BigDecimal(amount.trim().replace(',', '.')).setScale(2, java.math.RoundingMode.HALF_UP);
		} catch (Exception e) {
			return redirectWithError(redirect, "invalid_amount");
		}
		String err = ownerShareService.recordPayout(equipmentOwnerId, amt, note, user);
		if (err != null)
			return redirectWithError(redirect, err);
		return "redirect:" + appendQueryParam(safeRedirect(redirect), "success", "payout_recorded");
	}

	private static String safeRedirect(String redirect) {
		if (redirect == null || redirect.isBlank())
			return "/statistics?tab=owners";
		String r = redirect.trim();
		if (!r.startsWith("/") || r.startsWith("//"))
			return "/statistics?tab=owners";
		return r;
	}

	private static String appendQueryParam(String url, String key, String value) {
		if (url == null || url.isBlank())
			return "?" + key + "=" + value;
		return url.contains("?") ? url + "&" + key + "=" + value : url + "?" + key + "=" + value;
	}

	private String redirectWithError(String redirect, String error) {
		return "redirect:" + appendQueryParam(safeRedirect(redirect), "error", error);
	}
}

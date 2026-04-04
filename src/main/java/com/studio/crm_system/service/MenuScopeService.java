package com.studio.crm_system.service;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.enums.NavSection;
import com.studio.crm_system.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuScopeService {

	private static final List<NavSection> REDIRECT_ORDER = List.of(NavSection.values());

	public EnumSet<NavSection> defaultSectionsForRole(Role role) {
		if (role == null)
			return EnumSet.noneOf(NavSection.class);
		return switch (role) {
			case SUPER_ADMIN, ADMIN -> EnumSet.allOf(NavSection.class);
			case WORKER -> EnumSet.of(NavSection.STAFF, NavSection.CALLBACKS, NavSection.CLIENTS,
					NavSection.INVENTORY, NavSection.RENTALS);
		};
	}

	public Set<NavSection> parseStored(String menuSectionsCsv) {
		if (menuSectionsCsv == null || menuSectionsCsv.isBlank())
			return Set.of();
		Set<NavSection> out = new LinkedHashSet<>();
		for (String part : menuSectionsCsv.split(",")) {
			NavSection s = NavSection.fromCode(part);
			if (s != null)
				out.add(s);
		}
		return out;
	}

	public String toCsv(Set<NavSection> sections) {
		return Arrays.stream(NavSection.values())
				.filter(sections::contains)
				.map(NavSection::getCode)
				.collect(Collectors.joining(","));
	}

	public Set<NavSection> effectiveSections(User user) {
		if (user == null)
			return Set.of();
		Set<NavSection> stored = parseStored(user.getMenuSections());
		if (stored.isEmpty())
			return EnumSet.copyOf(defaultSectionsForRole(user.getRole()));
		return stored;
	}

	public List<GrantedAuthority> menuAuthorities(User user) {
		List<GrantedAuthority> list = new ArrayList<>();
		for (NavSection s : effectiveSections(user)) {
			list.add(new SimpleGrantedAuthority(s.getAuthority()));
		}
		return list;
	}

	public String firstAccessiblePath(User user) {
		Set<NavSection> eff = effectiveSections(user);
		for (NavSection s : REDIRECT_ORDER) {
			if (eff.contains(s))
				return s.getEntryPath();
		}
		return "/login";
	}

	
	public Set<NavSection> resolveGrantedSections(User actor, Role targetRole, String[] requestedCodes) {
		Set<NavSection> actorEff = effectiveSections(actor);
		Set<NavSection> requested = new LinkedHashSet<>();
		if (requestedCodes != null) {
			for (String code : requestedCodes) {
				NavSection s = NavSection.fromCode(code);
				if (s != null)
					requested.add(s);
			}
		}
		requested.retainAll(actorEff);
		Set<NavSection> roleCap = EnumSet.copyOf(defaultSectionsForRole(targetRole));
		requested.retainAll(roleCap);

		if (!requested.isEmpty())
			return requested;

		Set<NavSection> fallback = EnumSet.copyOf(roleCap);
		fallback.retainAll(actorEff);
		if (!fallback.isEmpty())
			return fallback;
		if (actorEff.contains(NavSection.STAFF))
			return EnumSet.of(NavSection.STAFF);
		return EnumSet.copyOf(actorEff);
	}
}

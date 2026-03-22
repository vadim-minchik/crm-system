package com.studio.crm_system.controller;

import com.studio.crm_system.dto.UnitHistoryEntry;
import com.studio.crm_system.dto.InventoryUnitRowDto;
import com.studio.crm_system.entity.Category;
import com.studio.crm_system.entity.Equipment;
import com.studio.crm_system.entity.EquipmentOwner;
import com.studio.crm_system.entity.Point;
import com.studio.crm_system.entity.PreCategory;
import com.studio.crm_system.entity.Booking;
import com.studio.crm_system.entity.Rental;
import com.studio.crm_system.entity.ToolName;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.CategoryRepository;
import com.studio.crm_system.repository.EquipmentRepository;
import com.studio.crm_system.repository.PointRepository;
import com.studio.crm_system.repository.PreCategoryRepository;
import com.studio.crm_system.repository.ToolNameRepository;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.dto.OwnerAccrualLineDto;
import com.studio.crm_system.service.BookingService;
import com.studio.crm_system.service.OwnerShareService;
import com.studio.crm_system.service.RentalService;
import com.studio.crm_system.enums.EquipmentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/inventory")
public class EquipmentController {

	@Autowired private EquipmentRepository equipmentRepository;
	@Autowired private ToolNameRepository toolNameRepository;
	@Autowired private CategoryRepository categoryRepository;
	@Autowired private PreCategoryRepository preCategoryRepository;
	@Autowired private PointRepository pointRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private BookingService bookingService;
	@Autowired private RentalService rentalService;
	@Autowired private OwnerShareService ownerShareService;

	private static final int INVENTORY_UNITS_PAGE_SIZE = 25;
	private static final BigDecimal HUNDRED = new BigDecimal("100");
	private static final BigDecimal OWNERS_SUM_TOLERANCE = new BigDecimal("0.02");
	private static final BigDecimal COEF_08 = new BigDecimal("0.8");
	private static final BigDecimal COEF_06 = new BigDecimal("0.6");

	/** @return null если ок, иначе код ошибки для ?error= */
	private String validateAndBuildOwners(String ownerMode, String singleOwnerName,
	                                      List<String> ownerNames, List<String> ownerPercents,
	                                      List<EquipmentOwner> out) {
		out.clear();
		boolean multiple = ownerMode != null && "multiple".equalsIgnoreCase(ownerMode.trim());
		if (multiple) {
			if (ownerNames == null || ownerPercents == null)
				return "owners_multiple_min";
			if (ownerNames.size() != ownerPercents.size())
				return "owners_mismatch";
			BigDecimal sum = BigDecimal.ZERO;
			int order = 0;
			for (int i = 0; i < ownerNames.size(); i++) {
				String n = ownerNames.get(i) != null ? ownerNames.get(i).trim() : "";
				String pRaw = ownerPercents.get(i) != null ? ownerPercents.get(i).trim().replace(',', '.') : "";
				if (n.isEmpty() && pRaw.isEmpty())
					continue;
				if (n.isEmpty() || pRaw.isEmpty())
					return "owner_name_required";
				BigDecimal p;
				try {
					p = new BigDecimal(pRaw).setScale(2, RoundingMode.HALF_UP);
				} catch (Exception e) {
					return "owners_percent_invalid";
				}
				if (p.compareTo(BigDecimal.ZERO) <= 0 || p.compareTo(HUNDRED) > 0)
					return "owners_percent_invalid";
				sum = sum.add(p);
				EquipmentOwner o = new EquipmentOwner();
				o.setOwnerName(n);
				o.setRentalSharePercent(p);
				o.setSortOrder(order++);
				out.add(o);
			}
			if (out.size() < 2)
				return "owners_multiple_min";
			if (sum.subtract(HUNDRED).abs().compareTo(OWNERS_SUM_TOLERANCE) > 0)
				return "owners_percent_sum";
			return null;
		}
		String n = singleOwnerName != null ? singleOwnerName.trim() : "";
		if (n.isEmpty())
			return "owner_name_required";
		EquipmentOwner o = new EquipmentOwner();
		o.setOwnerName(n);
		o.setRentalSharePercent(HUNDRED);
		o.setSortOrder(0);
		out.add(o);
		return null;
	}

	private void applyOwnersToEquipment(Equipment eq, List<EquipmentOwner> newOwners) {
		eq.getOwners().clear();
		for (int i = 0; i < newOwners.size(); i++) {
			EquipmentOwner o = newOwners.get(i);
			o.setEquipment(eq);
			o.setSortOrder(i);
			eq.getOwners().add(o);
		}
	}

	/** JSON без Jackson — чтобы Eclipse/Maven не зависели от явного classpath для com.fasterxml. */
	private static String jsonEscape(String s) {
		if (s == null)
			return "";
		return s.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r");
	}

	private static String ownersToJson(Equipment e) {
		StringBuilder sb = new StringBuilder("[");
		boolean first = true;
		if (e.getOwners() != null) {
			for (EquipmentOwner o : e.getOwners()) {
				if (!first)
					sb.append(',');
				first = false;
				sb.append("{\"name\":\"").append(jsonEscape(o.getOwnerName())).append("\",\"percent\":");
				if (o.getRentalSharePercent() != null)
					sb.append(o.getRentalSharePercent().stripTrailingZeros().toPlainString());
				else
					sb.append('0');
				sb.append('}');
			}
		}
		sb.append(']');
		return sb.toString();
	}

	private static String ownersSummaryLine(Equipment e) {
		if (e.getOwners() == null || e.getOwners().isEmpty())
			return "—";
		return e.getOwners().stream()
				.map(o -> o.getOwnerName() + " " + o.getRentalSharePercent().stripTrailingZeros().toPlainString() + "%")
				.collect(Collectors.joining(" · "));
	}

	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = (principal instanceof UserDetails)
				? ((UserDetails) principal).getUsername()
				: principal.toString();
		return userRepository.findByLogin(username).orElse(null);
	}

	private void addCommonAttrs(Model model, User user) {
		model.addAttribute("currentUser", user);
		model.addAttribute("username", user.getLogin());
		model.addAttribute("currentUserRole", user.getRole());
	}

	private long countUnitsInPreCategory(PreCategory pc) {
		long total = 0;
		for (Category c : categoryRepository.findByPreCategoryAndIsDeletedFalse(pc)) {
			for (ToolName t : toolNameRepository.findByCategoryAndIsDeletedFalse(c)) {
				total += equipmentRepository.countByToolNameAndIsDeletedFalse(t);
			}
		}
		return total;
	}

	private long countFreeUnitsInPreCategory(PreCategory pc) {
		long free = 0;
		for (Category c : categoryRepository.findByPreCategoryAndIsDeletedFalse(pc)) {
			for (ToolName t : toolNameRepository.findByCategoryAndIsDeletedFalse(c)) {
				free += equipmentRepository.countByToolNameAndStatusAndIsDeletedFalse(t, EquipmentStatus.FREE);
			}
		}
		return free;
	}

	private long countUnitsInCategory(Category cat) {
		long total = 0;
		for (ToolName t : toolNameRepository.findByCategoryAndIsDeletedFalse(cat)) {
			total += equipmentRepository.countByToolNameAndIsDeletedFalse(t);
		}
		return total;
	}

	private long countFreeUnitsInCategory(Category cat) {
		long free = 0;
		for (ToolName t : toolNameRepository.findByCategoryAndIsDeletedFalse(cat)) {
			free += equipmentRepository.countByToolNameAndStatusAndIsDeletedFalse(t, EquipmentStatus.FREE);
		}
		return free;
	}

	private boolean hasAnyBusyInPreCategory(PreCategory pc) {
		for (Category c : categoryRepository.findByPreCategoryAndIsDeletedFalse(pc)) {
			if (hasAnyBusyInCategory(c)) return true;
		}
		return false;
	}

	private boolean hasAnyBusyInCategory(Category cat) {
		for (ToolName t : toolNameRepository.findByCategoryAndIsDeletedFalse(cat)) {
			boolean busy = equipmentRepository.findByToolNameAndIsDeletedFalse(t)
					.stream().anyMatch(e -> e.getStatus() != EquipmentStatus.FREE
							|| bookingService.hasActiveOrFutureBooking(e.getId()));
			if (busy) return true;
		}
		return false;
	}

	/** Проверяет, есть ли занятое оборудование (в прокате или в брони) в категории и во всей поддереве подкатегорий. */
	private boolean hasAnyBusyInCategoryTree(Category cat) {
		if (hasAnyBusyInCategory(cat)) return true;
		for (Category child : categoryRepository.findByParentCategoryAndIsDeletedFalse(cat)) {
			if (hasAnyBusyInCategoryTree(child)) return true;
		}
		return false;
	}

	@GetMapping
	public String showInventory(Model model) {
		User currentUser = getCurrentUser();
		if (currentUser == null) return "redirect:/login";

		List<PreCategory> preCategories = preCategoryRepository.findByIsDeletedFalse();

		Map<Long, Long> categoriesCount = new HashMap<>();
		Map<Long, Long> unitsCount      = new HashMap<>();
		Map<Long, Long> freeCount       = new HashMap<>();

		Set<Long> precategoryIdsLocked = new HashSet<>();
		for (PreCategory pc : preCategories) {
			categoriesCount.put(pc.getId(), categoryRepository.countByPreCategoryAndParentCategoryIsNullAndIsDeletedFalse(pc));
			unitsCount.put(pc.getId(), countUnitsInPreCategory(pc));
			freeCount.put(pc.getId(), countFreeUnitsInPreCategory(pc));
			if (hasAnyBusyInPreCategory(pc)) precategoryIdsLocked.add(pc.getId());
		}

		model.addAttribute("preCategories", preCategories);
		model.addAttribute("precategoryIdsLocked", precategoryIdsLocked);
		model.addAttribute("categoriesCount", categoriesCount);
		model.addAttribute("unitsCount", unitsCount);
		model.addAttribute("freeCount", freeCount);
		addCommonAttrs(model, currentUser);
		return "html/inventory";
	}

	@PostMapping("/precategory/add")
	public String addPreCategory(@RequestParam String name,
	                              @RequestParam(required = false) String description) {
		if (name == null || name.trim().isEmpty())
			return "redirect:/inventory?error=name_required";
		if (preCategoryRepository.existsByNameIgnoreCaseAndIsDeletedFalse(name.trim()))
			return "redirect:/inventory?error=name_exists";

		PreCategory pc = new PreCategory();
		pc.setName(name.trim());
		pc.setDescription(description != null ? description.trim() : null);
		preCategoryRepository.save(pc);
		return "redirect:/inventory?success=precategory_added";
	}

	@PostMapping("/precategory/edit")
	public String editPreCategory(@RequestParam Long id,
	                               @RequestParam String name,
	                               @RequestParam(required = false) String description) {
		PreCategory pc = preCategoryRepository.findById(id).orElse(null);
		if (pc == null) return "redirect:/inventory?error=not_found";
		if (name == null || name.trim().isEmpty())
			return "redirect:/inventory?error=name_required";
		pc.setName(name.trim());
		pc.setDescription(description != null ? description.trim() : null);
		preCategoryRepository.save(pc);
		return "redirect:/inventory?success=precategory_updated";
	}

	@PostMapping("/precategory/delete")
	public String deletePreCategory(@RequestParam Long id) {
		PreCategory pc = preCategoryRepository.findById(id).orElse(null);
		if (pc == null) return "redirect:/inventory?success=precategory_deleted";

		if (hasAnyBusyInPreCategory(pc))
			return "redirect:/inventory?error=in_use";

		for (Category c : categoryRepository.findByPreCategoryAndIsDeletedFalse(pc)) {
			for (ToolName t : toolNameRepository.findByCategoryAndIsDeletedFalse(c)) {
				equipmentRepository.findByToolNameAndIsDeletedFalse(t)
						.forEach(e -> { e.setIsDeleted(true); equipmentRepository.save(e); });
				t.setIsDeleted(true);
				toolNameRepository.save(t);
			}
			c.setIsDeleted(true);
			categoryRepository.save(c);
		}
		pc.setIsDeleted(true);
		preCategoryRepository.save(pc);
		return "redirect:/inventory?success=precategory_deleted";
	}

	@GetMapping("/precategory/{preCategoryId}")
	public String showCategories(@PathVariable Long preCategoryId, Model model) {
		User currentUser = getCurrentUser();
		if (currentUser == null) return "redirect:/login";

		PreCategory pc = preCategoryRepository.findById(preCategoryId).orElse(null);
		if (pc == null || pc.getIsDeleted())
			return "redirect:/inventory?error=not_found";

		List<Category> categories = categoryRepository.findByPreCategoryAndParentCategoryIsNullAndIsDeletedFalse(pc);

		Map<Long, Long> modelsCount = new HashMap<>();
		Map<Long, Long> unitsCount  = new HashMap<>();
		Map<Long, Long> freeCount   = new HashMap<>();

		Set<Long> categoryIdsLocked = new HashSet<>();
		for (Category c : categories) {
			modelsCount.put(c.getId(), toolNameRepository.countByCategoryAndIsDeletedFalse(c));
			unitsCount.put(c.getId(), countUnitsInCategory(c));
			freeCount.put(c.getId(), countFreeUnitsInCategory(c));
			if (hasAnyBusyInCategoryTree(c)) categoryIdsLocked.add(c.getId());
		}

		model.addAttribute("preCategory", pc);
		model.addAttribute("categories", categories);
		model.addAttribute("modelsCount", modelsCount);
		model.addAttribute("unitsCount", unitsCount);
		model.addAttribute("freeCount", freeCount);
		model.addAttribute("categoryIdsLocked", categoryIdsLocked);
		addCommonAttrs(model, currentUser);
		return "html/inventory_precategory";
	}

	@PostMapping("/precategory/{preCategoryId}/category/add")
	public String addCategory(@PathVariable Long preCategoryId,
	                          @RequestParam String name,
	                          @RequestParam(required = false) String description,
	                          @RequestParam(required = false) Long parentCategoryId) {
		PreCategory pc = preCategoryRepository.findById(preCategoryId).orElse(null);
		if (pc == null) return "redirect:/inventory?error=not_found";

		if (name == null || name.trim().isEmpty()) {
			String redirect = parentCategoryId != null
				? "/inventory/category/" + parentCategoryId
				: "/inventory/precategory/" + preCategoryId;
			return "redirect:" + redirect + "?error=name_required";
		}

		Category cat = new Category();
		cat.setName(name.trim());
		cat.setDescription(description != null ? description.trim() : null);
		cat.setPreCategory(pc);
		if (parentCategoryId != null) {
			Category parent = categoryRepository.findById(parentCategoryId).orElse(null);
			if (parent != null && !parent.getIsDeleted()) {
				if (categoryRepository.existsByNameIgnoreCaseAndParentCategoryAndIsDeletedFalse(name.trim(), parent))
					return "redirect:/inventory/category/" + parentCategoryId + "?error=name_exists";
				cat.setParentCategory(parent);
			}
		} else {
			if (categoryRepository.existsByNameIgnoreCaseAndPreCategoryAndParentCategoryIsNullAndIsDeletedFalse(name.trim(), pc))
				return "redirect:/inventory/precategory/" + preCategoryId + "?error=name_exists";
		}
		categoryRepository.save(cat);
		if (parentCategoryId != null)
			return "redirect:/inventory/category/" + parentCategoryId + "?success=category_added";
		return "redirect:/inventory/precategory/" + preCategoryId + "?success=category_added";
	}

	@PostMapping("/category/edit")
	public String editCategory(@RequestParam Long id,
	                           @RequestParam String name,
	                           @RequestParam(required = false) String description) {
		Category cat = categoryRepository.findById(id).orElse(null);
		if (cat == null) return "redirect:/inventory?error=not_found";
		if (name == null || name.trim().isEmpty())
			return "redirect:/inventory/category/" + id + "?error=name_required";

		cat.setName(name.trim());
		cat.setDescription(description != null ? description.trim() : null);
		categoryRepository.save(cat);
		return "redirect:/inventory/category/" + id + "?success=category_updated";
	}

	@PostMapping("/category/delete")
	public String deleteCategory(@RequestParam Long id) {
		Category cat = categoryRepository.findById(id).orElse(null);
		if (cat == null) return "redirect:/inventory?success=category_deleted";

		Long pcId = cat.getPreCategory().getId();
		if (categoryRepository.countByParentCategoryAndIsDeletedFalse(cat) > 0)
			return "redirect:/inventory/category/" + id + "?error=has_children";
		if (hasAnyBusyInCategoryTree(cat))
			return "redirect:/inventory/category/" + id + "?error=in_use";

		String redirectBack = cat.getParentCategory() != null
			? "/inventory/category/" + cat.getParentCategory().getId()
			: "/inventory/precategory/" + pcId;

		for (ToolName t : toolNameRepository.findByCategoryAndIsDeletedFalse(cat)) {
			equipmentRepository.findByToolNameAndIsDeletedFalse(t)
					.forEach(e -> { e.setIsDeleted(true); equipmentRepository.save(e); });
			t.setIsDeleted(true);
			toolNameRepository.save(t);
		}
		cat.setIsDeleted(true);
		categoryRepository.save(cat);
		return "redirect:" + redirectBack + "?success=category_deleted";
	}

	@GetMapping("/category/{categoryId}")
	public String showModels(@PathVariable Long categoryId, Model model) {
		User currentUser = getCurrentUser();
		if (currentUser == null) return "redirect:/login";

		Category category = categoryRepository.findById(categoryId).orElse(null);
		if (category == null || category.getIsDeleted())
			return "redirect:/inventory?error=not_found";

		List<Category> subcategories = categoryRepository.findByParentCategoryAndIsDeletedFalse(category);
		List<ToolName> toolNames = toolNameRepository.findByCategoryAndIsDeletedFalse(category);

		Map<Long, Long> subcategoriesModelsCount = new HashMap<>();
		Map<Long, Long> subcategoriesUnitsCount  = new HashMap<>();
		Map<Long, Long> subcategoriesFreeCount   = new HashMap<>();
		Set<Long> subcategoryIdsLocked = new HashSet<>();
		for (Category sub : subcategories) {
			subcategoriesModelsCount.put(sub.getId(), toolNameRepository.countByCategoryAndIsDeletedFalse(sub));
			subcategoriesUnitsCount.put(sub.getId(), countUnitsInCategory(sub));
			subcategoriesFreeCount.put(sub.getId(), countFreeUnitsInCategory(sub));
			if (hasAnyBusyInCategoryTree(sub)) subcategoryIdsLocked.add(sub.getId());
		}

		Map<Long, Long> totalCount = new HashMap<>();
		Map<Long, Long> freeCount  = new HashMap<>();
		Set<Long> modelIdsLocked = new HashSet<>();
		for (ToolName t : toolNames) {
			totalCount.put(t.getId(), equipmentRepository.countByToolNameAndIsDeletedFalse(t));
			freeCount.put(t.getId(), equipmentRepository.countByToolNameAndStatusAndIsDeletedFalse(t, EquipmentStatus.FREE));
			List<Equipment> units = equipmentRepository.findByToolNameAndIsDeletedFalse(t);
			boolean locked = units.stream().anyMatch(e -> e.getStatus() != EquipmentStatus.FREE)
					|| units.stream().anyMatch(e -> bookingService.hasActiveOrFutureBooking(e.getId()));
			if (locked) modelIdsLocked.add(t.getId());
		}

		List<Category> breadcrumb = new ArrayList<>();
		for (Category c = category; c != null; c = c.getParentCategory())
			breadcrumb.add(0, c);

		model.addAttribute("preCategory", category.getPreCategory());
		model.addAttribute("category", category);
		model.addAttribute("breadcrumb", breadcrumb);
		model.addAttribute("subcategories", subcategories);
		model.addAttribute("subcategoriesModelsCount", subcategoriesModelsCount);
		model.addAttribute("subcategoriesUnitsCount", subcategoriesUnitsCount);
		model.addAttribute("subcategoriesFreeCount", subcategoriesFreeCount);
		model.addAttribute("subcategoryIdsLocked", subcategoryIdsLocked);
		model.addAttribute("toolNames", toolNames);
		model.addAttribute("totalCount", totalCount);
		model.addAttribute("freeCount", freeCount);
		model.addAttribute("modelIdsLocked", modelIdsLocked);
		addCommonAttrs(model, currentUser);
		return "html/inventory_models";
	}

	@PostMapping("/category/{categoryId}/model/add")
	public String addModel(@PathVariable Long categoryId,
	                       @RequestParam String name,
	                       @RequestParam(required = false) String description) {
		Category category = categoryRepository.findById(categoryId).orElse(null);
		if (category == null) return "redirect:/inventory?error=not_found";

		if (name == null || name.trim().isEmpty())
			return "redirect:/inventory/category/" + categoryId + "?error=name_required";
		if (toolNameRepository.existsByNameIgnoreCaseAndCategoryAndIsDeletedFalse(name.trim(), category))
			return "redirect:/inventory/category/" + categoryId + "?error=name_exists";

		ToolName toolName = new ToolName();
		toolName.setName(name.trim());
		toolName.setDescription(description != null ? description.trim() : null);
		toolName.setCategory(category);
		toolNameRepository.save(toolName);
		return "redirect:/inventory/category/" + categoryId + "?success=model_added";
	}

	@PostMapping("/model/edit")
	public String editModel(@RequestParam Long id,
	                        @RequestParam String name,
	                        @RequestParam(required = false) String description) {
		ToolName toolName = toolNameRepository.findById(id).orElse(null);
		if (toolName == null) return "redirect:/inventory?error=not_found";

		Long categoryId = toolName.getCategory().getId();
		if (name == null || name.trim().isEmpty())
			return "redirect:/inventory/category/" + categoryId + "?error=name_required";

		toolName.setName(name.trim());
		toolName.setDescription(description != null ? description.trim() : null);
		toolNameRepository.save(toolName);
		return "redirect:/inventory/category/" + categoryId + "?success=model_updated";
	}

	@PostMapping("/model/delete")
	public String deleteModel(@RequestParam Long id) {
		ToolName toolName = toolNameRepository.findById(id).orElse(null);
		if (toolName == null) return "redirect:/inventory?error=not_found";

		Long categoryId = toolName.getCategory().getId();
		List<Equipment> modelUnits = equipmentRepository.findByToolNameAndIsDeletedFalse(toolName);
		boolean busy = modelUnits.stream().anyMatch(e -> e.getStatus() != EquipmentStatus.FREE)
				|| modelUnits.stream().anyMatch(e -> bookingService.hasActiveOrFutureBooking(e.getId()));
		if (busy)
			return "redirect:/inventory/category/" + categoryId + "?error=in_use";

		equipmentRepository.findByToolNameAndIsDeletedFalse(toolName)
				.forEach(e -> { e.setIsDeleted(true); equipmentRepository.save(e); });
		toolName.setIsDeleted(true);
		toolNameRepository.save(toolName);
		return "redirect:/inventory/category/" + categoryId + "?success=model_deleted";
	}

	@GetMapping("/unit/{id}")
	public String unitInfoAndHistory(@PathVariable Long id, Model model) {
		User currentUser = getCurrentUser();
		if (currentUser == null) return "redirect:/login";

		Equipment unit = equipmentRepository.findByIdAndIsDeletedFalse(id).orElse(null);
		if (unit == null) return "redirect:/inventory?error=not_found";

		ToolName toolName = unit.getToolName();
		Category category = toolName != null ? toolName.getCategory() : null;
		PreCategory preCategory = category != null ? category.getPreCategory() : null;

		LocalDateTime now = LocalDateTime.now();
		List<UnitHistoryEntry> history = new ArrayList<>();
		for (Rental r : rentalService.findRentalsByEquipmentId(id)) {
			history.add(new UnitHistoryEntry(r, now));
		}
		for (Booking b : bookingService.findBookingsByEquipmentId(id)) {
			history.add(new UnitHistoryEntry(b, now));
		}
		List<UnitHistoryEntry> sortedHistory = history.stream()
				.sorted(Comparator.comparing(UnitHistoryEntry::isActive).reversed()
						.thenComparing(UnitHistoryEntry::getSortDate, Comparator.nullsLast(Comparator.reverseOrder())))
				.collect(Collectors.toList());

		model.addAttribute("unit", unit);
		model.addAttribute("toolName", toolName);
		model.addAttribute("category", category);
		model.addAttribute("preCategory", preCategory);
		model.addAttribute("history", sortedHistory);
		model.addAttribute("ownerBalanceRows", ownerShareService.buildOwnerBalanceRowsForEquipment(id));
		model.addAttribute("equipmentPayouts", ownerShareService.payoutsForEquipment(id));
		Map<Long, List<OwnerAccrualLineDto>> accrualByOwner = new LinkedHashMap<>();
		for (EquipmentOwner o : unit.getOwners()) {
			accrualByOwner.put(o.getId(), ownerShareService.accrualLinesForOwner(o));
		}
		model.addAttribute("ownerAccrualLinesByOwnerId", accrualByOwner);
		addCommonAttrs(model, currentUser);
		return "html/unit_history";
	}

	@GetMapping("/{toolNameId}")
	public String showUnits(@PathVariable Long toolNameId, Model model) {
		User currentUser = getCurrentUser();
		if (currentUser == null) return "redirect:/login";

		ToolName toolName = toolNameRepository.findById(toolNameId).orElse(null);
		if (toolName == null || toolName.getIsDeleted())
			return "redirect:/inventory?error=not_found";

		Category category = toolName.getCategory();
		Pageable pageable = PageRequest.of(0, INVENTORY_UNITS_PAGE_SIZE, Sort.by("id"));
		Page<Equipment> unitsPage = equipmentRepository.findByToolNameAndIsDeletedFalse(toolName, pageable);
		List<Equipment> units = unitsPage.getContent();
		Set<Long> equipmentIdsWithBooking = new HashSet<>();
		for (Equipment u : units) {
			if (bookingService.hasActiveOrFutureBooking(u.getId())) equipmentIdsWithBooking.add(u.getId());
		}
		long totalUnits = unitsPage.getTotalElements();
		boolean hasMoreUnits = unitsPage.hasNext();

		model.addAttribute("preCategory", category.getPreCategory());
		model.addAttribute("category", category);
		model.addAttribute("toolName", toolName);
		model.addAttribute("units", units);
		Map<Long, String> ownersJsonByUnitId = new HashMap<>();
		Map<Long, String> ownersSummaryByUnitId = new HashMap<>();
		for (Equipment u : units) {
			ownersJsonByUnitId.put(u.getId(), ownersToJson(u));
			ownersSummaryByUnitId.put(u.getId(), ownersSummaryLine(u));
		}
		model.addAttribute("ownersJsonByUnitId", ownersJsonByUnitId);
		model.addAttribute("ownersSummaryByUnitId", ownersSummaryByUnitId);
		model.addAttribute("equipmentIdsWithBooking", equipmentIdsWithBooking);
		model.addAttribute("totalUnits", totalUnits);
		model.addAttribute("hasMoreUnits", hasMoreUnits);
		model.addAttribute("toolNameId", toolNameId);
		List<Point> points = pointRepository.findByIsDeletedFalseOrderByNameAsc();
		model.addAttribute("points", points);
		if (!points.isEmpty()) {
			model.addAttribute("firstPointId", points.get(0).getId());
		}
		addCommonAttrs(model, currentUser);
		return "html/inventory_detail";
	}

	/**
	 * Подгрузка следующей страницы экземпляров для бесконечного скролла (JSON).
	 */
	@GetMapping(value = "/{toolNameId}/units/page", produces = "application/json")
	@ResponseBody
	@Transactional(readOnly = true)
	public Map<String, Object> getUnitsPage(@PathVariable Long toolNameId,
	                                        @RequestParam(defaultValue = "0") int page,
	                                        @RequestParam(defaultValue = "25") int size) {
		Map<String, Object> result = new HashMap<>();
		ToolName toolName = toolNameRepository.findById(toolNameId).orElse(null);
		if (toolName == null || toolName.getIsDeleted()) {
			result.put("content", List.of());
			result.put("totalElements", 0L);
			result.put("hasNext", false);
			return result;
		}
		Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("id"));
		Page<Equipment> unitsPage = equipmentRepository.findByToolNameAndIsDeletedFalse(toolName, pageable);
		List<InventoryUnitRowDto> rows = new ArrayList<>();
		for (Equipment u : unitsPage.getContent()) {
			InventoryUnitRowDto dto = new InventoryUnitRowDto();
			dto.setId(u.getId());
			dto.setSerialNumber(u.getSerialNumber());
			dto.setCondition(u.getCondition());
			dto.setPriceFirstDay(u.getPriceFirstDay());
			dto.setPriceSecondDay(u.getPriceSecondDay());
			dto.setPriceSubsequentDays(u.getPriceSubsequentDays());
			dto.setPriceFirstMonth(u.getPriceFirstMonth());
			dto.setPriceSecondMonth(u.getPriceSecondMonth());
			dto.setPriceSubsequentMonths(u.getPriceSubsequentMonths());
			dto.setBaseValue(u.getBaseValue());
			dto.setStatus(u.getStatus().name());
			dto.setStatusLabel(u.getStatus() == EquipmentStatus.FREE ? "Свободно"
				: (u.getStatus() == EquipmentStatus.BUSY ? "Занят" : "Забронирован"));
			dto.setInBooking(bookingService.hasActiveOrFutureBooking(u.getId()));
			dto.setPointId(u.getPoint() != null ? u.getPoint().getId() : null);
			dto.setPointName(u.getPoint() != null ? u.getPoint().getName() : null);
			dto.setOwnersSummary(ownersSummaryLine(u));
			dto.setOwnersJson(ownersToJson(u));
			rows.add(dto);
		}
		result.put("content", rows);
		result.put("totalElements", unitsPage.getTotalElements());
		result.put("hasNext", unitsPage.hasNext());
		return result;
	}

	private void applyPriceFormula(Equipment eq, BigDecimal firstDay, BigDecimal firstMonth,
	                               BigDecimal secondDay, BigDecimal subsequentDays,
	                               BigDecimal secondMonth, BigDecimal subsequentMonths) {
		eq.setPriceFirstDay(firstDay);
		eq.setPriceSecondDay(secondDay != null && secondDay.compareTo(BigDecimal.ZERO) > 0
				? secondDay : firstDay.multiply(COEF_08).setScale(2, RoundingMode.HALF_UP));
		eq.setPriceSubsequentDays(subsequentDays != null && subsequentDays.compareTo(BigDecimal.ZERO) > 0
				? subsequentDays : firstDay.multiply(COEF_06).setScale(2, RoundingMode.HALF_UP));
		eq.setPriceFirstMonth(firstMonth);
		eq.setPriceSecondMonth(secondMonth != null && secondMonth.compareTo(BigDecimal.ZERO) > 0
				? secondMonth : firstMonth.multiply(COEF_08).setScale(2, RoundingMode.HALF_UP));
		eq.setPriceSubsequentMonths(subsequentMonths != null && subsequentMonths.compareTo(BigDecimal.ZERO) > 0
				? subsequentMonths : firstMonth.multiply(COEF_06).setScale(2, RoundingMode.HALF_UP));
	}

	@PostMapping("/{toolNameId}/add")
	public String addUnit(@PathVariable Long toolNameId,
	                      @RequestParam(value = "pointId", required = false) Long pointId,
	                      @RequestParam String serialNumber,
	                      @RequestParam BigDecimal priceFirstDay,
	                      @RequestParam BigDecimal priceFirstMonth,
	                      @RequestParam(required = false) BigDecimal priceSecondDay,
	                      @RequestParam(required = false) BigDecimal priceSubsequentDays,
	                      @RequestParam(required = false) BigDecimal priceSecondMonth,
	                      @RequestParam(required = false) BigDecimal priceSubsequentMonths,
	                      @RequestParam BigDecimal baseValue,
	                      @RequestParam(required = false) Integer condition,
	                      @RequestParam(required = false, defaultValue = "single") String ownerMode,
	                      @RequestParam(required = false) String singleOwnerName,
	                      @RequestParam(required = false) List<String> ownerNames,
	                      @RequestParam(required = false) List<String> ownerPercents) {

		ToolName toolName = toolNameRepository.findById(toolNameId).orElse(null);
		if (toolName == null) return "redirect:/inventory?error=not_found";

		if (pointId == null)
			return "redirect:/inventory/" + toolNameId + "?error=point_required";
		Point point = pointRepository.findById(pointId).orElse(null);
		if (point == null || Boolean.TRUE.equals(point.getIsDeleted()))
			return "redirect:/inventory/" + toolNameId + "?error=point_invalid";

		if (serialNumber == null || serialNumber.trim().isEmpty())
			return "redirect:/inventory/" + toolNameId + "?error=serial_required";
		// Уникальность только среди неудалённых — удалённые не мешают завести такой же серийник снова
		if (equipmentRepository.existsBySerialNumberAndIsDeletedFalse(serialNumber.trim()))
			return "redirect:/inventory/" + toolNameId + "?error=serial_exists";
		if (priceFirstDay == null || priceFirstDay.compareTo(BigDecimal.ZERO) <= 0)
			return "redirect:/inventory/" + toolNameId + "?error=price_required";
		if (priceFirstMonth == null || priceFirstMonth.compareTo(BigDecimal.ZERO) <= 0)
			return "redirect:/inventory/" + toolNameId + "?error=price_required";
		if (baseValue == null || baseValue.compareTo(BigDecimal.ZERO) <= 0)
			return "redirect:/inventory/" + toolNameId + "?error=base_value_required";

		List<EquipmentOwner> builtOwners = new ArrayList<>();
		String ownerErr = validateAndBuildOwners(ownerMode, singleOwnerName, ownerNames, ownerPercents, builtOwners);
		if (ownerErr != null)
			return "redirect:/inventory/" + toolNameId + "?error=" + ownerErr;

		Equipment eq = new Equipment();
		eq.setToolName(toolName);
		eq.setPoint(point);
		eq.setSerialNumber(serialNumber.trim());
		eq.setBaseValue(baseValue);
		eq.setCondition(condition != null ? condition : 10);
		eq.setStatus(EquipmentStatus.FREE);
		applyPriceFormula(eq, priceFirstDay, priceFirstMonth, priceSecondDay, priceSubsequentDays, priceSecondMonth, priceSubsequentMonths);
		applyOwnersToEquipment(eq, builtOwners);

		try {
			equipmentRepository.save(eq);
		} catch (Exception e) {
			return "redirect:/inventory/" + toolNameId + "?error=save_failed";
		}
		return "redirect:/inventory/" + toolNameId + "?success=unit_added";
	}

	@PostMapping("/edit")
	public String editUnit(@RequestParam Long id,
	                       @RequestParam(value = "pointId", required = false) Long pointId,
	                       @RequestParam String serialNumber,
	                       @RequestParam BigDecimal priceFirstDay,
	                       @RequestParam BigDecimal priceFirstMonth,
	                       @RequestParam(required = false) BigDecimal priceSecondDay,
	                       @RequestParam(required = false) BigDecimal priceSubsequentDays,
	                       @RequestParam(required = false) BigDecimal priceSecondMonth,
	                       @RequestParam(required = false) BigDecimal priceSubsequentMonths,
	                       @RequestParam BigDecimal baseValue,
	                       @RequestParam(required = false) Integer condition,
	                       @RequestParam(required = false, defaultValue = "single") String ownerMode,
	                       @RequestParam(required = false) String singleOwnerName,
	                       @RequestParam(required = false) List<String> ownerNames,
	                       @RequestParam(required = false) List<String> ownerPercents) {

		Equipment eq = equipmentRepository.findByIdAndIsDeletedFalse(id).orElse(null);
		if (eq == null) return "redirect:/inventory?error=not_found";

		if (pointId == null)
			return "redirect:/inventory/" + eq.getToolName().getId() + "?error=point_required";
		Point point = pointRepository.findById(pointId).orElse(null);
		if (point == null || Boolean.TRUE.equals(point.getIsDeleted()))
			return "redirect:/inventory/" + eq.getToolName().getId() + "?error=point_invalid";

		Long toolNameId = eq.getToolName().getId();
		// Уникальность только среди неудалённых; при редактировании — кроме текущего юнита
		if (equipmentRepository.existsBySerialNumberAndIsDeletedFalseAndIdNot(serialNumber.trim(), eq.getId()))
			return "redirect:/inventory/" + toolNameId + "?error=serial_exists";
		List<EquipmentOwner> builtOwners = new ArrayList<>();
		String ownerErr = validateAndBuildOwners(ownerMode, singleOwnerName, ownerNames, ownerPercents, builtOwners);
		if (ownerErr != null)
			return "redirect:/inventory/" + toolNameId + "?error=" + ownerErr;

		eq.setSerialNumber(serialNumber.trim());
		eq.setPoint(point);
		eq.setBaseValue(baseValue);
		eq.setCondition(condition != null ? condition : eq.getCondition());
		applyPriceFormula(eq, priceFirstDay, priceFirstMonth, priceSecondDay, priceSubsequentDays, priceSecondMonth, priceSubsequentMonths);
		applyOwnersToEquipment(eq, builtOwners);

		try {
			equipmentRepository.save(eq);
		} catch (Exception e) {
			return "redirect:/inventory/" + toolNameId + "?error=save_failed";
		}
		return "redirect:/inventory/" + toolNameId + "?success=unit_updated";
	}

	@PostMapping("/delete")
	public String deleteUnit(@RequestParam Long id) {
		Equipment eq = equipmentRepository.findByIdAndIsDeletedFalse(id).orElse(null);
		if (eq == null) return "redirect:/inventory?error=not_found";

		Long toolNameId = eq.getToolName().getId();
		if (eq.getStatus() != EquipmentStatus.FREE)
			return "redirect:/inventory/" + toolNameId + "?error=unit_in_use";
		if (bookingService.hasActiveOrFutureBooking(eq.getId()))
			return "redirect:/inventory/" + toolNameId + "?error=unit_in_use";

		eq.setIsDeleted(true);
		equipmentRepository.save(eq);
		return "redirect:/inventory/" + toolNameId + "?success=unit_deleted";
	}
}

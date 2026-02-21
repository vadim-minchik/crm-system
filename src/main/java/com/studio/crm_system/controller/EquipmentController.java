package com.studio.crm_system.controller;

import com.studio.crm_system.entity.Category;
import com.studio.crm_system.entity.Equipment;
import com.studio.crm_system.entity.PreCategory;
import com.studio.crm_system.entity.ToolName;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.CategoryRepository;
import com.studio.crm_system.repository.EquipmentRepository;
import com.studio.crm_system.repository.PreCategoryRepository;
import com.studio.crm_system.repository.ToolNameRepository;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.enums.EquipmentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/inventory")
public class EquipmentController {

	@Autowired private EquipmentRepository equipmentRepository;
	@Autowired private ToolNameRepository toolNameRepository;
	@Autowired private CategoryRepository categoryRepository;
	@Autowired private PreCategoryRepository preCategoryRepository;
	@Autowired private UserRepository userRepository;

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
					.stream().anyMatch(e -> e.getStatus() != EquipmentStatus.FREE);
			if (busy) return true;
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

		for (PreCategory pc : preCategories) {
			categoriesCount.put(pc.getId(), categoryRepository.countByPreCategoryAndIsDeletedFalse(pc));
			unitsCount.put(pc.getId(), countUnitsInPreCategory(pc));
			freeCount.put(pc.getId(), countFreeUnitsInPreCategory(pc));
		}

		model.addAttribute("preCategories", preCategories);
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

		List<Category> categories = categoryRepository.findByPreCategoryAndIsDeletedFalse(pc);

		Map<Long, Long> modelsCount = new HashMap<>();
		Map<Long, Long> unitsCount  = new HashMap<>();
		Map<Long, Long> freeCount   = new HashMap<>();

		for (Category c : categories) {
			modelsCount.put(c.getId(), toolNameRepository.countByCategoryAndIsDeletedFalse(c));
			unitsCount.put(c.getId(), countUnitsInCategory(c));
			freeCount.put(c.getId(), countFreeUnitsInCategory(c));
		}

		model.addAttribute("preCategory", pc);
		model.addAttribute("categories", categories);
		model.addAttribute("modelsCount", modelsCount);
		model.addAttribute("unitsCount", unitsCount);
		model.addAttribute("freeCount", freeCount);
		addCommonAttrs(model, currentUser);
		return "html/inventory_precategory";
	}

	@PostMapping("/precategory/{preCategoryId}/category/add")
	public String addCategory(@PathVariable Long preCategoryId,
	                          @RequestParam String name,
	                          @RequestParam(required = false) String description) {
		PreCategory pc = preCategoryRepository.findById(preCategoryId).orElse(null);
		if (pc == null) return "redirect:/inventory?error=not_found";

		if (name == null || name.trim().isEmpty())
			return "redirect:/inventory/precategory/" + preCategoryId + "?error=name_required";
		if (categoryRepository.existsByNameIgnoreCaseAndPreCategoryAndIsDeletedFalse(name.trim(), pc))
			return "redirect:/inventory/precategory/" + preCategoryId + "?error=name_exists";

		Category cat = new Category();
		cat.setName(name.trim());
		cat.setDescription(description != null ? description.trim() : null);
		cat.setPreCategory(pc);
		categoryRepository.save(cat);
		return "redirect:/inventory/precategory/" + preCategoryId + "?success=category_added";
	}

	@PostMapping("/category/edit")
	public String editCategory(@RequestParam Long id,
	                           @RequestParam String name,
	                           @RequestParam(required = false) String description) {
		Category cat = categoryRepository.findById(id).orElse(null);
		if (cat == null) return "redirect:/inventory?error=not_found";
		if (name == null || name.trim().isEmpty())
			return "redirect:/inventory/precategory/" + cat.getPreCategory().getId() + "?error=name_required";

		cat.setName(name.trim());
		cat.setDescription(description != null ? description.trim() : null);
		categoryRepository.save(cat);
		return "redirect:/inventory/precategory/" + cat.getPreCategory().getId() + "?success=category_updated";
	}

	@PostMapping("/category/delete")
	public String deleteCategory(@RequestParam Long id) {
		Category cat = categoryRepository.findById(id).orElse(null);
		if (cat == null) return "redirect:/inventory?success=category_deleted";

		Long pcId = cat.getPreCategory().getId();
		if (hasAnyBusyInCategory(cat))
			return "redirect:/inventory/precategory/" + pcId + "?error=in_use";

		for (ToolName t : toolNameRepository.findByCategoryAndIsDeletedFalse(cat)) {
			equipmentRepository.findByToolNameAndIsDeletedFalse(t)
					.forEach(e -> { e.setIsDeleted(true); equipmentRepository.save(e); });
			t.setIsDeleted(true);
			toolNameRepository.save(t);
		}
		cat.setIsDeleted(true);
		categoryRepository.save(cat);
		return "redirect:/inventory/precategory/" + pcId + "?success=category_deleted";
	}

	@GetMapping("/category/{categoryId}")
	public String showModels(@PathVariable Long categoryId, Model model) {
		User currentUser = getCurrentUser();
		if (currentUser == null) return "redirect:/login";

		Category category = categoryRepository.findById(categoryId).orElse(null);
		if (category == null || category.getIsDeleted())
			return "redirect:/inventory?error=not_found";

		List<ToolName> toolNames = toolNameRepository.findByCategoryAndIsDeletedFalse(category);

		Map<Long, Long> totalCount = new HashMap<>();
		Map<Long, Long> freeCount  = new HashMap<>();
		for (ToolName t : toolNames) {
			totalCount.put(t.getId(), equipmentRepository.countByToolNameAndIsDeletedFalse(t));
			freeCount.put(t.getId(), equipmentRepository.countByToolNameAndStatusAndIsDeletedFalse(t, EquipmentStatus.FREE));
		}

		model.addAttribute("preCategory", category.getPreCategory());
		model.addAttribute("category", category);
		model.addAttribute("toolNames", toolNames);
		model.addAttribute("totalCount", totalCount);
		model.addAttribute("freeCount", freeCount);
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
		boolean busy = equipmentRepository.findByToolNameAndIsDeletedFalse(toolName)
				.stream().anyMatch(e -> e.getStatus() != EquipmentStatus.FREE);
		if (busy)
			return "redirect:/inventory/category/" + categoryId + "?error=in_use";

		equipmentRepository.findByToolNameAndIsDeletedFalse(toolName)
				.forEach(e -> { e.setIsDeleted(true); equipmentRepository.save(e); });
		toolName.setIsDeleted(true);
		toolNameRepository.save(toolName);
		return "redirect:/inventory/category/" + categoryId + "?success=model_deleted";
	}

	@GetMapping("/{toolNameId}")
	public String showUnits(@PathVariable Long toolNameId, Model model) {
		User currentUser = getCurrentUser();
		if (currentUser == null) return "redirect:/login";

		ToolName toolName = toolNameRepository.findById(toolNameId).orElse(null);
		if (toolName == null || toolName.getIsDeleted())
			return "redirect:/inventory?error=not_found";

		Category category = toolName.getCategory();
		List<Equipment> units = equipmentRepository.findByToolNameAndIsDeletedFalse(toolName);

		model.addAttribute("preCategory", category.getPreCategory());
		model.addAttribute("category", category);
		model.addAttribute("toolName", toolName);
		model.addAttribute("units", units);
		addCommonAttrs(model, currentUser);
		return "html/inventory_detail";
	}

	@PostMapping("/{toolNameId}/add")
	public String addUnit(@PathVariable Long toolNameId,
	                      @RequestParam String serialNumber,
	                      @RequestParam BigDecimal pricePerDay,
	                      @RequestParam BigDecimal baseValue,
	                      @RequestParam(required = false) Integer condition) {

		ToolName toolName = toolNameRepository.findById(toolNameId).orElse(null);
		if (toolName == null) return "redirect:/inventory?error=not_found";

		if (serialNumber == null || serialNumber.trim().isEmpty())
			return "redirect:/inventory/" + toolNameId + "?error=serial_required";
		if (pricePerDay == null || pricePerDay.compareTo(BigDecimal.ZERO) <= 0)
			return "redirect:/inventory/" + toolNameId + "?error=price_required";
		if (baseValue == null || baseValue.compareTo(BigDecimal.ZERO) <= 0)
			return "redirect:/inventory/" + toolNameId + "?error=base_value_required";

		Equipment eq = new Equipment();
		eq.setToolName(toolName);
		eq.setSerialNumber(serialNumber.trim());
		eq.setPricePerDay(pricePerDay);
		eq.setBaseValue(baseValue);
		eq.setCondition(condition != null ? condition : 10);
		eq.setStatus(EquipmentStatus.FREE);
		eq.setPricePerHour(pricePerDay.divide(new BigDecimal("8"), 2, RoundingMode.HALF_UP));
		eq.setPricePerWeek(pricePerDay.multiply(new BigDecimal("6")).setScale(2, RoundingMode.HALF_UP));
		eq.setPricePerMonth(pricePerDay.multiply(new BigDecimal("25")).setScale(2, RoundingMode.HALF_UP));

		try {
			equipmentRepository.save(eq);
		} catch (Exception e) {
			return "redirect:/inventory/" + toolNameId + "?error=save_failed";
		}
		return "redirect:/inventory/" + toolNameId + "?success=unit_added";
	}

	@PostMapping("/edit")
	public String editUnit(@RequestParam Long id,
	                       @RequestParam String serialNumber,
	                       @RequestParam BigDecimal pricePerDay,
	                       @RequestParam BigDecimal baseValue,
	                       @RequestParam(required = false) Integer condition) {

		Equipment eq = equipmentRepository.findById(id).orElse(null);
		if (eq == null) return "redirect:/inventory?error=not_found";

		Long toolNameId = eq.getToolName().getId();
		eq.setSerialNumber(serialNumber.trim());
		eq.setPricePerDay(pricePerDay);
		eq.setBaseValue(baseValue);
		eq.setCondition(condition != null ? condition : eq.getCondition());
		eq.setPricePerHour(pricePerDay.divide(new BigDecimal("8"), 2, RoundingMode.HALF_UP));
		eq.setPricePerWeek(pricePerDay.multiply(new BigDecimal("6")).setScale(2, RoundingMode.HALF_UP));
		eq.setPricePerMonth(pricePerDay.multiply(new BigDecimal("25")).setScale(2, RoundingMode.HALF_UP));

		try {
			equipmentRepository.save(eq);
		} catch (Exception e) {
			return "redirect:/inventory/" + toolNameId + "?error=save_failed";
		}
		return "redirect:/inventory/" + toolNameId + "?success=unit_updated";
	}

	@PostMapping("/delete")
	public String deleteUnit(@RequestParam Long id) {
		Equipment eq = equipmentRepository.findById(id).orElse(null);
		if (eq == null) return "redirect:/inventory?error=not_found";

		Long toolNameId = eq.getToolName().getId();
		if (eq.getStatus() != EquipmentStatus.FREE)
			return "redirect:/inventory/" + toolNameId + "?error=unit_in_use";

		eq.setIsDeleted(true);
		equipmentRepository.save(eq);
		return "redirect:/inventory/" + toolNameId + "?success=unit_deleted";
	}
}

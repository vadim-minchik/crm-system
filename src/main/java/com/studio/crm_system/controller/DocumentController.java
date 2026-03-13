package com.studio.crm_system.controller;

import com.studio.crm_system.entity.DocumentTemplate;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.DocumentTemplateRepository;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.RentalDocumentService;
import com.studio.crm_system.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/documents")
public class DocumentController {

	private static final List<String> ALLOWED_EXTENSIONS = List.of(".docx");

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DocumentTemplateRepository documentTemplateRepository;

	@Autowired
	private SupabaseStorageService storageService;

	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = (principal instanceof UserDetails)
				? ((UserDetails) principal).getUsername()
				: principal.toString();
		return userRepository.findByLogin(username).orElse(null);
	}

	private boolean isAllowedFileName(String name) {
		if (name == null || name.isBlank()) return false;
		String lower = name.toLowerCase(Locale.ROOT);
		return ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
	}

	@GetMapping
	public String list(Model model) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		model.addAttribute("templates", documentTemplateRepository.findAllByOrderByCreatedAtDesc());
		model.addAttribute("placeholdersHelp", RentalDocumentService.getPlaceholdersHelpWithEquipmentSlots());
		model.addAttribute("currentUser", user);
		model.addAttribute("username", user.getLogin());
		model.addAttribute("currentUserRole", user.getRole());
		return "html/documents";
	}

	@PostMapping("/upload")
	public String upload(@RequestParam String name,
	                     @RequestParam("file") MultipartFile file) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		if (name == null || name.isBlank())
			return "redirect:/documents?error=name_required";
		if (file.isEmpty())
			return "redirect:/documents?error=file_required";
		String originalName = file.getOriginalFilename();
		if (!isAllowedFileName(originalName))
			return "redirect:/documents?error=invalid_format";

		byte[] fileBytes;
		try {
			fileBytes = file.getBytes();
		} catch (Exception e) {
			return "redirect:/documents?error=upload_failed";
		}
		// .docx - это ZIP (начинается с PK). Отклоняем файл, если это не .docx.
		if (fileBytes.length < 4 || fileBytes[0] != 0x50 || fileBytes[1] != 0x4B) {
			return "redirect:/documents?error=not_docx";
		}

		DocumentTemplate template = new DocumentTemplate();
		template.setName(name.trim());
		template.setOriginalFileName(originalName != null ? originalName : "document.docx");
		template.setFileSize(file.getSize());
		template.setCreatedBy(user);
		documentTemplateRepository.save(template);

		try {
			String fileUrl = storageService.uploadTemplate(file, template.getId());
			template.setFileUrl(fileUrl);
			documentTemplateRepository.save(template);
		} catch (Exception e) {
			documentTemplateRepository.delete(template);
			e.printStackTrace();
			return "redirect:/documents?error=upload_failed";
		}

		return "redirect:/documents?success=template_added";
	}

	// Скачать файл шаблона (прокси с Supabase)
	@GetMapping(value = "/{id}/file", produces = "application/octet-stream")
	public ResponseEntity<byte[]> getFile(@PathVariable Long id) {
		if (getCurrentUser() == null)
			return ResponseEntity.status(401).build();

		DocumentTemplate template = documentTemplateRepository.findById(id).orElse(null);
		if (template == null || template.getFileUrl() == null || template.getFileUrl().isBlank())
			return ResponseEntity.notFound().build();

		byte[] bytes = storageService.downloadByStoredUrl(template.getFileUrl());
		if (bytes == null || bytes.length == 0) return ResponseEntity.notFound().build();

		// .docx - это ZIP (начинается с PK). Иначе в хранилище попал не тот файл.
		if (bytes.length < 4 || bytes[0] != 0x50 || bytes[1] != 0x4B) {
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-Error-Code", "not_docx");
			headers.set("X-Error-Message", "Not a valid .docx file. Re-upload template from Word.");
			return new ResponseEntity<byte[]>(null, headers, org.springframework.http.HttpStatusCode.valueOf(422));
		}

		String name = template.getOriginalFileName() != null ? template.getOriginalFileName() : "template.docx";
		MediaType mediaType = name.toLowerCase().endsWith(".docx")
				? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
				: MediaType.parseMediaType("application/msword");
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
				.contentType(mediaType)
				.body(bytes);
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id) {
		if (getCurrentUser() == null) return "redirect:/login";

		DocumentTemplate template = documentTemplateRepository.findById(id).orElse(null);
		if (template == null) return "redirect:/documents?error=not_found";

		if (template.getFileUrl() != null && !template.getFileUrl().isBlank()) {
			storageService.deleteByUrl(template.getFileUrl());
		}
		documentTemplateRepository.delete(template);
		return "redirect:/documents?success=template_deleted";
	}
}

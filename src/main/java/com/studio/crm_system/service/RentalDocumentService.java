package com.studio.crm_system.service;

import com.studio.crm_system.entity.Client;
import com.studio.crm_system.entity.Equipment;
import com.studio.crm_system.entity.Rental;
import com.studio.crm_system.entity.User;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import com.studio.crm_system.util.RussianNumberWords;

/**
 * Подставляет данные проката и клиента в шаблон (переменные {{NAME}}) для формирования документа Word.
 */
@Service
public class RentalDocumentService {

	private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	public static final int MAX_EQUIPMENT_SLOTS = 50;

	/** Список подстановок для справки в разделе «Документы» и при заполнении шаблона. */
	public static final String[][] PLACEHOLDERS_HELP = {
		{ "{{CLIENT_FIO}}", "ФИО клиента (полностью)" },
		{ "{{CLIENT_SURNAME}}", "Фамилия клиента" },
		{ "{{CLIENT_NAME}}", "Имя клиента" },
		{ "{{CLIENT_PATRONYMIC}}", "Отчество клиента" },
		{ "{{CLIENT_PHONE}}", "Телефон клиента" },
		{ "{{CLIENT_PASSPORT}}", "Серия и номер паспорта (вместе)" },
		{ "{{CLIENT_PASSPORT_SERIES}}", "Серия паспорта" },
		{ "{{CLIENT_PASSPORT_NUMBER}}", "Номер паспорта" },
		{ "{{CLIENT_IDENTIFICATION_NUMBER}}", "Идентификационный номер" },
		{ "{{CLIENT_ADDRESS}}", "Адрес регистрации (полный)" },
		{ "{{CLIENT_ADDRESS_STREET}}", "Улица" },
		{ "{{CLIENT_ADDRESS_HOUSE}}", "Дом" },
		{ "{{CLIENT_ADDRESS_ENTRANCE}}", "Подъезд" },
		{ "{{CLIENT_ADDRESS_BUILDING}}", "Корпус" },
		{ "{{CLIENT_ADDRESS_APARTMENT}}", "Квартира" },
		{ "{{CLIENT_BIRTH_DATE}}", "Дата рождения клиента" },
		{ "{{RENTAL_DATE_FROM}}", "Дата и время начала проката" },
		{ "{{RENTAL_DATE_TO}}", "Дата и время окончания проката" },
		{ "{{RENTAL_DURATION_DAYS}}", "Срок проката в сутках (число): от даты начала до даты окончания включительно по календарным дням" },
		{ "{{RENTAL_DURATION_DAYS_WORDS}}", "Тот же срок проката прописью (например «три»)" },
		{ "{{RENTAL_TOTAL}}", "Сумма проката (Br)" },
		{ "{{RENTAL_EQUIPMENT_LIST}}", "Список оборудования в одну строку" },
		{ "{{EQUIPMENT_COUNT}}", "Количество единиц оборудования" },
		{ "{{POINT_NAME}}", "Название точки выдачи" },
		{ "{{RENTAL_STAFF_CREATED_BY}}", "ФИО сотрудника, который оформил прокат" },
		{ "{{RENTAL_STAFF_CREATED_BY_SURNAME}}", "Фамилия того, кто оформил" },
		{ "{{RENTAL_STAFF_CREATED_BY_NAME}}", "Имя того, кто оформил" },
		{ "{{RENTAL_STAFF_CREATED_BY_PATRONYMIC}}", "Отчество того, кто оформил" },
		{ "{{RENTAL_STAFF_CREATED_BY_PHONE}}", "Телефон того, кто оформил" },
		{ "{{RENTAL_STAFF_CREATED_BY_LOGIN}}", "Логин того, кто оформил" },
		{ "{{RENTAL_STAFF_CREATED_BY_EMAIL}}", "Email того, кто оформил" },
		{ "{{RENTAL_STAFF_CREATED_BY_ROLE}}", "Роль того, кто оформил (ADMIN, WORKER, SUPER_ADMIN)" },
		{ "{{RENTAL_STAFF_HANDED_OVER_BY}}", "ФИО сотрудника, который отдал оборудование" },
		{ "{{RENTAL_STAFF_HANDED_OVER_BY_SURNAME}}", "Фамилия того, кто отдал" },
		{ "{{RENTAL_STAFF_HANDED_OVER_BY_NAME}}", "Имя того, кто отдал" },
		{ "{{RENTAL_STAFF_HANDED_OVER_BY_PATRONYMIC}}", "Отчество того, кто отдал" },
		{ "{{RENTAL_STAFF_HANDED_OVER_BY_PHONE}}", "Телефон того, кто отдал" },
		{ "{{RENTAL_STAFF_HANDED_OVER_BY_LOGIN}}", "Логин того, кто отдал" },
		{ "{{RENTAL_STAFF_HANDED_OVER_BY_EMAIL}}", "Email того, кто отдал" },
		{ "{{RENTAL_STAFF_HANDED_OVER_BY_ROLE}}", "Роль того, кто отдал" },
		{ "{{ADDITIONAL_SERVICES_DESC}}", "Описание доп. услуг" },
		{ "{{ADDITIONAL_SERVICES_AMOUNT}}", "Сумма доп. услуг (Br)" },
		{ "{{DELIVERY_AMOUNT}}", "Стоимость доставки (Br)" },
		{ "{{DELIVERY_ADDRESS}}", "Адрес доставки (улица, дом, кв.)" },
	};

	/** Количество полей на одну позицию оборудования (1..50). */
	private static final int EQUIPMENT_FIELDS_PER_SLOT = 8;

	/** Возвращает справку по подстановкам: базовые + все поля по каждой позиции оборудования (1..50). */
	public static String[][] getPlaceholdersHelpWithEquipmentSlots() {
		int baseLen = PLACEHOLDERS_HELP.length;
		int slotsLen = MAX_EQUIPMENT_SLOTS * EQUIPMENT_FIELDS_PER_SLOT;
		String[][] out = new String[baseLen + slotsLen][2];
		System.arraycopy(PLACEHOLDERS_HELP, 0, out, 0, baseLen);
		int idx = baseLen;
		for (int n = 1; n <= MAX_EQUIPMENT_SLOTS; n++) {
			out[idx][0] = "{{EQUIPMENT_" + n + "_TITLE}}";
			out[idx][1] = "Позиция " + n + ": название";
			idx++;
			out[idx][0] = "{{EQUIPMENT_" + n + "_SERIAL}}";
			out[idx][1] = "Позиция " + n + ": серийный номер";
			idx++;
			out[idx][0] = "{{EQUIPMENT_" + n + "_BASE_VALUE}}";
			out[idx][1] = "Позиция " + n + ": оценочная стоимость (Br)";
			idx++;
			out[idx][0] = "{{EQUIPMENT_" + n + "_BASE_VALUE_WORDS}}";
			out[idx][1] = "Позиция " + n + ": оценочная стоимость целым числом прописью (например «пятьсот»)";
			idx++;
			out[idx][0] = "{{EQUIPMENT_" + n + "_CONDITION}}";
			out[idx][1] = "Позиция " + n + ": состояние (1–10)";
			idx++;
			out[idx][0] = "{{EQUIPMENT_" + n + "_PRICE_FIRST_DAY}}";
			out[idx][1] = "Позиция " + n + ": цена первые сутки (Br)";
			idx++;
			out[idx][0] = "{{EQUIPMENT_" + n + "_PRICE_SUBSEQUENT_DAYS}}";
			out[idx][1] = "Позиция " + n + ": цена последующие сутки (Br)";
			idx++;
			out[idx][0] = "{{EQUIPMENT_" + n + "_PRICE_FIRST_MONTH}}";
			out[idx][1] = "Позиция " + n + ": цена первый месяц (Br)";
			idx++;
		}
		return out;
	}

	/**
	 * Заполняет шаблон .docx данными проката через Apache POI.
	 * Подставляет переменные {{NAME}} в параграфы, таблицы, колонтитулы — без зависимости от разбиения Word на run'ы.
	 */
	public byte[] fillTemplateDocx(byte[] docxBytes, Rental rental) throws IOException {
		if (docxBytes == null || docxBytes.length < 22) return null;
		if (docxBytes[0] != 0x50 || docxBytes[1] != 0x4B) return null;

		Map<String, String> placeholders = buildPlaceholders(rental);

		try (ByteArrayInputStream in = new ByteArrayInputStream(docxBytes);
		     XWPFDocument doc = new XWPFDocument(in);
		     ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			// Основной текст
			for (XWPFParagraph p : doc.getParagraphs()) {
				replaceInParagraph(p, placeholders);
			}

			// Таблицы
			for (XWPFTable table : doc.getTables()) {
				for (XWPFTableRow row : table.getRows()) {
					for (XWPFTableCell cell : row.getTableCells()) {
						for (XWPFParagraph p : cell.getParagraphs()) {
							replaceInParagraph(p, placeholders);
						}
					}
				}
			}

			// Колонтитулы
			if (doc.getHeaderList() != null) {
				doc.getHeaderList().forEach(h -> { if (h != null) h.getParagraphs().forEach(p -> replaceInParagraph(p, placeholders)); });
			}
			if (doc.getFooterList() != null) {
				doc.getFooterList().forEach(f -> { if (f != null) f.getParagraphs().forEach(p -> replaceInParagraph(p, placeholders)); });
			}

			doc.write(out);
			return out.toByteArray();
		}
	}

	/** Подставляет переменные в каждом run отдельно; подставленный текст получает размер/цвет как у соседнего run. */
	private static void replaceInParagraph(XWPFParagraph p, Map<String, String> placeholders) {
		java.util.List<XWPFRun> runs = p.getRuns();
		if (runs == null || runs.isEmpty()) return;

		for (int i = 0; i < runs.size(); i++) {
			XWPFRun run = runs.get(i);
			String text = run.text();
			if (text == null || text.isEmpty()) continue;

			String replaced = text;
			for (Map.Entry<String, String> e : placeholders.entrySet()) {
				String val = e.getValue() != null ? e.getValue() : "";
				replaced = replaced.replace(e.getKey(), val);
			}
			if (replaced.equals(text)) continue;

			run.setText(replaced, 0);
			// Размер — как у соседнего run; цвет всегда чёрный
			applyNeighborFormatting(run, runs, i);
		}
	}

	/** Копирует размер и шрифт из соседнего run; цвет подставленного текста всегда чёрный. */
	private static void applyNeighborFormatting(XWPFRun target, java.util.List<XWPFRun> runs, int currentIndex) {
		Integer size = null;
		String fontFamily = null;
		if (currentIndex > 0) {
			XWPFRun prev = runs.get(currentIndex - 1);
			int ps = prev.getFontSize();
			if (ps > 0) size = ps;
			String pf = prev.getFontFamily();
			if (pf != null && !pf.isBlank()) fontFamily = pf.trim();
		}
		if (currentIndex < runs.size() - 1) {
			XWPFRun next = runs.get(currentIndex + 1);
			if (size == null) { int ns = next.getFontSize(); if (ns > 0) size = ns; }
			if (fontFamily == null) { String nf = next.getFontFamily(); if (nf != null && !nf.isBlank()) fontFamily = nf.trim(); }
		}
		if (size != null) target.setFontSize(size);
		target.setColor("000000");
		// Шрифт — как у соседа, иначе Arial (чтобы не подставлялся системный моноширинный)
		if (fontFamily != null) target.setFontFamily(fontFamily); else target.setFontFamily("Arial");
	}

	/**
	 * Заполняет HTML-шаблон данными проката. В тексте заменяются все {{PLACEHOLDER}} на значения.
	 */
	public String fillTemplate(String html, Rental rental) {
		if (html == null) return "";
		String result = html;
		Map<String, String> map = buildPlaceholders(rental);
		for (Map.Entry<String, String> e : map.entrySet()) {
			result = result.replace(e.getKey(), e.getValue() != null ? e.getValue() : "");
		}
		return result;
	}

	private Map<String, String> buildPlaceholders(Rental rental) {
		Map<String, String> m = new HashMap<>();
		Client c = rental != null ? rental.getClient() : null;

		String fio = c != null ? (nullToEmpty(c.getSurname()) + " " + nullToEmpty(c.getName()) + " " + nullToEmpty(c.getPatronymic())).trim() : "";
		m.put("{{CLIENT_FIO}}", emptyToDash(fio));
		m.put("{{CLIENT_SURNAME}}", emptyToDash(c != null ? nullToEmpty(c.getSurname()) : ""));
		m.put("{{CLIENT_NAME}}", emptyToDash(c != null ? nullToEmpty(c.getName()) : ""));
		m.put("{{CLIENT_PATRONYMIC}}", emptyToDash(c != null ? nullToEmpty(c.getPatronymic()) : ""));
		m.put("{{CLIENT_PHONE}}", emptyToDash(c != null ? nullToEmpty(c.getPhoneNumber()) : ""));
		String passportFull = c != null ? nullToEmpty(c.getPassportNumber()) : "";
		m.put("{{CLIENT_PASSPORT}}", emptyToDash(passportFull));
		int spaceIdx = passportFull.indexOf(' ');
		m.put("{{CLIENT_PASSPORT_SERIES}}", emptyToDash(spaceIdx > 0 ? passportFull.substring(0, spaceIdx).trim() : ""));
		m.put("{{CLIENT_PASSPORT_NUMBER}}", emptyToDash(spaceIdx >= 0 ? passportFull.substring(spaceIdx + 1).trim() : passportFull));
		m.put("{{CLIENT_IDENTIFICATION_NUMBER}}", emptyToDash(c != null ? nullToEmpty(c.getIdentificationNumber()) : ""));
		m.put("{{CLIENT_ADDRESS}}", emptyToDash(c != null ? (c.getFullAddress() != null ? c.getFullAddress() : "") : ""));
		m.put("{{CLIENT_ADDRESS_STREET}}", emptyToDash(c != null ? nullToEmpty(c.getAddressStreet()) : ""));
		m.put("{{CLIENT_ADDRESS_HOUSE}}", emptyToDash(c != null ? nullToEmpty(c.getAddressHouse()) : ""));
		m.put("{{CLIENT_ADDRESS_ENTRANCE}}", emptyToDash(c != null ? nullToEmpty(c.getAddressEntrance()) : ""));
		m.put("{{CLIENT_ADDRESS_BUILDING}}", emptyToDash(c != null ? nullToEmpty(c.getAddressBuilding()) : ""));
		m.put("{{CLIENT_ADDRESS_APARTMENT}}", emptyToDash(c != null ? nullToEmpty(c.getAddressApartment()) : ""));
		m.put("{{CLIENT_BIRTH_DATE}}", c != null && c.getBirthDate() != null ? c.getBirthDate().format(DATE_FMT) : "—");

		m.put("{{RENTAL_DATE_FROM}}", rental != null && rental.getDateFrom() != null ? rental.getDateFrom().format(DATE_TIME_FMT) : "—");
		m.put("{{RENTAL_DATE_TO}}", rental != null && rental.getDateTo() != null ? rental.getDateTo().format(DATE_TIME_FMT) : "—");
		long rentalDays = rentalInclusiveCalendarDays(rental != null ? rental.getDateFrom() : null, rental != null ? rental.getDateTo() : null);
		m.put("{{RENTAL_DURATION_DAYS}}", rental != null && rental.getDateFrom() != null && rental.getDateTo() != null
				? String.valueOf(rentalDays) : "—");
		m.put("{{RENTAL_DURATION_DAYS_WORDS}}", rental != null && rental.getDateFrom() != null && rental.getDateTo() != null
				? RussianNumberWords.daysCountWords(rentalDays) : "—");
		m.put("{{RENTAL_TOTAL}}", emptyToDash(rental != null && rental.getTotalAmount() != null ? rental.getTotalAmount().toString() : ""));
		m.put("{{RENTAL_EQUIPMENT_LIST}}", emptyToDash(rental != null ? rental.getEquipmentDisplayString() : ""));
		List<Equipment> eqList = rental != null ? rental.getEquipmentList() : List.of();
		int count = eqList.size();
		m.put("{{EQUIPMENT_COUNT}}", String.valueOf(count));
		for (int n = 1; n <= MAX_EQUIPMENT_SLOTS; n++) {
			String title = "";
			String serial = "";
			String baseValue = "";
			String baseValueWords = "";
			String condition = "";
			String priceFirstDay = "";
			String priceSubsequentDays = "";
			String priceFirstMonth = "";
			if (n <= count) {
				Equipment eq = eqList.get(n - 1);
				if (eq != null) {
					title = nullToEmpty(eq.getTitle());
					serial = nullToEmpty(eq.getSerialNumber());
					baseValue = eq.getBaseValue() != null ? eq.getBaseValue().toString() : "";
					baseValueWords = RussianNumberWords.amountIntegerPartWords(eq.getBaseValue());
					condition = eq.getCondition() != null ? eq.getCondition().toString() : "";
					priceFirstDay = eq.getPriceFirstDay() != null ? eq.getPriceFirstDay().toString() : "";
					priceSubsequentDays = eq.getPriceSubsequentDays() != null ? eq.getPriceSubsequentDays().toString() : "";
					priceFirstMonth = eq.getPriceFirstMonth() != null ? eq.getPriceFirstMonth().toString() : "";
				}
			}
			m.put("{{EQUIPMENT_" + n + "_TITLE}}", emptyToDash(title));
			m.put("{{EQUIPMENT_" + n + "_SERIAL}}", emptyToDash(serial));
			m.put("{{EQUIPMENT_" + n + "_BASE_VALUE}}", emptyToDash(baseValue));
			m.put("{{EQUIPMENT_" + n + "_BASE_VALUE_WORDS}}", emptyToDash(baseValueWords));
			m.put("{{EQUIPMENT_" + n + "_CONDITION}}", emptyToDash(condition));
			m.put("{{EQUIPMENT_" + n + "_PRICE_FIRST_DAY}}", emptyToDash(priceFirstDay));
			m.put("{{EQUIPMENT_" + n + "_PRICE_SUBSEQUENT_DAYS}}", emptyToDash(priceSubsequentDays));
			m.put("{{EQUIPMENT_" + n + "_PRICE_FIRST_MONTH}}", emptyToDash(priceFirstMonth));
		}
		m.put("{{POINT_NAME}}", emptyToDash(rental != null && rental.getPoint() != null ? nullToEmpty(rental.getPoint().getName()) : ""));
		User createdBy = rental != null ? rental.getCreatedByStaff() : null;
		User handedOver = rental != null ? rental.getHandedOverByStaff() : null;
		m.put("{{RENTAL_STAFF_CREATED_BY}}", emptyToDash(formatUserFio(createdBy)));
		putUserPlaceholders(m, "RENTAL_STAFF_CREATED_BY", createdBy);
		m.put("{{RENTAL_STAFF_HANDED_OVER_BY}}", emptyToDash(formatUserFio(handedOver)));
		putUserPlaceholders(m, "RENTAL_STAFF_HANDED_OVER_BY", handedOver);
		m.put("{{ADDITIONAL_SERVICES_DESC}}", emptyToDash(rental != null ? nullToEmpty(rental.getAdditionalServicesDescription()) : ""));
		m.put("{{ADDITIONAL_SERVICES_AMOUNT}}", rental != null && rental.getAdditionalServicesAmount() != null ? rental.getAdditionalServicesAmount().toString() : "—");
		m.put("{{DELIVERY_AMOUNT}}", rental != null && rental.getDeliveryAmount() != null ? rental.getDeliveryAmount().toString() : "—");
		m.put("{{DELIVERY_ADDRESS}}", emptyToDash(rental != null ? rental.getDeliveryAddress() : null));

		return m;
	}

	private static String nullToEmpty(String s) {
		return s != null ? s : "";
	}

	/** Для подстановки в документ: пустую строку показываем как «—», чтобы было видно, что поле не заполнено. */
	private static String emptyToDash(String s) {
		return (s != null && !s.isBlank()) ? s : "—";
	}

	private static String formatUserFio(User u) {
		if (u == null) return "";
		return (nullToEmpty(u.getSurname()) + " " + nullToEmpty(u.getName()) + " " + nullToEmpty(u.getPatronymic())).trim();
	}

	private static void putUserPlaceholders(Map<String, String> m, String prefix, User u) {
		if (u == null) {
			m.put("{{" + prefix + "_SURNAME}}", "—");
			m.put("{{" + prefix + "_NAME}}", "—");
			m.put("{{" + prefix + "_PATRONYMIC}}", "—");
			m.put("{{" + prefix + "_PHONE}}", "—");
			m.put("{{" + prefix + "_LOGIN}}", "—");
			m.put("{{" + prefix + "_EMAIL}}", "—");
			m.put("{{" + prefix + "_ROLE}}", "—");
			return;
		}
		m.put("{{" + prefix + "_SURNAME}}", emptyToDash(nullToEmpty(u.getSurname())));
		m.put("{{" + prefix + "_NAME}}", emptyToDash(nullToEmpty(u.getName())));
		m.put("{{" + prefix + "_PATRONYMIC}}", emptyToDash(nullToEmpty(u.getPatronymic())));
		m.put("{{" + prefix + "_PHONE}}", emptyToDash(nullToEmpty(u.getPhoneNumber())));
		m.put("{{" + prefix + "_LOGIN}}", emptyToDash(nullToEmpty(u.getLogin())));
		m.put("{{" + prefix + "_EMAIL}}", emptyToDash(nullToEmpty(u.getEmail())));
		m.put("{{" + prefix + "_ROLE}}", emptyToDash(u.getRole() != null ? u.getRole().name() : ""));
	}

	/**
	 * Срок в календарных сутках включительно: от даты начала до даты окончания (по локальным датам).
	 * Например 01.01 10:00 — 03.01 18:00 → 3 суток.
	 */
	private static long rentalInclusiveCalendarDays(java.time.LocalDateTime from, java.time.LocalDateTime to) {
		if (from == null || to == null) return 0;
		if (to.isBefore(from)) return 0;
		return ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()) + 1;
	}
}

package com.studio.crm_system.service;

import com.studio.crm_system.entity.Client;
import com.studio.crm_system.entity.Equipment;
import com.studio.crm_system.entity.Rental;
import com.studio.crm_system.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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


@Service
public class RentalDocumentService {

	@Autowired
	private RentalService rentalService;

	private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	public static final int MAX_EQUIPMENT_SLOTS = 50;

	
	public static final String[][] PLACEHOLDERS_HELP = {
		{ "{{CLIENT_FIO}}", "ФИО клиента (полностью)" },
		{ "{{CLIENT_SURNAME}}", "Фамилия клиента" },
		{ "{{CLIENT_NAME}}", "Имя клиента" },
		{ "{{CLIENT_PATRONYMIC}}", "Отчество клиента" },
		{ "{{CLIENT_PHONE}}", "Телефон клиента" },
		{ "{{CLIENT_PASSPORT}}", "Серия и номер паспорта (вместе)" },
		{ "{{CLIENT_PASSPORT_SERIES}}", "Серия паспорта" },
		{ "{{CLIENT_PASSPORT_NUMBER}}", "Номер паспорта" },
		{ "{{CLIENT_PASSPORT_ISSUED_BY}}", "Орган, выдавший паспорт (как в паспорте)" },
		{ "{{CLIENT_PASSPORT_ISSUE_DATE}}", "Дата выдачи паспорта клиента (ДД.ММ.ГГГГ)" },
		{ "{{CLIENT_PASSPORT_EXPIRY_DATE}}", "Срок действия паспорта клиента — «действителен до» (ДД.ММ.ГГГГ)" },
		{ "{{CLIENT_PASSPORT_VALID_UNTIL}}", "То же, что {{CLIENT_PASSPORT_EXPIRY_DATE}} (удобно для формулировки «действителен до …»)" },
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
		{ "{{RENTAL_EQUIPMENT_LIST}}", "Краткий список через запятую (название — S/N …), как в карточке проката" },
		{ "{{EQUIPMENT_COUNT}}", "Количество единиц оборудования (число)" },
		{ "{{EQUIPMENT_LINES}}", "Универсальный блок по каждой позиции (несколько строк): наименование, S/N, оценка прописью, состояние, все тарифы суток и месяцев, точка" },
		{ "{{EQUIPMENT_INLINE_PLUS}}", "Все позиции в одну строку через « + »: кратко название и S/N" },
		{ "{{EQUIPMENT_NUMBERED}}", "То же полное описание, что в {{EQUIPMENT_LINES}}, но с номером 1., 2., …" },
		{ "{{EQUIPMENT_LINES_SHORT}}", "По одной строке на позицию: только наименование и S/N" },
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
		{ "{{RENTAL_STAFF_HANDED_OVER_PASSPORT}}", "Серия и номер паспорта сотрудника, выдавшего прокат" },
		{ "{{RENTAL_STAFF_HANDED_OVER_PASSPORT_ISSUE_DATE}}", "Дата выдачи паспорта того, кто выдал прокат (передал оборудование)" },
		{ "{{RENTAL_STAFF_HANDED_OVER_PASSPORT_EXPIRY_DATE}}", "Срок действия паспорта того, кто выдал прокат («действителен до»)" },
		{ "{{RENTAL_STAFF_HANDED_OVER_PASSPORT_VALID_UNTIL}}", "То же, что {{RENTAL_STAFF_HANDED_OVER_PASSPORT_EXPIRY_DATE}}" },
		{ "{{ADDITIONAL_SERVICES_DESC}}", "Описание доп. услуг" },
		{ "{{ADDITIONAL_SERVICES_AMOUNT}}", "Сумма доп. услуг (Br)" },
		{ "{{DELIVERY_AMOUNT}}", "Стоимость доставки (Br)" },
		{ "{{DELIVERY_ADDRESS}}", "Адрес доставки (улица, дом, кв.)" },
		{ "{{RENTAL_ORDER_CREATED_AT}}", "Когда оформлен заказ в CRM (дд.мм.гггг чч:мм); пусто, если не сохранялось в старых записях" },
		{ "{{RENTAL_DELIVERED_AT}}", "Когда нажато «Доставлено» / выдано клиенту (дд.мм.гггг чч:мм); пусто, если ещё не доставлено" },
		{ "{{RENTAL_ORDER_TO_DELIVERY_RANGE}}", "Строка «с … по …» от оформления до доставки; пусто, если нет одной из дат" },
		{ "{{DELIVERY_LEAD_MINUTES_TOTAL}}", "Всего минут от оформления до доставки; пусто, если нельзя посчитать" },
		{ "{{DELIVERY_LEAD_HOURS_TOTAL}}", "То же интервал в часах (число, до 2 знаков после запятой); пусто, если нельзя посчитать" },
		{ "{{DELIVERY_LEAD_DAYS}}", "Полные сутки в интервале оформление→доставка; пусто, если 0 или нет данных" },
		{ "{{DELIVERY_LEAD_HOURS_REMAINDER}}", "Часы после вычета полных суток (0–23); пусто, если 0 и нет суток, или нет данных" },
		{ "{{DELIVERY_LEAD_MINUTES_REMAINDER}}", "Минуты после вычета суток и часов (0–59); пусто, если 0 или нет данных" },
		{ "{{DELIVERY_LEAD_TEXT_RU}}", "Кратко по-русски, напр. «2 дн. 3 ч 15 мин»; пусто, если нет интервала или длительность 0" },
		{ "{{RENTAL_STAFF_CREATED_BY_OR_EMPTY}}", "ФИО оформившего; пустая строка, если не указан (в отличие от {{RENTAL_STAFF_CREATED_BY}} с «—»)" },
		{ "{{RENTAL_STAFF_HANDED_OVER_BY_OR_EMPTY}}", "ФИО выдавшего/доставившего; пусто, если не указан" },
	};

	
	private static String[][] equipmentSlotsHelpRows;

	
	private static String[][] getEquipmentSlotsHelpRows() {
		if (equipmentSlotsHelpRows != null) {
			return equipmentSlotsHelpRows;
		}
		String[][] meta = {
				{ "TITLE", "наименование модели" },
				{ "SERIAL", "серийный номер" },
				{ "BASE_VALUE", "оценочная стоимость (число)" },
				{ "BASE_VALUE_WORDS", "оценочная стоимость прописью" },
				{ "CONDITION", "состояние (баллы)" },
				{ "PRICE_FIRST_DAY", "тариф: первая сутки (Br)" },
				{ "PRICE_SECOND_DAY", "тариф: вторая сутки (Br)" },
				{ "PRICE_SUBSEQUENT_DAYS", "тариф: последующие сутки (Br)" },
				{ "PRICE_FIRST_MONTH", "тариф: первый месяц (Br)" },
				{ "PRICE_SECOND_MONTH", "тариф: второй месяц (Br)" },
				{ "PRICE_SUBSEQUENT_MONTHS", "тариф: последующие месяцы (Br)" },
				{ "RENT_TOTAL", "сумма аренды этой позиции за период проката по дневному тарифу (1-я + 2-я + последующие сутки), Br — как при автосумме проката" },
		};
		List<String[]> list = new ArrayList<>(MAX_EQUIPMENT_SLOTS * meta.length);
		for (int n = 1; n <= MAX_EQUIPMENT_SLOTS; n++) {
			for (String[] row : meta) {
				String key = "{{EQUIPMENT_" + n + "_" + row[0] + "}}";
				String desc = "Позиция " + n + " в прокате — " + row[1];
				list.add(new String[] { key, desc });
			}
		}
		equipmentSlotsHelpRows = list.toArray(new String[0][]);
		return equipmentSlotsHelpRows;
	}

	
	public static String[][] getPlaceholdersHelpWithEquipmentSlots() {
		String[][] base = PLACEHOLDERS_HELP;
		String[][] slots = getEquipmentSlotsHelpRows();
		String[][] out = new String[base.length + slots.length][2];
		System.arraycopy(base, 0, out, 0, base.length);
		System.arraycopy(slots, 0, out, base.length, slots.length);
		return out;
	}

	
	public byte[] fillTemplateDocx(byte[] docxBytes, Rental rental) throws IOException {
		if (docxBytes == null || docxBytes.length < 22) return null;
		if (docxBytes[0] != 0x50 || docxBytes[1] != 0x4B) return null;

		Map<String, String> placeholders = buildPlaceholders(rental);

		try (ByteArrayInputStream in = new ByteArrayInputStream(docxBytes);
		     XWPFDocument doc = new XWPFDocument(in);
		     ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			
			for (XWPFParagraph p : doc.getParagraphs()) {
				replaceInParagraph(p, placeholders);
			}

			
			for (XWPFTable table : doc.getTables()) {
				for (XWPFTableRow row : table.getRows()) {
					for (XWPFTableCell cell : row.getTableCells()) {
						for (XWPFParagraph p : cell.getParagraphs()) {
							replaceInParagraph(p, placeholders);
						}
					}
				}
			}

			
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
			
			applyNeighborFormatting(run, runs, i);
		}
	}

	
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
		
		if (fontFamily != null) target.setFontFamily(fontFamily); else target.setFontFamily("Arial");
	}

	
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
		m.put("{{CLIENT_PASSPORT_ISSUED_BY}}", emptyToDash(c != null ? nullToEmpty(c.getPassportIssuedBy()) : ""));
		m.put("{{CLIENT_IDENTIFICATION_NUMBER}}", emptyToDash(c != null ? nullToEmpty(c.getIdentificationNumber()) : ""));
		m.put("{{CLIENT_ADDRESS}}", emptyToDash(c != null ? (c.getFullAddress() != null ? c.getFullAddress() : "") : ""));
		m.put("{{CLIENT_ADDRESS_STREET}}", emptyToDash(c != null ? nullToEmpty(c.getAddressStreet()) : ""));
		m.put("{{CLIENT_ADDRESS_HOUSE}}", emptyToDash(c != null ? nullToEmpty(c.getAddressHouse()) : ""));
		m.put("{{CLIENT_ADDRESS_ENTRANCE}}", emptyToDash(c != null ? nullToEmpty(c.getAddressEntrance()) : ""));
		m.put("{{CLIENT_ADDRESS_BUILDING}}", emptyToDash(c != null ? nullToEmpty(c.getAddressBuilding()) : ""));
		m.put("{{CLIENT_ADDRESS_APARTMENT}}", emptyToDash(c != null ? nullToEmpty(c.getAddressApartment()) : ""));
		m.put("{{CLIENT_BIRTH_DATE}}", c != null && c.getBirthDate() != null ? c.getBirthDate().format(DATE_FMT) : "—");
		String clientIssue = c != null && c.getPassportIssueDate() != null ? c.getPassportIssueDate().format(DATE_FMT) : "—";
		String clientExpiry = c != null && c.getPassportExpiryDate() != null ? c.getPassportExpiryDate().format(DATE_FMT) : "—";
		m.put("{{CLIENT_PASSPORT_ISSUE_DATE}}", clientIssue);
		m.put("{{CLIENT_PASSPORT_EXPIRY_DATE}}", clientExpiry);
		m.put("{{CLIENT_PASSPORT_VALID_UNTIL}}", clientExpiry);

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
		putEquipmentAggregates(m, eqList);
		for (int n = 1; n <= MAX_EQUIPMENT_SLOTS; n++) {
			String title = "";
			String serial = "";
			String baseValue = "";
			String baseValueWords = "";
			String condition = "";
			String priceFirstDay = "";
			String priceSecondDay = "";
			String priceSubsequentDays = "";
			String priceFirstMonth = "";
			String priceSecondMonth = "";
			String priceSubsequentMonths = "";
			String rentTotal = "—";
			if (n <= count) {
				Equipment eq = eqList.get(n - 1);
				if (eq != null) {
					title = nullToEmpty(eq.getTitle());
					serial = nullToEmpty(eq.getSerialNumber());
					baseValue = eq.getBaseValue() != null ? eq.getBaseValue().toString() : "";
					baseValueWords = RussianNumberWords.amountIntegerPartWords(eq.getBaseValue());
					condition = eq.getCondition() != null ? eq.getCondition().toString() : "";
					priceFirstDay = eq.getPriceFirstDay() != null ? eq.getPriceFirstDay().toString() : "";
					priceSecondDay = eq.getPriceSecondDay() != null ? eq.getPriceSecondDay().toString() : "";
					priceSubsequentDays = eq.getPriceSubsequentDays() != null ? eq.getPriceSubsequentDays().toString() : "";
					priceFirstMonth = eq.getPriceFirstMonth() != null ? eq.getPriceFirstMonth().toString() : "";
					priceSecondMonth = eq.getPriceSecondMonth() != null ? eq.getPriceSecondMonth().toString() : "";
					priceSubsequentMonths = eq.getPriceSubsequentMonths() != null ? eq.getPriceSubsequentMonths().toString() : "";
					if (rental != null && rental.getDateFrom() != null && rental.getDateTo() != null) {
						BigDecimal calc = rentalService.calculateTotal(rental.getDateFrom(), rental.getDateTo(), eq);
						rentTotal = calc != null ? calc.setScale(2, RoundingMode.HALF_UP).toPlainString() : "—";
					}
				}
			}
			m.put("{{EQUIPMENT_" + n + "_TITLE}}", emptyToDash(title));
			m.put("{{EQUIPMENT_" + n + "_SERIAL}}", emptyToDash(serial));
			m.put("{{EQUIPMENT_" + n + "_BASE_VALUE}}", emptyToDash(baseValue));
			m.put("{{EQUIPMENT_" + n + "_BASE_VALUE_WORDS}}", emptyToDash(baseValueWords));
			m.put("{{EQUIPMENT_" + n + "_CONDITION}}", emptyToDash(condition));
			m.put("{{EQUIPMENT_" + n + "_PRICE_FIRST_DAY}}", emptyToDash(priceFirstDay));
			m.put("{{EQUIPMENT_" + n + "_PRICE_SECOND_DAY}}", emptyToDash(priceSecondDay));
			m.put("{{EQUIPMENT_" + n + "_PRICE_SUBSEQUENT_DAYS}}", emptyToDash(priceSubsequentDays));
			m.put("{{EQUIPMENT_" + n + "_PRICE_FIRST_MONTH}}", emptyToDash(priceFirstMonth));
			m.put("{{EQUIPMENT_" + n + "_PRICE_SECOND_MONTH}}", emptyToDash(priceSecondMonth));
			m.put("{{EQUIPMENT_" + n + "_PRICE_SUBSEQUENT_MONTHS}}", emptyToDash(priceSubsequentMonths));
			m.put("{{EQUIPMENT_" + n + "_RENT_TOTAL}}", rentTotal);
		}
		m.put("{{POINT_NAME}}", emptyToDash(rental != null && rental.getPoint() != null ? nullToEmpty(rental.getPoint().getName()) : ""));
		User createdBy = rental != null ? rental.getCreatedByStaff() : null;
		User handedOver = rental != null ? rental.getHandedOverByStaff() : null;
		m.put("{{RENTAL_STAFF_CREATED_BY}}", emptyToDash(formatUserFio(createdBy)));
		putUserPlaceholders(m, "RENTAL_STAFF_CREATED_BY", createdBy);
		m.put("{{RENTAL_STAFF_HANDED_OVER_BY}}", emptyToDash(formatUserFio(handedOver)));
		putUserPlaceholders(m, "RENTAL_STAFF_HANDED_OVER_BY", handedOver);
		putHandedOverStaffPassportPlaceholders(m, handedOver);
		m.put("{{ADDITIONAL_SERVICES_DESC}}", emptyToDash(rental != null ? nullToEmpty(rental.getAdditionalServicesDescription()) : ""));
		m.put("{{ADDITIONAL_SERVICES_AMOUNT}}", rental != null && rental.getAdditionalServicesAmount() != null ? rental.getAdditionalServicesAmount().toString() : "—");
		m.put("{{DELIVERY_AMOUNT}}", rental != null && rental.getDeliveryAmount() != null ? rental.getDeliveryAmount().toString() : "—");
		m.put("{{DELIVERY_ADDRESS}}", emptyToDash(rental != null ? rental.getDeliveryAddress() : null));

		putOptionalStaffFioPlaceholders(m, rental);
		putOrderAndDeliveryTimelinePlaceholders(m, rental);

		return m;
	}

	
	private static void putOptionalStaffFioPlaceholders(Map<String, String> m, Rental rental) {
		User createdBy = rental != null ? rental.getCreatedByStaff() : null;
		User handedOver = rental != null ? rental.getHandedOverByStaff() : null;
		String cf = formatUserFio(createdBy);
		String hf = formatUserFio(handedOver);
		m.put("{{RENTAL_STAFF_CREATED_BY_OR_EMPTY}}", cf.isBlank() ? "" : cf);
		m.put("{{RENTAL_STAFF_HANDED_OVER_BY_OR_EMPTY}}", hf.isBlank() ? "" : hf);
	}

	
	private static void putOrderAndDeliveryTimelinePlaceholders(Map<String, String> m, Rental rental) {
		String empty = "";
		m.put("{{RENTAL_ORDER_CREATED_AT}}", empty);
		m.put("{{RENTAL_DELIVERED_AT}}", empty);
		m.put("{{RENTAL_ORDER_TO_DELIVERY_RANGE}}", empty);
		m.put("{{DELIVERY_LEAD_MINUTES_TOTAL}}", empty);
		m.put("{{DELIVERY_LEAD_HOURS_TOTAL}}", empty);
		m.put("{{DELIVERY_LEAD_DAYS}}", empty);
		m.put("{{DELIVERY_LEAD_HOURS_REMAINDER}}", empty);
		m.put("{{DELIVERY_LEAD_MINUTES_REMAINDER}}", empty);
		m.put("{{DELIVERY_LEAD_TEXT_RU}}", empty);
		if (rental == null) {
			return;
		}
		LocalDateTime created = rental.getCreatedAt();
		LocalDateTime delivered = rental.getDeliveredAt();
		if (created != null) {
			m.put("{{RENTAL_ORDER_CREATED_AT}}", created.format(DATE_TIME_FMT));
		}
		if (delivered != null) {
			m.put("{{RENTAL_DELIVERED_AT}}", delivered.format(DATE_TIME_FMT));
		}
		if (created != null && delivered != null && !delivered.isBefore(created)) {
			m.put("{{RENTAL_ORDER_TO_DELIVERY_RANGE}}",
					"с " + created.format(DATE_TIME_FMT) + " по " + delivered.format(DATE_TIME_FMT));
			Duration d = Duration.between(created, delivered);
			long totalMinutes = d.toMinutes();
			if (totalMinutes > 0) {
				m.put("{{DELIVERY_LEAD_MINUTES_TOTAL}}", String.valueOf(totalMinutes));
				BigDecimal hoursTotal = BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP).stripTrailingZeros();
				m.put("{{DELIVERY_LEAD_HOURS_TOTAL}}", hoursTotal.toPlainString());
				long days = d.toDays();
				Duration afterDays = d.minusDays(days);
				long hoursRem = afterDays.toHours();
				Duration afterHours = afterDays.minusHours(hoursRem);
				long minRem = afterHours.toMinutes();
				if (days > 0) {
					m.put("{{DELIVERY_LEAD_DAYS}}", String.valueOf(days));
				}
				if (hoursRem > 0) {
					m.put("{{DELIVERY_LEAD_HOURS_REMAINDER}}", String.valueOf(hoursRem));
				}
				if (minRem > 0) {
					m.put("{{DELIVERY_LEAD_MINUTES_REMAINDER}}", String.valueOf(minRem));
				}
				m.put("{{DELIVERY_LEAD_TEXT_RU}}", formatDeliveryLeadRussian(d));
			}
		}
	}

	
	public String buildDeliveryLeadDescriptionForUi(Rental rental) {
		if (rental == null || rental.getCreatedAt() == null || rental.getDeliveredAt() == null) {
			return null;
		}
		if (rental.getDeliveredAt().isBefore(rental.getCreatedAt())) {
			return null;
		}
		Duration d = Duration.between(rental.getCreatedAt(), rental.getDeliveredAt());
		long mins = d.toMinutes();
		if (mins <= 0) {
			return null;
		}
		String ru = formatDeliveryLeadRussian(d);
		if (ru.isEmpty()) {
			return null;
		}
		BigDecimal h = BigDecimal.valueOf(mins).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP).stripTrailingZeros();
		return ru + " (" + mins + " мин; " + h.toPlainString() + " ч)";
	}

	private static String formatDeliveryLeadRussian(Duration d) {
		if (d == null || d.isNegative() || d.isZero()) {
			return "";
		}
		long days = d.toDays();
		Duration r = d.minusDays(days);
		long h = r.toHours();
		r = r.minusHours(h);
		long min = r.toMinutes();
		if (days == 0 && h == 0 && min == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if (days > 0) {
			sb.append(days).append(" дн. ");
		}
		if (h > 0) {
			sb.append(h).append(" ч ");
		}
		if (min > 0) {
			sb.append(min).append(" мин");
		}
		return sb.toString().trim();
	}

	
	private static void putEquipmentAggregates(Map<String, String> m, List<Equipment> eqList) {
		if (eqList == null || eqList.isEmpty()) {
			m.put("{{EQUIPMENT_LINES}}", "—");
			m.put("{{EQUIPMENT_INLINE_PLUS}}", "—");
			m.put("{{EQUIPMENT_NUMBERED}}", "—");
			m.put("{{EQUIPMENT_LINES_SHORT}}", "—");
			return;
		}
		List<String> lines = new ArrayList<>();
		List<String> plus = new ArrayList<>();
		List<String> numbered = new ArrayList<>();
		List<String> shortLines = new ArrayList<>();
		int idx = 1;
		for (Equipment eq : eqList) {
			if (eq == null) continue;
			String block = buildUniversalEquipmentBlock(eq);
			lines.add(block);
			String title = nullToEmpty(eq.getTitle());
			String serial = nullToEmpty(eq.getSerialNumber());
			shortLines.add(title + " — S/N " + serial);
			plus.add(title + " (S/N " + serial + ")");
			numbered.add(formatNumberedEquipmentBlock(idx, block));
			idx++;
		}
		String betweenItems = "\n\n";
		m.put("{{EQUIPMENT_LINES}}", String.join(betweenItems, lines));
		m.put("{{EQUIPMENT_INLINE_PLUS}}", String.join(" + ", plus));
		m.put("{{EQUIPMENT_NUMBERED}}", String.join(betweenItems, numbered));
		m.put("{{EQUIPMENT_LINES_SHORT}}", String.join("\n", shortLines));
	}

	
	private static String buildUniversalEquipmentBlock(Equipment eq) {
		String title = nullToEmpty(eq.getTitle());
		String serial = nullToEmpty(eq.getSerialNumber());
		String base = eq.getBaseValue() != null ? eq.getBaseValue().toPlainString() : "";
		String baseWords = RussianNumberWords.amountIntegerPartWords(eq.getBaseValue());
		String cond = eq.getCondition() != null ? eq.getCondition().toString() : "—";
		String p1 = moneyPlain(eq.getPriceFirstDay());
		String p2 = moneyPlain(eq.getPriceSecondDay());
		String ps = moneyPlain(eq.getPriceSubsequentDays());
		String pm1 = moneyPlain(eq.getPriceFirstMonth());
		String pm2 = moneyPlain(eq.getPriceSecondMonth());
		String pms = moneyPlain(eq.getPriceSubsequentMonths());
		String pointName = "";
		if (eq.getPoint() != null && eq.getPoint().getName() != null) {
			pointName = eq.getPoint().getName().trim();
		}

		StringBuilder sb = new StringBuilder();
		sb.append(title).append(" — заводской (серийный) № ").append(serial);
		sb.append("\n");
		sb.append("Оценочная стоимость: ");
		sb.append(base.isEmpty() ? "—" : base).append(" Br (").append(baseWords).append("); состояние (баллы): ").append(cond);
		sb.append("\n");
		sb.append("Тариф за сутки: 1-я ").append(p1).append(" Br, 2-я ").append(p2).append(" Br, последующие ").append(ps).append(" Br");
		sb.append("\n");
		sb.append("Тариф за месяцы: 1-й ").append(pm1).append(" Br, 2-й ").append(pm2).append(" Br, последующие ").append(pms).append(" Br");
		if (!pointName.isEmpty()) {
			sb.append("\n").append("Точка выдачи / учёта: ").append(pointName);
		}
		return sb.toString();
	}

	private static String formatNumberedEquipmentBlock(int n, String multilineBlock) {
		if (multilineBlock == null || multilineBlock.isEmpty()) {
			return n + ". —";
		}
		String[] lines = multilineBlock.split("\\R", -1);
		StringBuilder sb = new StringBuilder();
		sb.append(n).append(". ").append(lines[0]);
		for (int i = 1; i < lines.length; i++) {
			if (!lines[i].isEmpty()) {
				sb.append("\n   ").append(lines[i]);
			}
		}
		return sb.toString();
	}

	private static String moneyPlain(BigDecimal v) {
		return v != null ? v.toPlainString() : "—";
	}

	private static String nullToEmpty(String s) {
		return s != null ? s : "";
	}

	
	private static String emptyToDash(String s) {
		return (s != null && !s.isBlank()) ? s : "—";
	}

	private static String formatUserFio(User u) {
		if (u == null) return "";
		return (nullToEmpty(u.getSurname()) + " " + nullToEmpty(u.getName()) + " " + nullToEmpty(u.getPatronymic())).trim();
	}

	
	private static void putHandedOverStaffPassportPlaceholders(Map<String, String> m, User handedOver) {
		if (handedOver == null) {
			m.put("{{RENTAL_STAFF_HANDED_OVER_PASSPORT}}", "—");
			m.put("{{RENTAL_STAFF_HANDED_OVER_PASSPORT_ISSUE_DATE}}", "—");
			m.put("{{RENTAL_STAFF_HANDED_OVER_PASSPORT_EXPIRY_DATE}}", "—");
			m.put("{{RENTAL_STAFF_HANDED_OVER_PASSPORT_VALID_UNTIL}}", "—");
			return;
		}
		String ps = nullToEmpty(handedOver.getPassportSeries());
		String pn = nullToEmpty(handedOver.getPassportNumber());
		String fullPass = (ps + " " + pn).trim();
		m.put("{{RENTAL_STAFF_HANDED_OVER_PASSPORT}}", emptyToDash(fullPass));
		String issue = handedOver.getPassportIssueDate() != null
				? handedOver.getPassportIssueDate().format(DATE_FMT) : "—";
		String exp = handedOver.getPassportExpiryDate() != null
				? handedOver.getPassportExpiryDate().format(DATE_FMT) : "—";
		m.put("{{RENTAL_STAFF_HANDED_OVER_PASSPORT_ISSUE_DATE}}", issue);
		m.put("{{RENTAL_STAFF_HANDED_OVER_PASSPORT_EXPIRY_DATE}}", exp);
		m.put("{{RENTAL_STAFF_HANDED_OVER_PASSPORT_VALID_UNTIL}}", exp);
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

	
	private static long rentalInclusiveCalendarDays(java.time.LocalDateTime from, java.time.LocalDateTime to) {
		if (from == null || to == null) return 0;
		if (to.isBefore(from)) return 0;
		return ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()) + 1;
	}
}

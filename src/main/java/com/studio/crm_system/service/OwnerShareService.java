package com.studio.crm_system.service;

import com.studio.crm_system.dto.OwnerAccrualLineDto;
import com.studio.crm_system.dto.OwnerBalanceRowDto;
import com.studio.crm_system.entity.Equipment;
import com.studio.crm_system.entity.EquipmentOwner;
import com.studio.crm_system.entity.OwnerPayout;
import com.studio.crm_system.entity.Rental;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.enums.RentalStatus;
import com.studio.crm_system.repository.EquipmentOwnerRepository;
import com.studio.crm_system.repository.OwnerPayoutRepository;
import com.studio.crm_system.repository.RentalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class OwnerShareService {

	public static final Set<RentalStatus> ACCRUAL_STATUSES = EnumSet.of(
			RentalStatus.COMPLETED, RentalStatus.DEBTOR, RentalStatus.SOON_DEBTOR);

	private static final BigDecimal HUNDRED = new BigDecimal("100");

	@Autowired private RentalRepository rentalRepository;
	@Autowired private RentalService rentalService;
	@Autowired private EquipmentOwnerRepository equipmentOwnerRepository;
	@Autowired private OwnerPayoutRepository ownerPayoutRepository;

	/**
	 * Доля суммы проката, относимая к одному экземпляру: пропорционально расчётной базе позиций
	 * (как при автоматическом пересчёте total). Доп. услуги и доставка распределяются пропорционально базе.
	 */
	public BigDecimal equipmentShareInRental(Rental rental, Equipment equipment) {
		if (rental == null || equipment == null || rental.getTotalAmount() == null)
			return BigDecimal.ZERO;
		List<Equipment> list = rental.getEquipmentList();
		if (list == null || list.isEmpty())
			return BigDecimal.ZERO;
		BigDecimal total = rental.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
		BigDecimal baseSum = BigDecimal.ZERO;
		for (Equipment e : list) {
			baseSum = baseSum.add(rentalService.calculateTotal(rental.getDateFrom(), rental.getDateTo(), e));
		}
		baseSum = baseSum.setScale(2, RoundingMode.HALF_UP);
		if (baseSum.compareTo(BigDecimal.ZERO) <= 0) {
			int n = list.size();
			if (n <= 0) return BigDecimal.ZERO;
			return total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
		}
		BigDecimal eqBase = rentalService.calculateTotal(rental.getDateFrom(), rental.getDateTo(), equipment)
				.setScale(2, RoundingMode.HALF_UP);
		return total.multiply(eqBase).divide(baseSum, 2, RoundingMode.HALF_UP);
	}

	public BigDecimal accruedForEquipmentOwner(EquipmentOwner owner) {
		if (owner == null || owner.getEquipment() == null)
			return BigDecimal.ZERO;
		Equipment eq = owner.getEquipment();
		BigDecimal acc = BigDecimal.ZERO;
		List<Rental> rentals = rentalRepository.findByEquipmentIdOrderByDateFromDesc(eq.getId());
		for (Rental r : rentals) {
			if (!ACCRUAL_STATUSES.contains(r.getStatus()))
				continue;
			BigDecimal equipPart = equipmentShareInRental(r, eq);
			acc = acc.add(equipPart.multiply(owner.getRentalSharePercent())
					.divide(HUNDRED, 2, RoundingMode.HALF_UP));
		}
		return acc.setScale(2, RoundingMode.HALF_UP);
	}

	public BigDecimal paidForEquipmentOwner(EquipmentOwner owner) {
		if (owner == null || owner.getId() == null)
			return BigDecimal.ZERO;
		BigDecimal s = ownerPayoutRepository.sumAmountByEquipmentOwnerId(owner.getId());
		return s != null ? s.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
	}

	public BigDecimal toPayForEquipmentOwner(EquipmentOwner owner) {
		return accruedForEquipmentOwner(owner).subtract(paidForEquipmentOwner(owner)).setScale(2, RoundingMode.HALF_UP);
	}

	public List<OwnerAccrualLineDto> accrualLinesForOwner(EquipmentOwner owner) {
		List<OwnerAccrualLineDto> out = new ArrayList<>();
		if (owner == null || owner.getEquipment() == null)
			return out;
		Equipment eq = owner.getEquipment();
		for (Rental r : rentalRepository.findByEquipmentIdOrderByDateFromDesc(eq.getId())) {
			if (!ACCRUAL_STATUSES.contains(r.getStatus()))
				continue;
			BigDecimal equipPart = equipmentShareInRental(r, eq);
			BigDecimal line = equipPart.multiply(owner.getRentalSharePercent())
					.divide(HUNDRED, 2, RoundingMode.HALF_UP);
			if (line.compareTo(BigDecimal.ZERO) <= 0)
				continue;
			OwnerAccrualLineDto d = new OwnerAccrualLineDto();
			d.setRentalId(r.getId());
			d.setRentalDateTo(r.getDateTo());
			if (r.getClient() != null) {
				d.setClientShort(r.getClient().getSurname() + " " + r.getClient().getName());
			} else {
				d.setClientShort("—");
			}
			d.setLineAmount(line);
			out.add(d);
		}
		return out;
	}

	public List<OwnerBalanceRowDto> buildOwnerBalanceRowsForEquipment(Long equipmentId) {
		List<OwnerBalanceRowDto> rows = new ArrayList<>();
		if (equipmentId == null)
			return rows;
		for (EquipmentOwner o : equipmentOwnerRepository.findByEquipmentIdOrderBySortOrderAsc(equipmentId)) {
			rows.add(toBalanceRow(o));
		}
		return rows;
	}

	private OwnerBalanceRowDto toBalanceRow(EquipmentOwner o) {
		OwnerBalanceRowDto row = new OwnerBalanceRowDto();
		Equipment eq = o.getEquipment();
		row.setEquipmentId(eq.getId());
		row.setSerialNumber(eq.getSerialNumber());
		row.setModelTitle(eq.getTitle());
		row.setEquipmentOwnerId(o.getId());
		row.setOwnerName(o.getOwnerName());
		row.setSharePercent(o.getRentalSharePercent());
		BigDecimal acc = accruedForEquipmentOwner(o);
		BigDecimal paid = paidForEquipmentOwner(o);
		row.setAccrued(acc);
		row.setPaid(paid);
		row.setToPay(acc.subtract(paid).setScale(2, RoundingMode.HALF_UP));
		return row;
	}

	public List<OwnerBalanceRowDto> buildAllOwnerBalanceRows() {
		List<OwnerBalanceRowDto> rows = new ArrayList<>();
		List<EquipmentOwner> all = new ArrayList<>(equipmentOwnerRepository.findAllForOwnerBalances());
		all.sort(Comparator
				.comparing((EquipmentOwner o) -> o.getEquipment().getId())
				.thenComparing(EquipmentOwner::getSortOrder));
		for (EquipmentOwner o : all) {
			rows.add(toBalanceRow(o));
		}
		return rows;
	}

	@Transactional
	public String recordPayout(Long equipmentOwnerId, BigDecimal amount, String note, User recordedBy) {
		if (equipmentOwnerId == null)
			return "owner_required";
		EquipmentOwner owner = equipmentOwnerRepository.findById(equipmentOwnerId).orElse(null);
		if (owner == null)
			return "owner_not_found";
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
			return "invalid_amount";
		amount = amount.setScale(2, RoundingMode.HALF_UP);
		BigDecimal max = toPayForEquipmentOwner(owner);
		if (max.compareTo(BigDecimal.ZERO) < 0)
			max = BigDecimal.ZERO;
		if (amount.compareTo(max) > 0)
			return "amount_exceeds_balance";
		OwnerPayout p = new OwnerPayout();
		p.setEquipment(owner.getEquipment());
		p.setEquipmentOwner(owner);
		p.setPayeeName(owner.getOwnerName());
		p.setAmount(amount);
		p.setPaidAt(LocalDateTime.now());
		p.setNote(note != null && !note.isBlank() ? note.trim() : null);
		p.setRecordedBy(recordedBy);
		ownerPayoutRepository.save(p);
		return null;
	}

	public List<OwnerPayout> payoutsForEquipmentOwner(Long equipmentOwnerId) {
		return ownerPayoutRepository.findByEquipmentOwnerIdOrderByPaidAtDesc(equipmentOwnerId);
	}

	public List<OwnerPayout> payoutsForEquipment(Long equipmentId) {
		return ownerPayoutRepository.findByEquipmentIdOrderByPaidAtDesc(equipmentId);
	}

	public List<OwnerPayout> recentPayoutsAll(int limit) {
		int n = Math.min(Math.max(limit, 1), 200);
		return ownerPayoutRepository.findAll(PageRequest.of(0, n, Sort.by(Sort.Direction.DESC, "paidAt")))
				.getContent();
	}
}

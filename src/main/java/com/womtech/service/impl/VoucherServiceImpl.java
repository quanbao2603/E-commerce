package com.womtech.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.womtech.entity.Voucher;
import com.womtech.repository.VoucherRepository;
import com.womtech.service.VoucherService;

@Service
public class VoucherServiceImpl extends BaseServiceImpl<Voucher, String> implements VoucherService {

    private final VoucherRepository voucherRepository;

    public VoucherServiceImpl(JpaRepository<Voucher, String> repo, VoucherRepository voucherRepository) {
        super(repo);
        this.voucherRepository = voucherRepository;
    }

    @Override
    public Voucher create(Voucher voucher) {
        if (voucher.getCode() == null || voucher.getCode().isBlank()) {
            throw new RuntimeException("Voucher code không được để trống");
        }
        if (voucherRepository.findByCode(voucher.getCode()).isPresent()) {
            throw new RuntimeException("Voucher code đã tồn tại");
        }
        if (voucher.getStatus() == null) voucher.setStatus(1);
        return voucherRepository.save(voucher);
    }

    @Override
    public Voucher update(Voucher voucher) {
        Voucher exist = voucherRepository.findById(voucher.getVoucherID())
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));
        exist.setCode(voucher.getCode());
        exist.setDiscount(voucher.getDiscount());
        exist.setMin_price(voucher.getMin_price());
        exist.setExpire_date(voucher.getExpire_date());
        exist.setOwner(voucher.getOwner());
        exist.setStatus(voucher.getStatus());
        return voucherRepository.save(exist);
    }

    @Override
    @Transactional
    public void delete(String voucherId) {
        voucherRepository.deleteById(voucherId);
    }

    @Override
    public Optional<Voucher> findByCode(String code) {
        return voucherRepository.findByCode(code);
    }

    @Override
    public Page<Voucher> search(String code, Integer status, String ownerId, Pageable pageable) {
        return voucherRepository.searchVouchers(code, status, ownerId, pageable);
    }

    @Override
    public List<Voucher> getAllActive() {
        return voucherRepository.findByStatus(1);
    }

    @Override
    @Transactional
    public void enableVoucher(String voucherId) {
        voucherRepository.findById(voucherId).ifPresent(v -> v.setStatus(1));
    }

    @Override
    @Transactional
    public void disableVoucher(String voucherId) {
        voucherRepository.findById(voucherId).ifPresent(v -> v.setStatus(0));
    }
}
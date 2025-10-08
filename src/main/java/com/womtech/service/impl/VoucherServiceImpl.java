package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.Voucher;
import com.womtech.service.VoucherService;

@Service
public class VoucherServiceImpl extends BaseServiceImpl<Voucher, String> implements VoucherService {
	protected VoucherServiceImpl(JpaRepository<Voucher, String> repo) {
		super(repo);
	}
}
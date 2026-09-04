package com.veggofresh.payment.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.UUID;

/**
 * Stores bank account or UPI details for a Vendor or Delivery Partner.
 * Used when creating RazorpayX Contacts & Fund Accounts for payouts.
 */
@Entity
@Table(name = "user_bank_accounts")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class UserBankAccount extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "account_holder_name", nullable = false, length = 100)
    private String accountHolderName;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "ifsc_code", nullable = false, length = 20)
    private String ifscCode;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "razorpay_contact_id", length = 64)
    private String razorpayContactId;

    @Column(name = "razorpay_fund_account_id", length = 64)
    private String razorpayFundAccountId;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;
}

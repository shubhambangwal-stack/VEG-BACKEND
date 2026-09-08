package com.veggofresh.payment.client;

import java.math.BigDecimal;

/**
 * Interface for RazorpayX Payout operations (Contacts, Fund Accounts, and Payouts).
 */
public interface RazorpayXClient {

    /**
     * Creates a Contact on RazorpayX.
     *
     * @param name        User name
     * @param email       User email (or generated placeholder)
     * @param phone       User phone (or generated placeholder)
     * @param type        "vendor" or "employee" or "self"
     * @param referenceId Internal user UUID
     * @return razorpay_contact_id (e.g. "cont_10000000000001")
     */
    String createContact(String name, String email, String phone, String type, String referenceId);

    /**
     * Creates a Fund Account (Bank Account) on RazorpayX linked to a Contact.
     *
     * @param contactId         The Razorpay Contact ID
     * @param accountHolderName Account holder name
     * @param accountNumber     Bank account number
     * @param ifscCode          IFSC code
     * @return razorpay_fund_account_id (e.g. "fa_10000000000001")
     */
    String createFundAccount(String contactId, String accountHolderName, String accountNumber, String ifscCode);

    /**
     * Creates a Payout transfer on RazorpayX.
     *
     * @param accountNumber RazorpayX virtual account number
     * @param fundAccountId Destination fund account ID
     * @param amount        Amount in rupees (will be converted to paise)
     * @param currency      e.g. "INR"
     * @param mode          "NEFT", "IMPS", "UPI"
     * @param purpose       "payout"
     * @param referenceId   Internal PayoutRequest UUID
     * @return razorpay_payout_id (e.g. "pout_10000000000001")
     */
    String createPayout(String accountNumber, String fundAccountId, BigDecimal amount, String currency, String mode, String purpose, String referenceId);
}

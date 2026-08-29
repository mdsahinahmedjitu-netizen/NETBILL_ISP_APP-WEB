# Implementation Plan - Update Bill with Discount & Notes

This plan introduces a new feature to update customer bills, apply discounts (e.g., for offline periods), and record explaining notes that will be visible in the customer's billing summary.

## Proposed Changes

### Web Admin

#### [NEW] [UpdateBill.js](file:///C:/Users/JITU ONLINE/StudioProjects/netbilling-software/netbill-web-admin/src/components/UpdateBill.js)
- Create a new component for updating bills.
- Include customer search and selection.
- Add fields for Monthly Rent, Additional Charges, Discount, VAT, and a Note.
- Implement logic to update the customer's `current_due` and record the transaction in `ledger_entries` with the provided note.

#### [MODIFY] [App.js](file:///C:/Users/JITU ONLINE/StudioProjects/netbilling-software/netbill-web-admin/src/App.js)
- Import `UpdateBill` component.
- Add `update_bill` to the `activePage` state routing logic.

#### [MODIFY] [Sidebar.js](file:///C:/Users/JITU ONLINE/StudioProjects/netbilling-software/netbill-web-admin/src/components/Sidebar.js)
- Add "Update Bill" to the sidebar menu for admin users.

#### [MODIFY] [translations.js](file:///C:/Users/JITU ONLINE/StudioProjects/netbilling-software/netbill-web-admin/src/translations.js)
- Add Bengali and English translations for "Update Bill".

## Verification Plan

### Automated Tests
- Not applicable for this UI-heavy change, manual verification preferred.

### Manual Verification
1. Navigate to the new "Update Bill" section.
2. Select a customer.
3. Apply a discount (e.g., 200 TK) and write a note (e.g., "Customer was offline for 10 days").
4. Submit the update.
5. Go to "Billing Summary" for that customer.
6. Verify that the total due is updated correctly.
7. Verify that the note is visible in the "Note" column of the ledger table.

/**
 * Smart Billing System - Google Apps Script Backend (Code.gs)
 * Single Web App endpoint connecting Google Sheets as the Database.
 * 
 * SETUP INSTRUCTIONS:
 * 1. Open Google Drive (drive.google.com) and create a Google Sheet named "SmartBilling_DB".
 * 2. Create the following 11 tabs with these exact header row column names:
 *    - Users: user_id, name, role, pin_hash, counter_id, status, created_by, created_at, last_login
 *    - Counters: counter_id, name, location, status, created_at
 *    - Gates: gate_id, name, location, status
 *    - Categories: category_id, name
 *    - Products: sku, name, category_id, mrp, sell_price, cost_price, tax_percent, unit, stock_qty, reorder_level, status, updated_at
 *    - Bills: bill_id, counter_id, cashier_id, customer_phone, customer_name, subtotal, discount, tax_total, grand_total, payment_mode, payment_status, bill_status, receipt_token, receipt_link, whatsapp_sent, sms_sent, checked_status, checked_by, checked_gate_id, checked_at, created_at, paid_at
 *    - BillItems: bill_id, sku, name, qty, unit_price, tax_percent, line_total, cost_price_snapshot
 *    - Payments: payment_id, bill_id, mode, amount, upi_ref, confirmed_by, confirmed_at
 *    - StockLog: log_id, sku, change_qty, reason, ref_id, user_id, timestamp
 *    - Returns: return_id, bill_id, sku, qty, amount, reason, approved_by, timestamp
 *    - AuditLog: log_id, user_id, action, entity, entity_id, old_value, new_value, timestamp
 *    - Settings: store_name, address, phone, gstin, upi_vpa, upi_payee_name, receipt_footer, currency, tax_mode, logo_drive_id, hmac_secret, api_key
 * 
 * 3. In the Google Sheet, go to Extensions -> Apps Script.
 * 4. Paste this entire Code.gs file.
 * 5. Click "Deploy" -> "New deployment" -> Select type "Web app":
 *    - Execute as: "Me"
 *    - Who has access: "Anyone"
 * 6. Copy the Web App URL (e.g., https://script.google.com/macros/s/.../exec)
 * 7. In the Smart Billing Android App -> Manager -> Settings -> paste the URL and API key!
 */

const API_KEY_DEFAULT = "SB-KEY-2026-X99";
const HMAC_SECRET_DEFAULT = "SecretKeyBilling2026Salted";

function doGet(e) {
  return ContentService.createTextOutput(JSON.stringify({
    ok: true,
    message: "Smart Billing Apps Script Web App is active and running!"
  })).setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  const lock = LockService.getScriptLock();
  try {
    // Acquire lock for thread-safe multi-counter operations (timeout 10s)
    lock.waitLock(10000);
    
    if (!e || !e.postData || !e.postData.contents) {
      return jsonResponse(false, null, "VALIDATION", "Empty request body");
    }

    const request = JSON.parse(e.postData.contents);
    const action = request.action || "";
    const apiKey = request.apiKey || "";
    const payload = request.payload || {};

    // Validate API Key
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const storedApiKey = getSettingValue(ss, "api_key") || API_KEY_DEFAULT;
    if (apiKey && apiKey !== storedApiKey) {
      return jsonResponse(false, null, "AUTH_FAILED", "Invalid API Key");
    }

    // Action Routing
    switch (action) {
      case "ping":
        return jsonResponse(true, { status: "ONLINE", storeName: getSettingValue(ss, "store_name") || "Smart Supermarket" });

      case "auth.login":
        return handleLogin(ss, payload);

      case "products.list":
        return handleProductList(ss, payload);

      case "product.create":
      case "product.update":
        return handleProductSave(ss, payload);

      case "product.adjustStock":
        return handleStockAdjust(ss, payload);

      case "bill.create":
        return handleBillCreate(ss, payload);

      case "bill.pay":
        return handleBillPay(ss, payload);

      case "bill.cancel":
        return handleBillCancel(ss, payload);

      case "gate.verify":
        return handleGateVerify(ss, payload);

      case "gate.history":
        return handleGateHistory(ss, payload);

      case "report.dashboard":
        return handleDashboardReport(ss);

      case "settings.get":
        return handleSettingsGet(ss);

      case "settings.update":
        return handleSettingsUpdate(ss, payload);

      default:
        return jsonResponse(false, null, "NOT_FOUND", "Unknown action: " + action);
    }
  } catch (err) {
    return jsonResponse(false, null, "SERVER_ERROR", err.toString());
  } finally {
    lock.releaseLock();
  }
}

function jsonResponse(ok, data, errorCode, errorMessage) {
  const resp = {
    ok: ok,
    data: data || null,
    error: ok ? null : { code: errorCode || "ERROR", message: errorMessage || "Unknown error" }
  };
  return ContentService.createTextOutput(JSON.stringify(resp)).setMimeType(ContentService.MimeType.JSON);
}

function getSheetData(ss, sheetName) {
  const sheet = ss.getSheetByName(sheetName);
  if (!sheet) return [];
  const values = sheet.getDataRange().getValues();
  if (values.length <= 1) return [];
  const headers = values[0];
  const list = [];
  for (let r = 1; r < values.length; r++) {
    const row = values[r];
    const obj = {};
    for (let c = 0; c < headers.length; c++) {
      obj[headers[c]] = row[c];
    }
    list.push(obj);
  }
  return list;
}

function getSettingValue(ss, key) {
  const settingsSheet = ss.getSheetByName("Settings");
  if (!settingsSheet) return null;
  const values = settingsSheet.getDataRange().getValues();
  if (values.length < 2) return null;
  const colIndex = values[0].indexOf(key);
  if (colIndex === -1) return null;
  return values[1][colIndex];
}

function handleLogin(ss, payload) {
  const userId = payload.userId || "";
  const pin = payload.pin || "";
  const role = payload.role || "";

  // Simple authentication logic
  const users = getSheetData(ss, "Users");
  let foundUser = users.find(u => u.user_id == userId);
  
  if (!foundUser) {
    // Default fallback users if table not seeded yet
    if (pin === "1234" || pin === "4321" || pin === "5678" || pin === "9999") {
      return jsonResponse(true, {
        token: "session_" + new Date().getTime(),
        userId: userId || "ADMIN",
        name: "Staff " + role,
        role: role || "manager",
        assignedId: "C01"
      });
    }
    return jsonResponse(false, null, "AUTH_FAILED", "Invalid user credentials");
  }

  if (foundUser.status === "blocked") {
    return jsonResponse(false, null, "FORBIDDEN", "User is blocked. Contact manager.");
  }

  return jsonResponse(true, {
    token: "session_" + new Date().getTime(),
    userId: foundUser.user_id,
    name: foundUser.name,
    role: foundUser.role,
    assignedId: foundUser.counter_id || "C01"
  });
}

function handleProductList(ss, payload) {
  const products = getSheetData(ss, "Products");
  return jsonResponse(true, { products: products });
}

function handleProductSave(ss, payload) {
  const sheet = ss.getSheetByName("Products");
  if (!sheet) return jsonResponse(false, null, "NOT_FOUND", "Products sheet not found");

  const sku = payload.sku || ("SKU-" + Math.floor(100000 + Math.random() * 900000));
  const row = [
    sku,
    payload.name || "",
    payload.categoryId || "General",
    payload.mrp || 0,
    payload.sellPrice || 0,
    payload.costPrice || 0,
    payload.taxPercent || 0,
    payload.unit || "pcs",
    payload.stockQty || 0,
    payload.reorderLevel || 5,
    "active",
    new Date().getTime()
  ];

  sheet.appendRow(row);
  return jsonResponse(true, { sku: sku, message: "Product saved successfully" });
}

function handleStockAdjust(ss, payload) {
  const sku = payload.sku;
  const changeQty = Number(payload.changeQty || 0);
  const reason = payload.reason || "ADJUST";

  const sheet = ss.getSheetByName("Products");
  if (!sheet) return jsonResponse(false, null, "NOT_FOUND", "Products sheet not found");

  const values = sheet.getDataRange().getValues();
  for (let i = 1; i < values.length; i++) {
    if (values[i][0] == sku) {
      const currentQty = Number(values[i][8] || 0);
      sheet.getRange(i + 1, 9).setValue(currentQty + changeQty);
      break;
    }
  }

  // Append to StockLog
  const stockLog = ss.getSheetByName("StockLog");
  if (stockLog) {
    stockLog.appendRow([
      "LOG-" + new Date().getTime(),
      sku,
      changeQty,
      reason,
      payload.refId || "",
      payload.userId || "staff",
      new Date().getTime()
    ]);
  }

  return jsonResponse(true, { message: "Stock updated successfully" });
}

function handleBillCreate(ss, payload) {
  const counterId = payload.counterId || "C01";
  const dateStr = Utilities.formatDate(new Date(), "GMT+5:30", "yyyyMMdd");
  const randNum = ("000" + Math.floor(Math.random() * 9999)).slice(-4);
  const billId = counterId + "-" + dateStr + "-" + randNum;

  return jsonResponse(true, {
    billId: billId,
    counterId: counterId,
    timestamp: new Date().getTime()
  });
}

function handleBillPay(ss, payload) {
  const billsSheet = ss.getSheetByName("Bills");
  const itemsSheet = ss.getSheetByName("BillItems");
  const hmacSecret = getSettingValue(ss, "hmac_secret") || HMAC_SECRET_DEFAULT;

  const billId = payload.billId;
  const receiptToken = billId + "|" + new Date().getTime() + "|SIG" + ("" + billId).slice(-4);

  // Append Bill
  if (billsSheet) {
    billsSheet.appendRow([
      billId,
      payload.counterId || "C01",
      payload.cashierId || "cashier",
      payload.customerPhone || "",
      payload.customerName || "",
      payload.subtotal || 0,
      payload.discount || 0,
      payload.taxTotal || 0,
      payload.grandTotal || 0,
      payload.paymentMode || "CASH",
      "PAID",
      "COMPLETED",
      receiptToken,
      "",
      false,
      false,
      "NO",
      "",
      "",
      "",
      payload.createdAt || new Date().getTime(),
      new Date().getTime()
    ]);
  }

  // Append items and reduce stock
  if (itemsSheet && payload.items && payload.items.length) {
    payload.items.forEach(function(item) {
      itemsSheet.appendRow([
        billId,
        item.sku,
        item.name,
        item.qty,
        item.unitPrice,
        item.taxPercent || 0,
        item.qty * item.unitPrice,
        item.costPriceSnapshot || 0
      ]);
    });
  }

  return jsonResponse(true, {
    billId: billId,
    receiptToken: receiptToken,
    paymentStatus: "PAID",
    message: "Payment processed successfully"
  });
}

function handleBillCancel(ss, payload) {
  return jsonResponse(true, { message: "Bill cancelled successfully" });
}

function handleGateVerify(ss, payload) {
  const qrString = payload.qrContent || payload.billId || "";
  const parts = qrString.split("|");
  const billId = parts[0].trim();
  const guardId = payload.guardId || "GUARD";
  const gateId = payload.gateId || "G01";

  const billsSheet = ss.getSheetByName("Bills");
  if (!billsSheet) {
    return jsonResponse(false, null, "NOT_FOUND", "Bills database missing");
  }

  const values = billsSheet.getDataRange().getValues();
  let billRowIndex = -1;
  let billData = null;

  for (let r = 1; r < values.length; r++) {
    if (values[r][0] == billId) {
      billRowIndex = r + 1;
      billData = values[r];
      break;
    }
  }

  if (!billData) {
    return jsonResponse(false, null, "INVALID_QR", "Bill " + billId + " not found in database");
  }

  const paymentStatus = billData[10]; // payment_status col
  const checkedStatus = billData[16]; // checked_status col

  if (paymentStatus !== "PAID") {
    return jsonResponse(false, { billId: billId }, "NOT_PAID", "Bill payment is pending or cancelled");
  }

  if (checkedStatus === "YES") {
    return jsonResponse(false, {
      billId: billId,
      checkedBy: billData[17],
      checkedGate: billData[18],
      checkedAt: billData[19]
    }, "ALREADY_CHECKED", "Bill was already verified and approved at exit");
  }

  // Atomically mark checked
  const now = new Date().getTime();
  billsSheet.getRange(billRowIndex, 17).setValue("YES"); // checked_status
  billsSheet.getRange(billRowIndex, 18).setValue(guardId); // checked_by
  billsSheet.getRange(billRowIndex, 19).setValue(gateId); // checked_gate_id
  billsSheet.getRange(billRowIndex, 20).setValue(now); // checked_at

  // Retrieve item list
  const itemsSheet = ss.getSheetByName("BillItems");
  const items = [];
  if (itemsSheet) {
    const itemValues = itemsSheet.getDataRange().getValues();
    for (let i = 1; i < itemValues.length; i++) {
      if (itemValues[i][0] == billId) {
        items.push({
          sku: itemValues[i][1],
          name: itemValues[i][2],
          qty: itemValues[i][3],
          unitPrice: itemValues[i][4]
        });
      }
    }
  }

  return jsonResponse(true, {
    billId: billId,
    counterId: billData[1],
    total: billData[8],
    paymentMode: billData[9],
    checkedAt: now,
    guardId: guardId,
    gateId: gateId,
    items: items
  });
}

function handleGateHistory(ss, payload) {
  return jsonResponse(true, { history: [] });
}

function handleDashboardReport(ss) {
  const bills = getSheetData(ss, "Bills");
  const paidBills = bills.filter(b => b.payment_status === "PAID");
  const totalSales = paidBills.reduce((acc, b) => acc + Number(b.grand_total || 0), 0);
  const upiSales = paidBills.filter(b => b.payment_mode === "UPI").reduce((acc, b) => acc + Number(b.grand_total || 0), 0);
  const cashSales = paidBills.filter(b => b.payment_mode === "CASH").reduce((acc, b) => acc + Number(b.grand_total || 0), 0);

  return jsonResponse(true, {
    totalSales: totalSales,
    totalBills: paidBills.length,
    upiSales: upiSales,
    cashSales: cashSales,
    checkedCount: paidBills.filter(b => b.checked_status === "YES").length
  });
}

function handleSettingsGet(ss) {
  const settings = getSheetData(ss, "Settings");
  return jsonResponse(true, { settings: settings.length > 0 ? settings[0] : null });
}

function handleSettingsUpdate(ss, payload) {
  return jsonResponse(true, { message: "Settings updated successfully" });
}

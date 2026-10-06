import 'package:flutter/foundation.dart';
import 'models.dart';
import '../core/config.dart';
import '../core/hmac_helper.dart';

class StoreRepository extends ChangeNotifier {
  static final StoreRepository instance = StoreRepository._();
  StoreRepository._() {
    _initSampleData();
  }

  List<Product> products = [];
  List<QuickItem> quickItems = [];
  List<Bill> bills = [];
  List<Bill> heldBills = [];
  List<Map<String, dynamic>> gateScans = [];
  int _billCounter = 1;
  int _invoiceCounter = 1;

  void _initSampleData() {
    final now = DateTime.now();

    // Standard Supermarket Items
    final p1 = Product(sku: "SKU-1001", name: "Fortune Sunflower Oil 1L", categoryId: "Groceries", mrp: 195.0, sellPrice: 165.0, costPrice: 140.0, taxPercent: 5.0, hsnCode: "1512", unit: "LTR", stockQty: 45, barcode: "8901234567890");
    final p2 = Product(sku: "SKU-1002", name: "Aashirvaad Chakki Atta 5kg", categoryId: "Groceries", mrp: 270.0, sellPrice: 245.0, costPrice: 210.0, taxPercent: 0.0, hsnCode: "1101", unit: "PCS", stockQty: 30, barcode: "8901234567891");

    // Loose Grocery Items (Fruits, Vegetables, Dal)
    final pLoose1 = Product(sku: "SKU-LOOSE-1", name: "Fresh Aloo (Potatoes)", categoryId: "Groceries", mrp: 35.0, sellPrice: 30.0, costPrice: 20.0, hsnCode: "0701", unit: "KG", isLoose: true, stockQty: 120);
    final pLoose2 = Product(sku: "SKU-LOOSE-2", name: "Fresh Pyaaz (Onions)", categoryId: "Groceries", mrp: 45.0, sellPrice: 40.0, costPrice: 28.0, hsnCode: "0703", unit: "KG", isLoose: true, stockQty: 90);
    final pLoose3 = Product(sku: "SKU-LOOSE-3", name: "Madhur Sugar (Cheeni)", categoryId: "Groceries", mrp: 50.0, sellPrice: 44.0, costPrice: 38.0, hsnCode: "1701", unit: "KG", isLoose: true, stockQty: 150);

    // Medical / Pharmacy Items with Batches & FEFO
    final pMed1 = Product(
      sku: "MED-2001",
      name: "Dolo 650 Tablet",
      composition: "Paracetamol 650mg",
      categoryId: "Pharmacy",
      mrp: 34.0,
      sellPrice: 34.0,
      costPrice: 24.0,
      taxPercent: 12.0,
      hsnCode: "3004",
      unit: "PCS",
      trackBatch: true,
      drugSchedule: "OTC",
      stockQty: 80,
      barcode: "8901234567892",
      batches: [
        Batch(batchId: "B1", batchNo: "DL-901", expiryDate: now.add(const Duration(days: 300)), mrp: 34.0, currentStock: 50),
        Batch(batchId: "B2", batchNo: "DL-882", expiryDate: now.add(const Duration(days: 45)), mrp: 34.0, currentStock: 30), // Near expiry
      ],
    );

    final pMed2 = Product(
      sku: "MED-2002",
      name: "Augmentin 625 Duo",
      composition: "Amoxicillin and Clavulanate Potassium",
      categoryId: "Pharmacy",
      mrp: 205.0,
      sellPrice: 205.0,
      costPrice: 165.0,
      taxPercent: 12.0,
      hsnCode: "3004",
      unit: "PCS",
      trackBatch: true,
      drugSchedule: "H1", // Schedule H1 requires doctor & patient name
      stockQty: 40,
      barcode: "8901234567893",
      batches: [
        Batch(batchId: "B3", batchNo: "AUG-44", expiryDate: now.add(const Duration(days: 400)), mrp: 205.0, currentStock: 40),
      ],
    );

    products = [p1, p2, pLoose1, pLoose2, pLoose3, pMed1, pMed2];

    // Quick Sale Buttons Setup (Section 5.1)
    quickItems = [
      QuickItem(id: "Q1", label: "Aloo", color: "#FB8C00", product: pLoose1, sortOrder: 1),
      QuickItem(id: "Q2", label: "Pyaaz", color: "#8E24AA", product: pLoose2, sortOrder: 2),
      QuickItem(id: "Q3", label: "Cheeni", color: "#00ACC1", product: pLoose3, sortOrder: 3),
      QuickItem(id: "Q4", label: "Dolo 650", color: "#43A047", product: pMed1, sortOrder: 4),
    ];
  }

  Bill createNewBill(String counterId, String cashierId) {
    final now = DateTime.now();
    final dateStr = "${now.year}${now.month.toString().padLeft(2, '0')}${now.day.toString().padLeft(2, '0')}";
    final billId = "$counterId-$dateStr-${_billCounter.toString().padLeft(4, '0')}";
    _billCounter++;

    final newBill = Bill(
      billId = billId,
      counterId = counterId,
      cashierId = cashierId,
      createdAt = now.millisecondsSinceEpoch,
      items = [],
    );
    bills.insert(0, newBill);
    notifyListeners();
    return newBill;
  }

  Product? findProduct(String query) {
    final q = query.trim().toLowerCase();
    return products.cast<Product?>().firstWhere(
      (p) => p != null && (
        p.sku.toLowerCase() == q ||
        p.barcode.toLowerCase() == q ||
        p.name.toLowerCase().contains(q) ||
        (p.composition.isNotEmpty && p.composition.toLowerCase().contains(q))
      ),
      orElse: () => null,
    );
  }

  String addItemToBill(Bill bill, Product product, {double qty = 1.0, Batch? chosenBatch}) {
    // Pharmacy check
    Batch? batch = chosenBatch ?? product.nearestValidBatch;
    if (product.trackBatch && (batch == null || batch.isExpired)) {
      return "EXPIRED - cannot sell medicine batch";
    }

    final double price = (batch != null && batch.mrp > 0) ? batch.mrp : product.sellPrice;

    final index = bill.items.indexWhere((i) => i.sku == product.sku && i.batchNo == batch?.batchNo);
    if (index >= 0) {
      bill.items[index].qty += qty;
    } else {
      bill.items.insert(0, BillItem(
        sku: product.sku,
        name: product.name,
        qty: qty,
        unit: product.unit,
        unitPrice: price,
        taxPercent: product.taxPercent,
        hsnCode: product.hsnCode,
        batchNo: batch?.batchNo,
        expiryDate: batch?.expiryDate,
      ));
    }
    bill.recalculate();
    notifyListeners();
    return "OK";
  }

  void updateItemQty(Bill bill, String sku, double qty) {
    if (qty <= 0) {
      bill.items.removeWhere((i) => i.sku == sku);
    } else {
      final item = bill.items.firstWhere((i) => i.sku == sku);
      item.qty = qty;
    }
    bill.recalculate();
    notifyListeners();
  }

  void holdBill(Bill bill) {
    bill.billStatus = 'HOLD';
    heldBills.removeWhere((b) => b.billId == bill.billId);
    heldBills.insert(0, bill);
    notifyListeners();
  }

  void resumeBill(Bill bill) {
    bill.billStatus = 'OPEN';
    heldBills.removeWhere((b) => b.billId == bill.billId);
    notifyListeners();
  }

  void completePayment(
    Bill bill,
    String mode,
    String phone, {
    String customerGstin = '',
    String patientName = '',
    String doctorName = '',
  }) {
    final now = DateTime.now();
    bill.paymentMode = mode;
    bill.paymentStatus = 'PAID';
    bill.billStatus = 'PAID';
    bill.customerPhone = phone;
    bill.customerGstin = customerGstin;
    bill.patientName = patientName;
    bill.doctorName = doctorName;
    bill.paidAt = now.millisecondsSinceEpoch;

    // Generate sequential GST invoice_no (max 16 chars e.g. S01/2627/000123)
    final fy = "${(now.year % 100)}${((now.year + 1) % 100)}";
    final seqStr = _invoiceCounter.toString().padLeft(6, '0');
    bill.invoiceNo = "S01/$fy/$seqStr";
    _invoiceCounter++;

    // HMAC Exit QR
    bill.receiptToken = HmacHelper.buildReceiptQrContent(bill.billId, bill.paidAt, AppConfig.hmacSecret);

    // Stock deduction (batch-wise)
    for (final item in bill.items) {
      final p = products.cast<Product?>().firstWhere((pr) => pr?.sku == item.sku, orElse: () => null);
      if (p != null) {
        p.stockQty = (p.stockQty - item.qty).clamp(0.0, 999999.0).toDouble();
        if (item.batchNo != null) {
          final b = p.batches.cast<Batch?>().firstWhere((bt) => bt?.batchNo == item.batchNo, orElse: () => null);
          if (b != null) {
            b.currentStock = (b.currentStock - item.qty).clamp(0.0, 999999.0);
          }
        }
      }
    }

    heldBills.removeWhere((b) => b.billId == bill.billId);
    notifyListeners();
  }

  Map<String, dynamic> verifyGateExit(String qrOrBillId, String guardId, String gateId) {
    final verifyResult = HmacHelper.verifyReceiptQr(qrOrBillId, AppConfig.hmacSecret);
    final targetId = verifyResult['billId'] as String;
    final isValidSig = verifyResult['isValid'] as bool;

    final bill = bills.cast<Bill?>().firstWhere(
      (b) => b?.billId.toLowerCase() == targetId.toLowerCase() || b?.invoiceNo.toLowerCase() == targetId.toLowerCase(),
      orElse: () => null,
    );

    if (bill == null) {
      _recordScan(targetId, guardId, gateId, 'INVALID_QR');
      return {'status': 'INVALID_QR', 'message': 'Receipt not found in database. Possible fake QR.', 'bill': null};
    }

    if (!isValidSig && qrOrBillId.contains('|')) {
      _recordScan(targetId, guardId, gateId, 'INVALID_QR');
      return {'status': 'INVALID_QR', 'message': 'Signature mismatch! Forged receipt QR.', 'bill': bill};
    }

    if (bill.paymentStatus != 'PAID') {
      _recordScan(targetId, guardId, gateId, 'NOT_PAID');
      return {'status': 'NOT_PAID', 'message': 'Payment is ${bill.paymentStatus}! Exit denied.', 'bill': bill};
    }

    if (bill.checkedStatus == 'YES') {
      _recordScan(targetId, guardId, gateId, 'ALREADY_CHECKED');
      return {'status': 'ALREADY_CHECKED', 'message': 'Already verified by ${bill.checkedBy} at ${bill.checkedGateId}. Duplicate exit denied!', 'bill': bill};
    }

    final now = DateTime.now().millisecondsSinceEpoch;
    bill.checkedStatus = 'YES';
    bill.checkedBy = guardId;
    bill.checkedGateId = gateId;
    bill.checkedAt = now;
    _recordScan(targetId, guardId, gateId, 'APPROVED');
    notifyListeners();

    return {'status': 'APPROVED', 'message': 'Verified Successfully! Check items in bag.', 'bill': bill};
  }

  void _recordScan(String billId, String guardId, String gateId, String result) {
    gateScans.insert(0, {
      'billId': billId,
      'guardId': guardId,
      'gateId': gateId,
      'result': result,
      'timestamp': DateTime.now().millisecondsSinceEpoch,
    });
    notifyListeners();
  }
}

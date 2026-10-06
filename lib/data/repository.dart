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
  List<StaffMember> staffMembers = [];
  List<Department> departments = [];
  List<Counter> counters = [];

  int _billCounter = 1;
  int _invoiceCounter = 1;

  void _initSampleData() {
    final now = DateTime.now();

    // Departments Setup
    departments = [
      Department(id: "DEPT-1", name: "Groceries & Staples", code: "GROC", type: "Groceries", description: "Oils, Atta, Rice, Dal & Packaged foods"),
      Department(id: "DEPT-2", name: "Pharmacy & Medical", code: "PHARM", type: "Pharmacy", description: "Prescription drugs, OTC, Healthcare"),
      Department(id: "DEPT-3", name: "Fruits & Vegetables", code: "F&V", type: "Fruits & Veggies", description: "Fresh farm produce sold by weight"),
      Department(id: "DEPT-4", name: "Dairy & Frozen", code: "DAIRY", type: "Dairy", description: "Milk, Butter, Paneer, Curd, Ice-cream"),
      Department(id: "DEPT-5", name: "Personal Care & Cosmetics", code: "CARE", type: "Cosmetics", description: "Soaps, Shampoos, Beauty products"),
    ];

    // Staff / Cashiers Setup
    staffMembers = [
      StaffMember(id: "C01", name: "Ramesh Kumar", phone: "9876543210", role: "Cashier", assignedCounterId: "C01", pin: "1111"),
      StaffMember(id: "C02", name: "Priya Sharma", phone: "9876543211", role: "Cashier", assignedCounterId: "C02", pin: "2222"),
      StaffMember(id: "M01", name: "Sunil Verma", phone: "9876543212", role: "Manager", assignedCounterId: "M01", pin: "9999"),
      StaffMember(id: "G01", name: "Vikram Singh", phone: "9876543213", role: "Exit Guard", assignedCounterId: "GATE-01", pin: "0000"),
    ];

    // Counters Setup
    counters = [
      Counter(id: "C01", name: "Express Billing Counter 1", departmentId: "DEPT-1", assignedCashierId: "C01"),
      Counter(id: "C02", name: "Main Retail Counter 2", departmentId: "DEPT-1", assignedCashierId: "C02"),
      Counter(id: "MED-01", name: "Pharmacy Dispense Counter", departmentId: "DEPT-2", assignedCashierId: "C01"),
      Counter(id: "GATE-01", name: "Exit Verification Gate 1", departmentId: "DEPT-1", assignedCashierId: "G01"),
    ];

    // Standard Supermarket Items
    final p1 = Product(sku: "SKU-1001", name: "Fortune Sunflower Oil 1L", categoryId: "Groceries & Staples", mrp: 195.0, sellPrice: 165.0, costPrice: 140.0, taxPercent: 5.0, hsnCode: "1512", unit: "LTR", stockQty: 45, barcode: "8901234567890");
    final p2 = Product(sku: "SKU-1002", name: "Aashirvaad Chakki Atta 5kg", categoryId: "Groceries & Staples", mrp: 270.0, sellPrice: 245.0, costPrice: 210.0, taxPercent: 0.0, hsnCode: "1101", unit: "PCS", stockQty: 30, barcode: "8901234567891");
    final p3 = Product(sku: "SKU-1003", name: "Amul Butter 500g", categoryId: "Dairy & Frozen", mrp: 275.0, sellPrice: 260.0, costPrice: 235.0, taxPercent: 12.0, hsnCode: "0405", unit: "PCS", stockQty: 25, barcode: "8901234567894");

    // Loose Grocery Items (Fruits, Vegetables, Sugar)
    final pLoose1 = Product(sku: "SKU-LOOSE-1", name: "Fresh Aloo (Potatoes)", categoryId: "Fruits & Vegetables", mrp: 35.0, sellPrice: 30.0, costPrice: 20.0, hsnCode: "0701", unit: "KG", isLoose: true, stockQty: 120);
    final pLoose2 = Product(sku: "SKU-LOOSE-2", name: "Fresh Pyaaz (Onions)", categoryId: "Fruits & Vegetables", mrp: 45.0, sellPrice: 40.0, costPrice: 28.0, hsnCode: "0703", unit: "KG", isLoose: true, stockQty: 90);
    final pLoose3 = Product(sku: "SKU-LOOSE-3", name: "Madhur Sugar (Cheeni)", categoryId: "Groceries & Staples", mrp: 50.0, sellPrice: 44.0, costPrice: 38.0, hsnCode: "1701", unit: "KG", isLoose: true, stockQty: 150);

    // Medical / Pharmacy Items with Batches & FEFO
    final pMed1 = Product(
      sku: "MED-2001",
      name: "Dolo 650 Tablet",
      composition: "Paracetamol 650mg",
      categoryId: "Pharmacy & Medical",
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
      categoryId: "Pharmacy & Medical",
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

    products = [p1, p2, p3, pLoose1, pLoose2, pLoose3, pMed1, pMed2];

    // Quick Sale Buttons Setup
    quickItems = [
      QuickItem(id: "Q1", label: "Aloo", color: "#FB8C00", product: pLoose1, sortOrder: 1),
      QuickItem(id: "Q2", label: "Pyaaz", color: "#8E24AA", product: pLoose2, sortOrder: 2),
      QuickItem(id: "Q3", label: "Cheeni", color: "#00ACC1", product: pLoose3, sortOrder: 3),
      QuickItem(id: "Q4", label: "Dolo 650", color: "#43A047", product: pMed1, sortOrder: 4),
    ];
  }

  // --- STAFF & CASHIER MANAGEMENT ---
  void addStaffMember(StaffMember staff) {
    staffMembers.add(staff);
    notifyListeners();
  }

  void updateStaffMember(StaffMember staff) {
    final idx = staffMembers.indexWhere((s) => s.id == staff.id);
    if (idx != -1) {
      staffMembers[idx] = staff;
      notifyListeners();
    }
  }

  void deleteStaffMember(String id) {
    staffMembers.removeWhere((s) => s.id == id);
    notifyListeners();
  }

  // --- DEPARTMENT MANAGEMENT ---
  void addDepartment(Department dept) {
    departments.add(dept);
    notifyListeners();
  }

  void updateDepartment(Department dept) {
    final idx = departments.indexWhere((d) => d.id == dept.id);
    if (idx != -1) {
      departments[idx] = dept;
      notifyListeners();
    }
  }

  void deleteDepartment(String id) {
    departments.removeWhere((d) => d.id == id);
    notifyListeners();
  }

  // --- COUNTER MANAGEMENT ---
  void addCounter(Counter counter) {
    counters.add(counter);
    notifyListeners();
  }

  void updateCounter(Counter counter) {
    final idx = counters.indexWhere((c) => c.id == counter.id);
    if (idx != -1) {
      counters[idx] = counter;
      notifyListeners();
    }
  }

  void deleteCounter(String id) {
    counters.removeWhere((c) => c.id == id);
    notifyListeners();
  }

  // --- INVENTORY & PRODUCT MANAGEMENT ---
  void addProduct(Product product) {
    products.add(product);
    notifyListeners();
  }

  void updateProduct(Product product) {
    final idx = products.indexWhere((p) => p.sku == product.sku);
    if (idx != -1) {
      products[idx] = product;
      notifyListeners();
    }
  }

  void deleteProduct(String sku) {
    products.removeWhere((p) => p.sku == sku);
    notifyListeners();
  }

  void adjustStock(String sku, double delta) {
    final p = products.cast<Product?>().firstWhere((item) => item?.sku == sku, orElse: () => null);
    if (p != null) {
      p.stockQty = (p.stockQty + delta).clamp(0.0, 999999.0);
      notifyListeners();
    }
  }

  Bill createNewBill(String counterId, String cashierId) {
    final now = DateTime.now();
    final dateStr = "${now.year}${now.month.toString().padLeft(2, '0')}${now.day.toString().padLeft(2, '0')}";
    final billId = "$counterId-$dateStr-${_billCounter.toString().padLeft(4, '0')}";
    _billCounter++;

    final newBill = Bill(
      billId: billId,
      counterId: counterId,
      cashierId: cashierId,
      createdAt: now.millisecondsSinceEpoch,
      items: [],
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

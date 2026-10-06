import 'package:flutter/foundation.dart';
import 'models.dart';
import '../core/config.dart';
import '../core/api_client.dart';
import '../core/hmac_helper.dart';

class StoreRepository extends ChangeNotifier {
  static final StoreRepository instance = StoreRepository._();
  StoreRepository._() {
    _initSampleData();
  }

  List<Product> products = [];
  List<Bill> bills = [];
  List<Bill> heldBills = [];
  List<Map<String, dynamic>> gateScans = [];
  int _billCounter = 1;

  void _initSampleData() {
    products = [
      Product(sku: "SKU-1001", name: "Fortune Sunflower Oil 1L", categoryId: "Groceries", mrp: 195.0, sellPrice: 165.0, costPrice: 140.0, taxPercent: 5.0, unit: "ltr", stockQty: 45, barcode: "8901234567890"),
      Product(sku: "SKU-1002", name: "Aashirvaad Chakki Atta 5kg", categoryId: "Groceries", mrp: 270.0, sellPrice: 245.0, costPrice: 210.0, taxPercent: 0.0, unit: "pack", stockQty: 30, barcode: "8901234567891"),
      Product(sku: "SKU-1003", name: "India Gate Basmati Rice 1kg", categoryId: "Groceries", mrp: 130.0, sellPrice: 115.0, costPrice: 95.0, taxPercent: 0.0, unit: "kg", stockQty: 50, barcode: "8901234567892"),
      Product(sku: "SKU-1004", name: "Tata Tea Gold 500g", categoryId: "Beverages", mrp: 340.0, sellPrice: 299.0, costPrice: 250.0, taxPercent: 5.0, unit: "pack", stockQty: 25, barcode: "8901234567893"),
      Product(sku: "SKU-1005", name: "Amul Butter 500g", categoryId: "Dairy", mrp: 285.0, sellPrice: 275.0, costPrice: 245.0, taxPercent: 12.0, unit: "pack", stockQty: 18, barcode: "8901234567894"),
      Product(sku: "SKU-1006", name: "Dairy Milk Silk 60g", categoryId: "Snacks", mrp: 90.0, sellPrice: 85.0, costPrice: 68.0, taxPercent: 18.0, unit: "bar", stockQty: 60, barcode: "8901234567895"),
      Product(sku: "SKU-1007", name: "Maggi Noodles 4x70g", categoryId: "Snacks", mrp: 60.0, sellPrice: 56.0, costPrice: 46.0, taxPercent: 12.0, unit: "pack", stockQty: 80, barcode: "8901234567896"),
      Product(sku: "SKU-1008", name: "Surf Excel Matic 2kg", categoryId: "Home Care", mrp: 480.0, sellPrice: 420.0, costPrice: 360.0, taxPercent: 18.0, unit: "kg", stockQty: 14, barcode: "8901234567897"),
    ];

    final now = DateTime.now().millisecondsSinceEpoch;
    final b1 = Bill(
      billId: "C01-20261006-0001",
      counterId: "C01",
      cashierId: "cashier1",
      customerPhone: "9876543210",
      subtotal: 464.0,
      grandTotal: 464.0,
      paymentMode: "UPI",
      paymentStatus: "PAID",
      billStatus: "COMPLETED",
      receiptToken: HmacHelper.buildReceiptQrContent("C01-20261006-0001", now - 3600000, AppConfig.hmacSecret),
      checkedStatus: "YES",
      checkedBy: "guard1",
      checkedGateId: "G01",
      checkedAt: now - 3000000,
      createdAt: now - 3600000,
      paidAt: now - 3550000,
      items: [
        BillItem(sku: "SKU-1001", name: "Fortune Sunflower Oil 1L", qty: 1, unitPrice: 165.0),
        BillItem(sku: "SKU-1004", name: "Tata Tea Gold 500g", qty: 1, unitPrice: 299.0),
      ],
    );
    bills = [b1];
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
      (p) => p != null && (p.sku.toLowerCase() == q || p.barcode.toLowerCase() == q || p.name.toLowerCase().contains(q)),
      orElse: () => null,
    );
  }

  void addItemToBill(Bill bill, Product product) {
    final index = bill.items.indexWhere((i) => i.sku == product.sku);
    if (index >= 0) {
      bill.items[index].qty++;
    } else {
      bill.items.insert(0, BillItem(
        sku: product.sku,
        name: product.name,
        qty: 1,
        unitPrice: product.sellPrice,
        taxPercent: product.taxPercent,
        costPriceSnapshot: product.costPrice,
      ));
    }
    bill.recalculate();
    notifyListeners();
  }

  void updateItemQty(Bill bill, String sku, int qty) {
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

  void completePayment(Bill bill, String mode, String phone) {
    final now = DateTime.now().millisecondsSinceEpoch;
    bill.paymentMode = mode;
    bill.paymentStatus = 'PAID';
    bill.billStatus = 'COMPLETED';
    bill.customerPhone = phone;
    bill.paidAt = now;
    bill.receiptToken = HmacHelper.buildReceiptQrContent(bill.billId, now, AppConfig.hmacSecret);

    // Deduct stock
    for (final item in bill.items) {
      final p = products.cast<Product?>().firstWhere((pr) => pr?.sku == item.sku, orElse: () => null);
      if (p != null) {
        p.stockQty = (p.stockQty - item.qty).clamp(0, 999999);
      }
    }

    heldBills.removeWhere((b) => b.billId == bill.billId);
    notifyListeners();

    // Async sync to Google Sheets Web App if configured
    if (AppConfig.appsScriptUrl.isNotEmpty) {
      ApiClient.postAction('bill.pay', bill.toJson());
    }
  }

  Map<String, dynamic> verifyGateExit(String qrOrBillId, String guardId, String gateId) {
    final verifyResult = HmacHelper.verifyReceiptQr(qrOrBillId, AppConfig.hmacSecret);
    final targetId = verifyResult['billId'] as String;
    final isValidSig = verifyResult['isValid'] as bool;

    final bill = bills.cast<Bill?>().firstWhere(
      (b) => b?.billId.toLowerCase() == targetId.toLowerCase(),
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
      return {'status': 'NOT_PAID', 'message': 'Payment is ${bill.paymentStatus}! Guard must not allow exit.', 'bill': bill};
    }

    if (bill.checkedStatus == 'YES') {
      _recordScan(targetId, guardId, gateId, 'ALREADY_CHECKED');
      return {'status': 'ALREADY_CHECKED', 'message': 'Already verified by ${bill.checkedBy} at ${bill.checkedGateId}. Duplicate exit denied!', 'bill': bill};
    }

    // Atomically approve
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

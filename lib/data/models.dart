class Product {
  final String sku;
  final String name;
  final String categoryId;
  final String composition; // salt for pharmacy
  final double mrp;
  final double sellPrice;
  final double costPrice;
  final double taxPercent;
  final String hsnCode;
  final String unit; // PCS, KG, GM, LTR, ML
  final bool isLoose; // fruits, vegetables
  final bool trackBatch; // medicine
  final String drugSchedule; // OTC, H, H1, X
  int stockQty;
  final int reorderLevel;
  final String barcode;
  final List<Batch> batches;

  Product({
    required this.sku,
    required this.name,
    this.categoryId = 'General',
    this.composition = '',
    required this.mrp,
    required this.sellPrice,
    required this.costPrice,
    this.taxPercent = 0.0,
    this.hsnCode = '',
    this.unit = 'PCS',
    this.isLoose = false,
    this.trackBatch = false,
    this.drugSchedule = 'OTC',
    required this.stockQty,
    this.reorderLevel = 5,
    this.barcode = '',
    this.batches = const [],
  });

  Batch? get nearestValidBatch {
    if (!trackBatch || batches.isEmpty) return null;
    final now = DateTime.now();
    final valid = batches.where((b) => b.expiryDate.isAfter(now) && b.currentStock > 0).toList();
    if (valid.isEmpty) return null;
    valid.sort((a, b) => a.expiryDate.compareTo(b.expiryDate)); // FEFO
    return valid.first;
  }
}

class Batch {
  final String batchId;
  final String batchNo;
  final DateTime expiryDate;
  final double mrp;
  double currentStock;

  Batch({
    required this.batchId,
    required this.batchNo,
    required this.expiryDate,
    required this.mrp,
    required this.currentStock,
  });

  bool get isExpired => expiryDate.isBefore(DateTime.now());
  bool get isNearExpiry {
    final diff = expiryDate.difference(DateTime.now()).inDays;
    return diff inRange (0, 90);
  }
}

extension IntRange on int {
  bool inRange(int min, int max) => this >= min && this <= max;
}

class QuickItem {
  final String id;
  final String label;
  final String color;
  final Product product;
  final int sortOrder;

  QuickItem({
    required this.id,
    required this.label,
    required this.color,
    required this.product,
    this.sortOrder = 0,
  });
}

class BillItem {
  final String sku;
  final String name;
  double qty; // Decimal quantities (e.g. 0.750 kg)
  final String unit;
  final double unitPrice;
  final double taxPercent;
  final String? batchNo;
  final DateTime? expiryDate;
  final String hsnCode;

  BillItem({
    required this.sku,
    required this.name,
    required this.qty,
    this.unit = 'PCS',
    required this.unitPrice,
    this.taxPercent = 0.0,
    this.batchNo,
    this.expiryDate,
    this.hsnCode = '',
  });

  double get lineTotal => (qty * unitPrice * 100).round() / 100.0;
}

class Bill {
  final String billId;
  final String counterId;
  final String cashierId;
  String invoiceNo; // Max 16 chars sequential, e.g. S01/2627/000123
  String customerPhone;
  String customerName;
  String customerGstin;
  String patientName;
  String doctorName;
  double subtotal;
  double discount;
  double taxTotal;
  double roundOff;
  double grandTotal;
  String paymentMode;
  String paymentStatus;
  String billStatus;
  String receiptToken;
  String checkedStatus;
  String checkedBy;
  String checkedGateId;
  int checkedAt;
  final int createdAt;
  int paidAt;
  List<BillItem> items;

  Bill({
    required this.billId,
    required this.counterId,
    required this.cashierId,
    this.invoiceNo = '',
    this.customerPhone = '',
    this.customerName = '',
    this.customerGstin = '',
    this.patientName = '',
    this.doctorName = '',
    this.subtotal = 0.0,
    this.discount = 0.0,
    this.taxTotal = 0.0,
    this.roundOff = 0.0,
    this.grandTotal = 0.0,
    this.paymentMode = 'PENDING',
    this.paymentStatus = 'PENDING',
    this.billStatus = 'OPEN',
    this.receiptToken = '',
    this.checkedStatus = 'NO',
    this.checkedBy = '',
    this.checkedGateId = '',
    this.checkedAt = 0,
    required this.createdAt,
    this.paidAt = 0,
    this.items = const [],
  });

  void recalculate() {
    subtotal = items.fold(0.0, (sum, i) => sum + i.lineTotal);
    taxTotal = items.fold(0.0, (sum, i) => sum + (i.lineTotal * (i.taxPercent / (100.0 + i.taxPercent))));
    final rawTotal = (subtotal - discount).clamp(0.0, double.infinity);
    final rounded = rawTotal.roundToDouble();
    roundOff = ((rounded - rawTotal) * 100).round() / 100.0;
    grandTotal = rounded;
  }
}

class Product {
  final String sku;
  final String name;
  final String categoryId;
  final double mrp;
  final double sellPrice;
  final double costPrice;
  final double taxPercent;
  final String unit;
  int stockQty;
  final int reorderLevel;
  final String barcode;

  Product({
    required this.sku,
    required this.name,
    this.categoryId = 'General',
    required this.mrp,
    required this.sellPrice,
    required this.costPrice,
    this.taxPercent = 0.0,
    this.unit = 'pcs',
    required this.stockQty,
    this.reorderLevel = 5,
    this.barcode = '',
  });

  factory Product.fromJson(Map<String, dynamic> json) {
    return Product(
      sku: json['sku'] ?? '',
      name: json['name'] ?? '',
      categoryId: json['category_id'] ?? 'General',
      mrp: (json['mrp'] as num?)?.toDouble() ?? 0.0,
      sellPrice: (json['sell_price'] as num?)?.toDouble() ?? 0.0,
      costPrice: (json['cost_price'] as num?)?.toDouble() ?? 0.0,
      taxPercent: (json['tax_percent'] as num?)?.toDouble() ?? 0.0,
      unit: json['unit'] ?? 'pcs',
      stockQty: (json['stock_qty'] as num?)?.toInt() ?? 0,
      reorderLevel: (json['reorder_level'] as num?)?.toInt() ?? 5,
      barcode: json['barcode'] ?? '',
    );
  }

  Map<String, dynamic> toJson() => {
    'sku': sku,
    'name': name,
    'categoryId': categoryId,
    'mrp': mrp,
    'sellPrice': sellPrice,
    'costPrice': costPrice,
    'taxPercent': taxPercent,
    'unit': unit,
    'stockQty': stockQty,
    'reorderLevel': reorderLevel,
    'barcode': barcode,
  };
}

class BillItem {
  final String sku;
  final String name;
  int qty;
  final double unitPrice;
  final double taxPercent;
  final double costPriceSnapshot;

  BillItem({
    required this.sku,
    required this.name,
    required this.qty,
    required this.unitPrice,
    this.taxPercent = 0.0,
    this.costPriceSnapshot = 0.0,
  });

  double get lineTotal => qty * unitPrice;

  Map<String, dynamic> toJson() => {
    'sku': sku,
    'name': name,
    'qty': qty,
    'unitPrice': unitPrice,
    'taxPercent': taxPercent,
    'costPriceSnapshot': costPriceSnapshot,
    'lineTotal': lineTotal,
  };
}

class Bill {
  final String billId;
  final String counterId;
  final String cashierId;
  String customerPhone;
  double subtotal;
  double discount;
  double taxTotal;
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
    this.customerPhone = '',
    this.subtotal = 0.0,
    this.discount = 0.0,
    this.taxTotal = 0.0,
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
    taxTotal = items.fold(0.0, (sum, i) => sum + (i.lineTotal * (i.taxPercent / 100.0)));
    grandTotal = (subtotal + taxTotal - discount).clamp(0.0, double.infinity);
  }

  Map<String, dynamic> toJson() => {
    'billId': billId,
    'counterId': counterId,
    'cashierId': cashierId,
    'customerPhone': customerPhone,
    'subtotal': subtotal,
    'discount': discount,
    'taxTotal': taxTotal,
    'grandTotal': grandTotal,
    'paymentMode': paymentMode,
    'paymentStatus': paymentStatus,
    'billStatus': billStatus,
    'receiptToken': receiptToken,
    'checkedStatus': checkedStatus,
    'checkedBy': checkedBy,
    'checkedGateId': checkedGateId,
    'checkedAt': checkedAt,
    'createdAt': createdAt,
    'paidAt': paidAt,
    'items': items.map((i) => i.toJson()).toList(),
  };
}

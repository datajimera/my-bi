import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';
import '../../core/config.dart';
import '../../data/models.dart';
import '../../data/repository.dart';

class CounterScreen extends StatefulWidget {
  const CounterScreen({super.key});

  @override
  State<CounterScreen> createState() => _CounterScreenState();
}

class _CounterScreenState extends State<CounterScreen> with SingleTickerProviderStateMixin {
  final repo = StoreRepository.instance;
  Bill? currentBill;
  final TextEditingController searchController = TextEditingController();
  final TextEditingController phoneController = TextEditingController();
  final TextEditingController gstinController = TextEditingController();
  final TextEditingController patientController = TextEditingController();
  final TextEditingController doctorController = TextEditingController();
  final FocusNode barcodeGunFocus = FocusNode();
  late TabController tabController;
  String paymentMode = 'UPI';

  @override
  void initState() {
    super.initState();
    tabController = TabController(length: 2, vsync: this);
    repo.addListener(_onRepoUpdate);
  }

  @override
  void dispose() {
    tabController.dispose();
    repo.removeListener(_onRepoUpdate);
    searchController.dispose();
    phoneController.dispose();
    gstinController.dispose();
    patientController.dispose();
    doctorController.dispose();
    barcodeGunFocus.dispose();
    super.dispose();
  }

  void _onRepoUpdate() {
    if (mounted) setState(() {});
  }

  void _startNewBill() {
    setState(() {
      currentBill = repo.createNewBill("C01", "cashier1");
      phoneController.clear();
      gstinController.clear();
      patientController.clear();
      doctorController.clear();
      searchController.clear();
    });
  }

  void _handleBarcodeOrSku(String val) {
    if (val.trim().isEmpty || currentBill == null) return;
    final prod = repo.findProduct(val.trim());
    if (prod != null) {
      if (prod.isLoose) {
        _showWeightDialog(prod);
      } else {
        final res = repo.addItemToBill(currentBill!, prod);
        if (res != "OK") {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(res), backgroundColor: Colors.red));
        }
      }
      searchController.clear();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Product not found!"), backgroundColor: Colors.red));
    }
  }

  void _showWeightDialog(Product prod) {
    double pricePerKg = prod.sellPrice;
    String mode = 'WEIGHT'; // 'WEIGHT' or 'AMOUNT'
    final weightCtrl = TextEditingController(text: "1.000");
    final amountCtrl = TextEditingController(text: "${pricePerKg.toInt()}");
    bool isGrams = false;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDlgState) {
          double calculatedLineTotal = 0;
          double finalKg = 0;

          if (mode == 'WEIGHT') {
            double val = double.tryParse(weightCtrl.text) ?? 0.0;
            finalKg = isGrams ? val / 1000.0 : val;
            calculatedLineTotal = finalKg * pricePerKg;
          } else {
            double amt = double.tryParse(amountCtrl.text) ?? 0.0;
            finalKg = pricePerKg > 0 ? amt / pricePerKg : 0.0;
            calculatedLineTotal = amt;
          }

          return AlertDialog(
            title: Text("Weigh: ${prod.name}"),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text("Rate: ₹${pricePerKg.toStringAsFixed(2)} / kg", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF1B998B))),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: ChoiceChip(
                        label: const Center(child: Text("By Weight")),
                        selected: mode == 'WEIGHT',
                        onSelected: (v) => setDlgState(() => mode = 'WEIGHT'),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: ChoiceChip(
                        label: const Center(child: Text("By Amount (₹)")),
                        selected: mode == 'AMOUNT',
                        onSelected: (v) => setDlgState(() => mode = 'AMOUNT'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                if (mode == 'WEIGHT') ...[
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: weightCtrl,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: InputDecoration(labelText: isGrams ? "Grams (g)" : "Kilograms (kg)", border: const OutlineInputBorder()),
                          onChanged: (v) => setDlgState(() {}),
                        ),
                      ),
                      const SizedBox(width: 8),
                      ToggleButtons(
                        isSelected: [!isGrams, isGrams],
                        onPressed: (idx) => setDlgState(() => isGrams = idx == 1),
                        children: const [Padding(padding: EdgeInsets.symmetric(horizontal: 10), child: Text("kg")), Padding(padding: EdgeInsets.symmetric(horizontal: 10), child: Text("g"))],
                      ),
                    ],
                  ),
                ] else ...[
                  TextField(
                    controller: amountCtrl,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(labelText: "Customer says 'Rs X ka'", border: OutlineInputBorder(), prefixText: "₹ "),
                    onChanged: (v) => setDlgState(() {}),
                  ),
                ],
                const SizedBox(height: 14),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(color: const Color(0xFFE8F5E9), borderRadius: BorderRadius.circular(8)),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text("Weight: ${finalKg.toStringAsFixed(3)} kg", style: const TextStyle(fontWeight: FontWeight.bold)),
                      Text("Total: ₹${calculatedLineTotal.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.green, fontSize: 16)),
                    ],
                  ),
                ),
              ],
            ),
            actions: [
              TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Cancel")),
              ElevatedButton(
                onPressed: () {
                  if (finalKg > 0 && currentBill != null) {
                    repo.addItemToBill(currentBill!, prod, qty: double.parse(finalKg.toStringAsFixed(3)));
                    Navigator.pop(ctx);
                  }
                },
                style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF12355B)),
                child: const Text("Add to Bill", style: TextStyle(color: Colors.white)),
              ),
            ],
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (currentBill == null) {
      return _buildHomeScreen();
    }
    return _buildBillingScreen();
  }

  Widget _buildHomeScreen() {
    final todayBills = repo.bills.where((b) => b.paymentStatus == 'PAID').toList();
    final totalSales = todayBills.fold(0.0, (sum, b) => sum + b.grandTotal);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Billing Counter C01', style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
        backgroundColor: const Color(0xFF12355B),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(
                  child: Card(
                    color: Colors.white,
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text("Today's Bills", style: TextStyle(color: Colors.grey, fontSize: 12)),
                          Text("${todayBills.length}", style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
                        ],
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Card(
                    color: Colors.white,
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text("Gross Revenue", style: TextStyle(color: Colors.grey, fontSize: 12)),
                          Text("₹${totalSales.toStringAsFixed(0)}", style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF1B998B))),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            // Big + New Bill Button
            InkWell(
              onTap: _startNewBill,
              child: Container(
                height: 110,
                decoration: BoxDecoration(
                  color: const Color(0xFF12355B),
                  borderRadius: BorderRadius.circular(16),
                  boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.1), blurRadius: 8, offset: const Offset(0, 4))],
                ),
                padding: const EdgeInsets.all(20),
                child: Row(
                  children: [
                    Container(
                      width: 56,
                      height: 56,
                      decoration: const BoxDecoration(shape: BoxShape.circle, color: Color(0xFF1B998B)),
                      child: const Icon(Icons.add, color: Colors.white, size: 36),
                    ),
                    const SizedBox(width: 16),
                    const Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text("+ New Customer Bill", style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
                        Text("Auto-creates Bill ID & opens scanner", style: TextStyle(color: Color(0xFFCCFBF1), fontSize: 12)),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),
            if (repo.heldBills.isNotEmpty) ...[
              const Text("Parked / Held Bills:", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.orange)),
              const SizedBox(height: 8),
              Expanded(
                child: ListView.builder(
                  itemCount: repo.heldBills.length,
                  itemBuilder: (ctx, idx) {
                    final b = repo.heldBills[idx];
                    return Card(
                      color: const Color(0xFFFFF3E0),
                      child: ListTile(
                        title: Text(b.billId, style: const TextStyle(fontWeight: FontWeight.bold)),
                        subtitle: Text("${b.items.length} items • ₹${b.grandTotal.toStringAsFixed(2)}"),
                        trailing: ElevatedButton(
                          onPressed: () {
                            repo.resumeBill(b);
                            setState(() => currentBill = b);
                          },
                          style: ElevatedButton.styleFrom(backgroundColor: Colors.orange),
                          child: const Text("Resume", style: TextStyle(color: Colors.white)),
                        ),
                      ),
                    );
                  },
                ),
              ),
            ] else
              const Spacer(),
          ],
        ),
      ),
    );
  }

  Widget _buildBillingScreen() {
    final bill = currentBill!;

    return Scaffold(
      appBar: AppBar(
        title: Text("Bill: ${bill.billId}", style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: const Color(0xFF12355B),
        bottom: TabBar(
          controller: tabController,
          labelColor: Colors.white,
          indicatorColor: const Color(0xFF1B998B),
          tabs: const [Tab(text: "Cart Items"), Tab(text: "Quick Sale (Loose/Fruits)")],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.pause, color: Colors.orange),
            tooltip: "Hold Bill",
            onPressed: () {
              repo.holdBill(bill);
              setState(() => currentBill = null);
            },
          ),
          IconButton(
            icon: const Icon(Icons.close, color: Colors.red),
            tooltip: "Cancel Bill",
            onPressed: () => setState(() => currentBill = null),
          ),
        ],
      ),
      body: Column(
        children: [
          // Barcode Gun / Camera Barcode Search Box (Section 3.3)
          Container(
            padding: const EdgeInsets.all(12),
            color: const Color(0xFF0F172A),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: searchController,
                    focusNode: barcodeGunFocus,
                    autofocus: true,
                    style: const TextStyle(color: Colors.white),
                    decoration: InputDecoration(
                      hintText: "Barcode gun / type EAN, SKU or medicine salt",
                      hintStyle: const TextStyle(color: Colors.grey, fontSize: 13),
                      filled: true,
                      fillColor: const Color(0xFF1E293B),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
                    ),
                    onSubmitted: (val) => _handleBarcodeOrSku(val),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: () => _handleBarcodeOrSku(searchController.text),
                  style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF1B998B)),
                  child: const Text("Scan/Add", style: TextStyle(color: Colors.white)),
                ),
              ],
            ),
          ),
          Expanded(
            child: TabBarView(
              controller: tabController,
              children: [
                // TAB 1: Cart Items
                bill.items.isEmpty
                    ? const Center(child: Text("No items scanned yet.\nScan barcode or use Quick Sale tab.", textAlign: TextAlign.center, style: TextStyle(color: Colors.grey)))
                    : ListView.builder(
                        itemCount: bill.items.length,
                        itemBuilder: (ctx, idx) {
                          final item = bill.items[idx];
                          return Card(
                            margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                            child: Padding(
                              padding: const EdgeInsets.all(8.0),
                              child: Row(
                                children: [
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(item.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                                        Text(
                                          "${item.unit == 'KG' ? '${item.qty.toStringAsFixed(3)} kg' : 'Qty: ${item.qty.toInt()}'} • ₹${item.unitPrice.toStringAsFixed(2)} / ${item.unit}" +
                                          (item.batchNo != null ? " [Batch: ${item.batchNo}]" : ""),
                                          style: const TextStyle(color: Colors.grey, fontSize: 11),
                                        ),
                                      ],
                                    ),
                                  ),
                                  IconButton(
                                    icon: const Icon(Icons.remove_circle_outline, size: 20),
                                    onPressed: () => repo.updateItemQty(bill, item.sku, item.qty - (item.unit == 'KG' ? 0.250 : 1)),
                                  ),
                                  Text(item.unit == 'KG' ? item.qty.toStringAsFixed(3) : "${item.qty.toInt()}", style: const TextStyle(fontWeight: FontWeight.bold)),
                                  IconButton(
                                    icon: const Icon(Icons.add_circle_outline, size: 20),
                                    onPressed: () => repo.updateItemQty(bill, item.sku, item.qty + (item.unit == 'KG' ? 0.250 : 1)),
                                  ),
                                  SizedBox(
                                    width: 70,
                                    child: Text("₹${item.lineTotal.toStringAsFixed(2)}", textAlign: TextAlign.right, style: const TextStyle(fontWeight: FontWeight.bold)),
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
                // TAB 2: Quick Sale Button Grid (Section 5.1)
                Padding(
                  padding: const EdgeInsets.all(12.0),
                  child: GridView.builder(
                    gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, childAspectRatio: 1.3, crossAxisSpacing: 10, mainAxisSpacing: 10),
                    itemCount: repo.quickItems.length,
                    itemBuilder: (ctx, idx) {
                      final qi = repo.quickItems[idx];
                      return InkWell(
                        onTap: () {
                          if (qi.product.isLoose) {
                            _showWeightDialog(qi.product);
                          } else {
                            repo.addItemToBill(bill, qi.product);
                          }
                        },
                        child: Container(
                          decoration: BoxDecoration(
                            color: Color(int.parse(qi.color.replaceFirst('#', '0xFF'))),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          alignment: Alignment.center,
                          padding: const EdgeInsets.all(8),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Text(qi.label, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                              Text("₹${qi.product.sellPrice.toStringAsFixed(0)} / ${qi.product.unit}", style: const TextStyle(color: Colors.white70, fontSize: 11)),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
          // Sticky Bottom Summary Bar with Round-off & Total (Section 5.2 & 7.2)
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(color: Colors.white, boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.1), blurRadius: 8, offset: const Offset(0, -2))]),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text("Subtotal: ₹${bill.subtotal.toStringAsFixed(2)} • Round off: ₹${bill.roundOff.toStringAsFixed(2)}", style: const TextStyle(color: Colors.grey, fontSize: 11)),
                    Text("Grand Total: ₹${bill.grandTotal.toStringAsFixed(2)}", style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
                  ],
                ),
                ElevatedButton.icon(
                  onPressed: bill.items.isEmpty ? null : () => _showPaymentDialog(bill),
                  style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF12355B), padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 12), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10))),
                  icon: const Icon(Icons.arrow_forward, color: Colors.white),
                  label: const Text("Next / Pay", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _showPaymentDialog(Bill bill) {
    bool hasScheduleH = bill.items.any((i) {
      final p = repo.findProduct(i.sku);
      return p != null && (p.drugSchedule == 'H' || p.drugSchedule == 'H1');
    });

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) {
          final upiUri = "upi://pay?pa=${AppConfig.upiVpa}&pn=${Uri.encodeComponent(AppConfig.upiPayeeName)}&am=${bill.grandTotal.toStringAsFixed(2)}&tn=${bill.billId}&cu=INR";

          return Padding(
            padding: EdgeInsets.only(bottom: MediaQuery.of(ctx).viewInsets.bottom, left: 20, right: 20, top: 20),
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text("Total Payable: ₹${bill.grandTotal.toStringAsFixed(2)}", style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(child: ChoiceChip(label: const Center(child: Text("UPI QR")), selected: paymentMode == 'UPI', onSelected: (v) => setModalState(() => paymentMode = 'UPI'))),
                      const SizedBox(width: 8),
                      Expanded(child: ChoiceChip(label: const Center(child: Text("Cash")), selected: paymentMode == 'CASH', onSelected: (v) => setModalState(() => paymentMode = 'CASH'))),
                    ],
                  ),
                  const SizedBox(height: 12),
                  if (paymentMode == 'UPI') ...[
                    SizedBox(width: 140, height: 140, child: QrImageView(data: upiUri, size: 140)),
                  ],
                  // Pharmacy Schedule H/H1 Doctor & Patient Capture (Section 4.3)
                  if (hasScheduleH) ...[
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(color: const Color(0xFFFFEBEE), borderRadius: BorderRadius.circular(8)),
                      child: Column(
                        children: [
                          const Text("⚠️ Schedule H/H1 Drugs: Doctor & Patient Info Required", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.red, fontSize: 11)),
                          const SizedBox(height: 6),
                          TextField(controller: patientController, decoration: const InputDecoration(labelText: "Patient Name*", isDense: true, border: OutlineInputBorder())),
                          const SizedBox(height: 6),
                          TextField(controller: doctorController, decoration: const InputDecoration(labelText: "Doctor Name*", isDense: true, border: OutlineInputBorder())),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: 8),
                  TextField(controller: phoneController, keyboardType: TextInputType.phone, decoration: const InputDecoration(labelText: "Customer Mobile Number", isDense: true, border: OutlineInputBorder())),
                  const SizedBox(height: 8),
                  TextField(controller: gstinController, decoration: const InputDecoration(labelText: "Customer GSTIN (Optional B2B Invoice)", isDense: true, border: OutlineInputBorder())),
                  const SizedBox(height: 16),
                  ElevatedButton(
                    onPressed: () {
                      if (hasScheduleH && (patientController.text.isEmpty || doctorController.text.isEmpty)) {
                        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Patient and Doctor name are required for Schedule H medicines!")));
                        return;
                      }
                      repo.completePayment(
                        bill,
                        paymentMode,
                        phoneController.text,
                        customerGstin: gstinController.text,
                        patientName: patientController.text,
                        doctorName: doctorController.text,
                      );
                      Navigator.pop(ctx);
                      _showReceiptDialog(bill);
                    },
                    style: ElevatedButton.styleFrom(backgroundColor: Colors.green, minimumSize: const Size.fromHeight(48)),
                    child: const Text("Confirm Payment", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                  ),
                  const SizedBox(height: 20),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  void _showReceiptDialog(Bill bill) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        title: const Text("Payment Successful!"),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text("GST Invoice: ${bill.invoiceNo}", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
            Text("Bill ID: ${bill.billId}", style: const TextStyle(color: Colors.grey, fontSize: 12)),
            Text("Total: ₹${bill.grandTotal.toStringAsFixed(2)} (${bill.paymentMode})"),
            const SizedBox(height: 10),
            SizedBox(width: 130, height: 130, child: QrImageView(data: bill.receiptToken, size: 130)),
            const SizedBox(height: 6),
            const Text("Show this Exit QR to Security Guard", style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
          ],
        ),
        actions: [
          OutlinedButton.icon(
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Sent print command to Bluetooth Thermal Printer!")));
            },
            icon: const Icon(Icons.print),
            label: const Text("Thermal Print"),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(ctx);
              setState(() => currentBill = null);
            },
            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF12355B)),
            child: const Text("Done (Next Customer)", style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }
}

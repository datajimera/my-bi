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

class _CounterScreenState extends State<CounterScreen> {
  final repo = StoreRepository.instance;
  Bill? currentBill;
  final TextEditingController searchController = TextEditingController();
  final TextEditingController phoneController = TextEditingController();
  final TextEditingController cashController = TextEditingController();
  String paymentMode = 'UPI';

  @override
  void initState() {
    super.initState();
    repo.addListener(_onRepoUpdate);
  }

  @override
  void dispose() {
    repo.removeListener(_onRepoUpdate);
    searchController.dispose();
    phoneController.dispose();
    cashController.dispose();
    super.dispose();
  }

  void _onRepoUpdate() {
    if (mounted) setState(() {});
  }

  void _startNewBill() {
    setState(() {
      currentBill = repo.createNewBill("C01", "cashier1");
      phoneController.clear();
      cashController.clear();
      searchController.clear();
    });
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
                          const Text("Counter Sales", style: TextStyle(color: Colors.grey, fontSize: 12)),
                          Text("₹${totalSales.toStringAsFixed(0)}", style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF1B998B))),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            // BIG + NEW BILL BUTTON (Section 4.1)
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
            onPressed: () {
              setState(() => currentBill = null);
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // Scanner / Quick Add Box
          Container(
            padding: const EdgeInsets.all(12),
            color: const Color(0xFF0F172A),
            child: Column(
              children: [
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: searchController,
                        style: const TextStyle(color: Colors.white),
                        decoration: InputDecoration(
                          hintText: "Scan QR or type SKU (e.g. SKU-1001)",
                          hintStyle: const TextStyle(color: Colors.grey, fontSize: 13),
                          filled: true,
                          fillColor: const Color(0xFF1E293B),
                          contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                          border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
                        ),
                        onSubmitted: (val) {
                          final prod = repo.findProduct(val);
                          if (prod != null) {
                            repo.addItemToBill(bill, prod);
                            searchController.clear();
                          }
                        },
                      ),
                    ),
                    const SizedBox(width: 8),
                    ElevatedButton(
                      onPressed: () {
                        final prod = repo.findProduct(searchController.text);
                        if (prod != null) {
                          repo.addItemToBill(bill, prod);
                          searchController.clear();
                        }
                      },
                      style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF1B998B)),
                      child: const Text("Add", style: TextStyle(color: Colors.white)),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: repo.products.take(5).map((p) => Padding(
                      padding: const EdgeInsets.only(right: 6),
                      child: ActionChip(
                        label: Text("${p.name.split(' ').first} (₹${p.sellPrice.toInt()})", style: const TextStyle(fontSize: 11, color: Colors.white)),
                        backgroundColor: const Color(0xFF1E293B),
                        onPressed: () => repo.addItemToBill(bill, p),
                      ),
                    )).toList(),
                  ),
                ),
              ],
            ),
          ),
          // Live Cart Items
          Expanded(
            child: bill.items.isEmpty
                ? const Center(child: Text("Cart is empty. Scan products above.", style: TextStyle(color: Colors.grey)))
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
                                    Text("SKU: ${item.sku} • ₹${item.unitPrice.toStringAsFixed(2)}", style: const TextStyle(color: Colors.grey, fontSize: 11)),
                                  ],
                                ),
                              ),
                              IconButton(
                                icon: const Icon(Icons.remove_circle_outline, size: 20),
                                onPressed: () => repo.updateItemQty(bill, item.sku, item.qty - 1),
                              ),
                              Text("${item.qty}", style: const TextStyle(fontWeight: FontWeight.bold)),
                              IconButton(
                                icon: const Icon(Icons.add_circle_outline, size: 20),
                                onPressed: () => repo.updateItemQty(bill, item.sku, item.qty + 1),
                              ),
                              SizedBox(
                                width: 65,
                                child: Text("₹${item.lineTotal.toStringAsFixed(2)}", textAlign: TextAlign.right, style: const TextStyle(fontWeight: FontWeight.bold)),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
          ),
          // Sticky Bottom Bar
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.1), blurRadius: 8, offset: const Offset(0, -2))],
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text("Items: ${bill.items.fold(0, (sum, i) => sum + i.qty)}", style: const TextStyle(color: Colors.grey, fontSize: 12)),
                    Text("Total: ₹${bill.grandTotal.toStringAsFixed(2)}", style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
                  ],
                ),
                ElevatedButton.icon(
                  onPressed: bill.items.isEmpty ? null : () => _showPaymentDialog(bill),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF12355B),
                    padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                    shape: RoundedCornerShape(10),
                  ),
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
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) {
          final upiUri = "upi://pay?pa=${AppConfig.upiVpa}&pn=${Uri.encodeComponent(AppConfig.upiPayeeName)}&am=${bill.grandTotal.toStringAsFixed(2)}&tn=${bill.billId}&cu=INR";
          final cashVal = double.tryParse(cashController.text) ?? 0.0;
          final changeVal = (cashVal - bill.grandTotal).clamp(0.0, double.infinity);

          return Padding(
            padding: EdgeInsets.only(bottom: MediaQuery.of(ctx).viewInsets.bottom, left: 20, right: 20, top: 20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text("Total: ₹${bill.grandTotal.toStringAsFixed(2)}", style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: ChoiceChip(
                        label: const Center(child: Text("UPI QR")),
                        selected: paymentMode == 'UPI',
                        onSelected: (val) => setModalState(() => paymentMode = 'UPI'),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: ChoiceChip(
                        label: const Center(child: Text("Cash")),
                        selected: paymentMode == 'CASH',
                        onSelected: (val) => setModalState(() => paymentMode = 'CASH'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                if (paymentMode == 'UPI') ...[
                  SizedBox(
                    width: 150,
                    height: 150,
                    child: QrImageView(data: upiUri, version: QrVersions.auto, size: 150),
                  ),
                  const SizedBox(height: 4),
                  const Text("Customer scans with GPay/PhonePe/Paytm", style: TextStyle(fontSize: 11, color: Colors.grey)),
                ] else ...[
                  TextField(
                    controller: cashController,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(labelText: "Cash Received (₹)", border: OutlineInputBorder()),
                    onChanged: (v) => setModalState(() {}),
                  ),
                  const SizedBox(height: 8),
                  Text("Change to Return: ₹${changeVal.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.green)),
                ],
                const SizedBox(height: 12),
                TextField(
                  controller: phoneController,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(labelText: "Customer Mobile Number", border: OutlineInputBorder()),
                ),
                const SizedBox(height: 16),
                ElevatedButton(
                  onPressed: () {
                    repo.completePayment(bill, paymentMode, phoneController.text);
                    Navigator.pop(ctx);
                    _showReceiptDialog(bill);
                  },
                  style: ElevatedButton.styleFrom(backgroundColor: Colors.green, minimumSize: const Size.fromHeight(48)),
                  child: const Text("Confirm Payment", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                ),
                const SizedBox(height: 20),
              ],
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
            Text("Bill ID: ${bill.billId}", style: const TextStyle(fontWeight: FontWeight.bold)),
            Text("Amount: ₹${bill.grandTotal.toStringAsFixed(2)}"),
            const SizedBox(height: 12),
            SizedBox(
              width: 140,
              height: 140,
              child: QrImageView(data: bill.receiptToken, size: 140),
            ),
            const SizedBox(height: 6),
            const Text("Show this Exit QR to Security Guard", style: TextStyle(fontSize: 11, color: Color(0xFF12355B), fontWeight: FontWeight.bold)),
          ],
        ),
        actions: [
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

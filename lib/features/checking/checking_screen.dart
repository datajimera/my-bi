import 'package:flutter/material.dart';
import '../../data/models.dart';
import '../../data/repository.dart';

class CheckingScreen extends StatefulWidget {
  const CheckingScreen({super.key});

  @override
  State<CheckingScreen> createState() => _CheckingScreenState();
}

class _CheckingScreenState extends State<CheckingScreen> {
  final repo = StoreRepository.instance;
  final TextEditingController inputController = TextEditingController();
  Map<String, dynamic>? lastResult;

  @override
  void initState() {
    super.initState();
    repo.addListener(_update);
  }

  @override
  void dispose() {
    repo.removeListener(_update);
    inputController.dispose();
    super.dispose();
  }

  void _update() {
    if (mounted) setState(() {});
  }

  void _verify(String qrOrId) {
    if (qrOrId.trim().isEmpty) return;
    final res = repo.verifyGateExit(qrOrId.trim(), "guard1", "G01");
    setState(() {
      lastResult = res;
      inputController.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    if (lastResult != null) {
      return _buildResultScreen();
    }
    return _buildScanScreen();
  }

  Widget _buildScanScreen() {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Exit Guard Verification (Gate G01)", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
        backgroundColor: const Color(0xFF12355B),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            // Scanner / Manual Entry Container
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: const Color(0xFF0F172A),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(
                children: [
                  const Icon(Icons.qr_code_scanner, color: Color(0xFF1B998B), size: 54),
                  const SizedBox(height: 8),
                  const Text("Scan Customer Receipt QR Code", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                  const Text("Or enter Bill ID from Customer SMS", style: TextStyle(color: Colors.grey, fontSize: 12)),
                  const SizedBox(height: 16),
                  TextField(
                    controller: inputController,
                    style: const TextStyle(color: Colors.white),
                    decoration: InputDecoration(
                      hintText: "Enter Bill ID (e.g. C01-20261006-0001)",
                      hintStyle: const TextStyle(color: Colors.grey, fontSize: 13),
                      filled: true,
                      fillColor: const Color(0xFF1E293B),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
                    ),
                    onSubmitted: (val) => _verify(val),
                  ),
                  const SizedBox(height: 10),
                  ElevatedButton.icon(
                    onPressed: () => _verify(inputController.text),
                    style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF1B998B), minimumSize: const Size.fromHeight(44)),
                    icon: const Icon(Icons.check, color: Colors.white),
                    label: const Text("Verify Exit", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            const Align(
              alignment: Alignment.centerLeft,
              child: Text("Recent Customer Receipts to Test:", style: TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
            ),
            const SizedBox(height: 8),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: repo.bills.take(4).map((b) => Padding(
                  padding: const EdgeInsets.only(right: 8.0),
                  child: ActionChip(
                    label: Text("${b.billId} (${b.checkedStatus == 'YES' ? 'Checked' : b.paymentStatus})"),
                    backgroundColor: Colors.white,
                    onPressed: () => _verify(b.receiptToken.isNotEmpty ? b.receiptToken : b.billId),
                  ),
                )).toList(),
              ),
            ),
            const SizedBox(height: 16),
            const Align(
              alignment: Alignment.centerLeft,
              child: Text("Today's Gate Scan History:", style: TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
            ),
            const SizedBox(height: 8),
            Expanded(
              child: repo.gateScans.isEmpty
                  ? const Center(child: Text("No scans recorded yet.", style: TextStyle(color: Colors.grey)))
                  : ListView.builder(
                      itemCount: repo.gateScans.length,
                      itemBuilder: (ctx, idx) {
                        final s = repo.gateScans[idx];
                        final isApproved = s['result'] == 'APPROVED';
                        return Card(
                          child: ListTile(
                            leading: Icon(
                              isApproved ? Icons.check_circle : Icons.warning,
                              color: isApproved ? Colors.green : Colors.red,
                            ),
                            title: Text(s['billId'], style: const TextStyle(fontWeight: FontWeight.bold)),
                            subtitle: Text("Result: ${s['result']}"),
                          ),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildResultScreen() {
    final status = lastResult!['status'] as String;
    final message = lastResult!['message'] as String;
    final Bill? bill = lastResult!['bill'] as Bill?;

    Color bgColor = Colors.red;
    IconData icon = Icons.cancel;
    String title = "INVALID QR / NOT PAID";

    if (status == 'APPROVED') {
      bgColor = const Color(0xFF2E7D32); // Green
      icon = Icons.check_circle;
      title = "APPROVED - EXIT ALLOWED";
    } else if (status == 'ALREADY_CHECKED') {
      bgColor = const Color(0xFFED6C02); // Orange
      icon = Icons.warning;
      title = "ALREADY CHECKED - DUPLICATE";
    }

    return Scaffold(
      backgroundColor: bgColor,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            children: [
              const SizedBox(height: 20),
              Icon(icon, color: Colors.white, size: 70),
              const SizedBox(height: 12),
              Text(title, textAlign: TextAlign.center, style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              Text(message, textAlign: TextAlign.center, style: const TextStyle(color: Colors.white, fontSize: 14)),
              const SizedBox(height: 20),
              if (bill != null) ...[
                Expanded(
                  child: Container(
                    decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16)),
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text("Bill: ${bill.billId} • Total: ₹${bill.grandTotal.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold)),
                        const Divider(),
                        const Text("CHECK PHYSICAL BAG ITEMS:", style: TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF1B998B))),
                        const SizedBox(height: 8),
                        Expanded(
                          child: ListView.builder(
                            itemCount: bill.items.length,
                            itemBuilder: (ctx, idx) {
                              final item = bill.items[idx];
                              return Card(
                                color: const Color(0xFFF8FAFC),
                                child: Padding(
                                  padding: const EdgeInsets.all(10.0),
                                  child: Row(
                                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                    children: [
                                      Text(item.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                        decoration: BoxDecoration(color: const Color(0xFF12355B), borderRadius: BorderRadius.circular(6)),
                                        child: Text("QTY: ${item.qty}", style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                                      ),
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
                ),
              ] else
                const Spacer(),
              const SizedBox(height: 16),
              ElevatedButton.icon(
                onPressed: () => setState(() => lastResult = null),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.white,
                  foregroundColor: const Color(0xFF12355B),
                  minimumSize: const Size.fromHeight(52),
                  shape: RoundedCornerShape(12),
                ),
                icon: const Icon(Icons.qr_code_scanner),
                label: const Text("Scan Next Customer", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

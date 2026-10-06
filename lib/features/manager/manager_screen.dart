import 'package:flutter/material.dart';
import '../../core/config.dart';
import '../../core/api_client.dart';
import '../../data/models.dart';
import '../../data/repository.dart';

class ManagerScreen extends StatefulWidget {
  const ManagerScreen({super.key});

  @override
  State<ManagerScreen> createState() => _ManagerScreenState();
}

class _ManagerScreenState extends State<ManagerScreen> {
  final repo = StoreRepository.instance;
  final TextEditingController urlController = TextEditingController(text: AppConfig.appsScriptUrl);
  final TextEditingController keyController = TextEditingController(text: AppConfig.apiKey);
  String connectionStatus = '';

  @override
  void initState() {
    super.initState();
    repo.addListener(_update);
  }

  @override
  void dispose() {
    repo.removeListener(_update);
    urlController.dispose();
    keyController.dispose();
    super.dispose();
  }

  void _update() {
    if (mounted) setState(() {});
  }

  void _testConnection() async {
    setState(() => connectionStatus = 'Connecting...');
    final res = await ApiClient.testConnection(urlController.text.trim(), keyController.text.trim());
    if (res['ok'] == true) {
      AppConfig.appsScriptUrl = urlController.text.trim();
      AppConfig.apiKey = keyController.text.trim();
      setState(() => connectionStatus = 'SUCCESS: Connected to Google Sheets! ⚡');
    } else {
      setState(() => connectionStatus = 'ERROR: ${res['error']}');
    }
  }

  @override
  Widget build(BuildContext context) {
    final paidBills = repo.bills.where((b) => b.paymentStatus == 'PAID').toList();
    final totalSales = paidBills.fold(0.0, (sum, b) => sum + b.grandTotal);
    final upiSales = paidBills.where((b) => b.paymentMode == 'UPI').fold(0.0, (sum, b) => sum + b.grandTotal);
    final cashSales = paidBills.where((b) => b.paymentMode == 'CASH').fold(0.0, (sum, b) => sum + b.grandTotal);

    return Scaffold(
      appBar: AppBar(
        title: const Text("Store Manager Dashboard", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
        backgroundColor: const Color(0xFF12355B),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // KPI Summary Row
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
                          const Text("Gross Revenue", style: TextStyle(color: Colors.grey, fontSize: 12)),
                          Text("₹${totalSales.toStringAsFixed(2)}", style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
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
                          const Text("Bills Paid", style: TextStyle(color: Colors.grey, fontSize: 12)),
                          Text("${paidBills.length}", style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Color(0xFF1B998B))),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            // Payment Split Card
            Card(
              color: Colors.white,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text("Payment Split", style: TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
                    const SizedBox(height: 8),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text("UPI: ₹${upiSales.toStringAsFixed(2)}", style: const TextStyle(color: Color(0xFF1B998B), fontWeight: FontWeight.bold)),
                        Text("Cash: ₹${cashSales.toStringAsFixed(2)}", style: const TextStyle(color: Color(0xFF12355B), fontWeight: FontWeight.bold)),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            // Google Apps Script Backend Settings Card
            Card(
              color: Colors.white,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.cloud_sync, color: Color(0xFF1B998B)),
                        SizedBox(width: 8),
                        Text("Google Apps Script Web App Link", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Color(0xFF12355B))),
                      ],
                    ),
                    const SizedBox(height: 6),
                    const Text("Paste your deployed script URL to sync live with Google Sheets:", style: TextStyle(fontSize: 12, color: Colors.grey)),
                    const SizedBox(height: 12),
                    TextField(
                      controller: urlController,
                      decoration: const InputDecoration(
                        labelText: "Web App URL (https://script.google.com/...)",
                        border: OutlineInputBorder(),
                        contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                      ),
                    ),
                    const SizedBox(height: 10),
                    TextField(
                      controller: keyController,
                      decoration: const InputDecoration(
                        labelText: "API Key",
                        border: OutlineInputBorder(),
                        contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                      ),
                    ),
                    const SizedBox(height: 12),
                    ElevatedButton.icon(
                      onPressed: _testConnection,
                      style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF1B998B), minimumSize: const Size.fromHeight(44)),
                      icon: const Icon(Icons.link, color: Colors.white),
                      label: const Text("Test & Save Connection", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                    ),
                    if (connectionStatus.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      Text(
                        connectionStatus,
                        style: TextStyle(
                          color: connectionStatus.startsWith('SUCCESS') ? Colors.green : Colors.red,
                          fontWeight: FontWeight.bold,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            // Products Catalog Table
            Card(
              color: Colors.white,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text("Catalog Products (${repo.products.length})", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
                        TextButton.icon(
                          onPressed: () => _showAddProductDialog(),
                          icon: const Icon(Icons.add),
                          label: const Text("Add Product"),
                        ),
                      ],
                    ),
                    const Divider(),
                    ...repo.products.take(6).map((p) => ListTile(
                      dense: true,
                      contentPadding: EdgeInsets.zero,
                      title: Text(p.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                      subtitle: Text("${p.sku} • Stock: ${p.stockQty} ${p.unit}"),
                      trailing: Text("₹${p.sellPrice.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF1B998B))),
                    )),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _showAddProductDialog() {
    final nameCtrl = TextEditingController();
    final priceCtrl = TextEditingController();
    final mrpCtrl = TextEditingController();
    final stockCtrl = TextEditingController(text: "20");

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("Add New Product"),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: nameCtrl, decoration: const InputDecoration(labelText: "Product Name")),
            TextField(controller: priceCtrl, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: "Sell Price (₹)")),
            TextField(controller: mrpCtrl, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: "MRP (₹)")),
            TextField(controller: stockCtrl, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: "Opening Stock")),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Cancel")),
          ElevatedButton(
            onPressed: () {
              if (nameCtrl.text.isNotEmpty && priceCtrl.text.isNotEmpty) {
                final sku = "SKU-${(1000 + repo.products.length + 1)}";
                final sp = double.tryParse(priceCtrl.text) ?? 0.0;
                final mp = double.tryParse(mrpCtrl.text) ?? sp;
                final sq = int.tryParse(stockCtrl.text) ?? 10;
                repo.products.insert(0, Product(
                  sku: sku,
                  name: nameCtrl.text,
                  mrp: mp,
                  sellPrice: sp,
                  costPrice: sp * 0.8,
                  stockQty: sq,
                ));
                repo.notifyListeners();
                Navigator.pop(ctx);
              }
            },
            child: const Text("Save"),
          ),
        ],
      ),
    );
  }
}

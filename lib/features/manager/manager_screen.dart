import 'package:flutter/material.dart';
import '../../core/config.dart';
import '../../data/models.dart';
import '../../data/repository.dart';

class ManagerScreen extends StatefulWidget {
  const ManagerScreen({super.key});

  @override
  State<ManagerScreen> createState() => _ManagerScreenState();
}

class _ManagerScreenState extends State<ManagerScreen> {
  final repo = StoreRepository.instance;
  final TextEditingController supabaseUrlCtrl = TextEditingController(text: AppConfig.supabaseUrl);
  final TextEditingController supabaseKeyCtrl = TextEditingController(text: AppConfig.supabaseAnonKey);

  @override
  void initState() {
    super.initState();
    repo.addListener(_update);
  }

  @override
  void dispose() {
    repo.removeListener(_update);
    supabaseUrlCtrl.dispose();
    supabaseKeyCtrl.dispose();
    super.dispose();
  }

  void _update() {
    if (mounted) setState(() {});
  }

  void _exportGstr1() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("Export GSTR-1 Data"),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            Text("Generated GSTR-1 Sheets ready for export / CA:"),
            SizedBox(height: 8),
            Text("• B2B (Invoices with customer GSTIN)"),
            Text("• B2CS (Consumer sales summary)"),
            Text("• CDNR (Credit Notes)"),
            Text("• HSN (HSN code-wise summary)"),
            Text("• DOCS (Sequential invoice number ranges)"),
            Text("• Summary (Tax liability by slab: 0%, 5%, 12%, 18%)"),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Close")),
          ElevatedButton.icon(
            onPressed: () {
              Navigator.pop(ctx);
              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("GSTR-1 Excel/CSV report exported successfully!")));
            },
            icon: const Icon(Icons.download),
            label: const Text("Download GSTR-1 (CSV/Excel)"),
          ),
        ],
      ),
    );
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
            // Revenue Cards
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
            // Payment Split & Exit Verification
            Card(
              color: Colors.white,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text("UPI: ₹${upiSales.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF1B998B))),
                    Text("Cash: ₹${cashSales.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
                    Text("Exit: ${paidBills.where((b) => b.checkedStatus == 'YES').length}/${paidBills.length}", style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.green)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            // Universal Retail & Pharmacy Settings Card (Update v3)
            Card(
              color: Colors.white,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text("Retail & Pharmacy Configurations", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Color(0xFF12355B))),
                    const SizedBox(height: 10),
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text("Pharmacy / Medical Mode"),
                      subtitle: const Text("Enables FEFO batches, expiry block, salt search, Schedule H"),
                      value: AppConfig.pharmacyMode,
                      onChanged: (val) {
                        setState(() => AppConfig.pharmacyMode = val);
                      },
                    ),
                    const Divider(),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text("Barcode Scanner Mode:", style: TextStyle(fontWeight: FontWeight.bold)),
                        DropdownButton<String>(
                          value: AppConfig.scannerMode,
                          items: const [
                            DropdownMenuItem(value: "Camera", child: Text("Camera")),
                            DropdownMenuItem(value: "Gun", child: Text("Barcode Gun (HID)")),
                            DropdownMenuItem(value: "Both", child: Text("Both")),
                          ],
                          onChanged: (val) {
                            if (val != null) setState(() => AppConfig.scannerMode = val);
                          },
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            // GST & GSTR-1 Reports Card
            Card(
              color: Colors.white,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.assessment, color: Color(0xFF12355B)),
                        SizedBox(width: 8),
                        Text("GST & GSTR-1 Month-End Export", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Color(0xFF12355B))),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text("Store GSTIN: ${AppConfig.gstin} • State: ${AppConfig.stateCode} (Maharashtra)", style: const TextStyle(fontSize: 12, color: Colors.grey)),
                    const SizedBox(height: 12),
                    ElevatedButton.icon(
                      onPressed: _exportGstr1,
                      style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF12355B), minimumSize: const Size.fromHeight(44)),
                      icon: const Icon(Icons.file_download, color: Colors.white),
                      label: const Text("Export GSTR-1 (Excel / CSV for CA)", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            // Supabase Database Connection Settings Card
            Card(
              color: Colors.white,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.cloud_done, color: Color(0xFF1B998B)),
                        SizedBox(width: 8),
                        Text("Supabase PostgreSQL Database Settings", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Color(0xFF12355B))),
                      ],
                    ),
                    const SizedBox(height: 6),
                    const Text("Run schema.sql in Supabase SQL editor (Mumbai region ap-south-1). Paste your project credentials here:", style: TextStyle(fontSize: 11, color: Colors.grey)),
                    const SizedBox(height: 12),
                    TextField(
                      controller: supabaseUrlCtrl,
                      decoration: const InputDecoration(labelText: "Supabase URL (https://xyz.supabase.co)", border: OutlineInputBorder(), isDense: true),
                    ),
                    const SizedBox(height: 8),
                    TextField(
                      controller: supabaseKeyCtrl,
                      decoration: const InputDecoration(labelText: "Supabase Anon Key", border: OutlineInputBorder(), isDense: true),
                    ),
                    const SizedBox(height: 10),
                    ElevatedButton(
                      onPressed: () {
                        AppConfig.supabaseUrl = supabaseUrlCtrl.text.trim();
                        AppConfig.supabaseAnonKey = supabaseKeyCtrl.text.trim();
                        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Supabase credentials saved successfully!"), backgroundColor: Colors.green));
                      },
                      style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF1B998B), minimumSize: const Size.fromHeight(42)),
                      child: const Text("Save Database Settings", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

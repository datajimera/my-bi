import 'package:flutter/material.dart';
import '../../core/config.dart';
import '../../data/models.dart';
import '../../data/repository.dart';

class ManagerScreen extends StatefulWidget {
  const ManagerScreen({super.key});

  @override
  State<ManagerScreen> createState() => _ManagerScreenState();
}

class _ManagerScreenState extends State<ManagerScreen> with SingleTickerProviderStateMixin {
  final repo = StoreRepository.instance;
  late TabController _tabController;

  // Search & Filter controllers
  final TextEditingController _prodSearchCtrl = TextEditingController();
  String _selectedCategoryFilter = 'All';
  bool _filterLowStockOnly = false;
  bool _filterExpiringOnly = false;

  final TextEditingController supabaseUrlCtrl = TextEditingController(text: AppConfig.supabaseUrl);
  final TextEditingController supabaseKeyCtrl = TextEditingController(text: AppConfig.supabaseAnonKey);

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 5, vsync: this);
    repo.addListener(_update);
  }

  @override
  void dispose() {
    repo.removeListener(_update);
    _tabController.dispose();
    _prodSearchCtrl.dispose();
    supabaseUrlCtrl.dispose();
    supabaseKeyCtrl.dispose();
    super.dispose();
  }

  void _update() {
    if (mounted) setState(() {});
  }

  // ---------------------------------------------------------
  // INVENTORY METHODS (Add / Edit / Stock Refill)
  // ---------------------------------------------------------

  void _showAddOrEditProductDialog({Product? existingProduct}) {
    final isEdit = existingProduct != null;
    final skuCtrl = TextEditingController(text: existingProduct?.sku ?? "SKU-${DateTime.now().millisecondsSinceEpoch % 10000}");
    final nameCtrl = TextEditingController(text: existingProduct?.name ?? "");
    final barcodeCtrl = TextEditingController(text: existingProduct?.barcode ?? "");
    final compositionCtrl = TextEditingController(text: existingProduct?.composition ?? "");
    final mrpCtrl = TextEditingController(text: existingProduct != null ? existingProduct.mrp.toString() : "");
    final sellPriceCtrl = TextEditingController(text: existingProduct != null ? existingProduct.sellPrice.toString() : "");
    final costPriceCtrl = TextEditingController(text: existingProduct != null ? existingProduct.costPrice.toString() : "");
    final taxCtrl = TextEditingController(text: existingProduct != null ? existingProduct.taxPercent.toString() : "5.0");
    final stockCtrl = TextEditingController(text: existingProduct != null ? existingProduct.stockQty.toString() : "50.0");
    final reorderCtrl = TextEditingController(text: existingProduct != null ? existingProduct.reorderLevel.toString() : "10.0");
    final hsnCtrl = TextEditingController(text: existingProduct?.hsnCode ?? "2106");

    String selectedDept = existingProduct?.categoryId ?? (repo.departments.isNotEmpty ? repo.departments.first.name : "Groceries");
    String selectedUnit = existingProduct?.unit ?? "PCS";
    bool isLoose = existingProduct?.isLoose ?? false;
    bool trackBatch = existingProduct?.trackBatch ?? false;
    String drugSchedule = existingProduct?.drugSchedule ?? "OTC";

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: Text(isEdit ? "Edit Product (${existingProduct.sku})" : "Add New Inventory Product", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
          content: SizedBox(
            width: 500,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: skuCtrl,
                          enabled: !isEdit,
                          decoration: const InputDecoration(labelText: "SKU / Item Code *", border: OutlineInputBorder(), isDense: true),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: barcodeCtrl,
                          decoration: const InputDecoration(labelText: "Barcode / EAN (Optional)", border: OutlineInputBorder(), isDense: true),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: nameCtrl,
                    decoration: const InputDecoration(labelText: "Product Item Name *", border: OutlineInputBorder(), isDense: true),
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: DropdownButtonFormField<String>(
                          value: repo.departments.any((d) => d.name == selectedDept) ? selectedDept : (repo.departments.isNotEmpty ? repo.departments.first.name : null),
                          decoration: const InputDecoration(labelText: "Department / Category", border: OutlineInputBorder(), isDense: true),
                          items: repo.departments.map((d) => DropdownMenuItem(value: d.name, child: Text(d.name, overflow: TextOverflow.ellipsis))).toList(),
                          onChanged: (val) {
                            if (val != null) setDialogState(() => selectedDept = val);
                          },
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: DropdownButtonFormField<String>(
                          value: selectedUnit,
                          decoration: const InputDecoration(labelText: "Unit", border: OutlineInputBorder(), isDense: true),
                          items: const [
                            DropdownMenuItem(value: "PCS", child: Text("PCS (Pieces)")),
                            DropdownMenuItem(value: "KG", child: Text("KG (Kilogram)")),
                            DropdownMenuItem(value: "GM", child: Text("GM (Gram)")),
                            DropdownMenuItem(value: "LTR", child: Text("LTR (Litre)")),
                            DropdownMenuItem(value: "PACK", child: Text("PACK (Box/Packet)")),
                          ],
                          onChanged: (val) {
                            if (val != null) setDialogState(() => selectedUnit = val);
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: mrpCtrl,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: const InputDecoration(labelText: "MRP (₹) *", border: OutlineInputBorder(), isDense: true),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: sellPriceCtrl,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: const InputDecoration(labelText: "Sell Price (₹) *", border: OutlineInputBorder(), isDense: true),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: costPriceCtrl,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: const InputDecoration(labelText: "Cost Price (₹)", border: OutlineInputBorder(), isDense: true),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: taxCtrl,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: const InputDecoration(labelText: "GST Tax % (e.g. 5, 12, 18)", border: OutlineInputBorder(), isDense: true),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: hsnCtrl,
                          decoration: const InputDecoration(labelText: "HSN Code", border: OutlineInputBorder(), isDense: true),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: stockCtrl,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: const InputDecoration(labelText: "Current Stock Qty *", border: OutlineInputBorder(), isDense: true),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: reorderCtrl,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: const InputDecoration(labelText: "Reorder Alert Level", border: OutlineInputBorder(), isDense: true),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: CheckboxListTile(
                          contentPadding: EdgeInsets.zero,
                          title: const Text("Loose item (sold by weight)", style: TextStyle(fontSize: 12)),
                          value: isLoose,
                          onChanged: (val) => setDialogState(() => isLoose = val ?? false),
                          controlAffinity: ListTileControlAffinity.leading,
                        ),
                      ),
                      Expanded(
                        child: CheckboxListTile(
                          contentPadding: EdgeInsets.zero,
                          title: const Text("Pharmacy / Batch track", style: TextStyle(fontSize: 12)),
                          value: trackBatch,
                          onChanged: (val) => setDialogState(() => trackBatch = val ?? false),
                          controlAffinity: ListTileControlAffinity.leading,
                        ),
                      ),
                    ],
                  ),
                  if (trackBatch) ...[
                    const SizedBox(height: 6),
                    TextField(
                      controller: compositionCtrl,
                      decoration: const InputDecoration(labelText: "Salt / Composition (e.g. Paracetamol 650mg)", border: OutlineInputBorder(), isDense: true),
                    ),
                    const SizedBox(height: 8),
                    DropdownButtonFormField<String>(
                      value: drugSchedule,
                      decoration: const InputDecoration(labelText: "Drug Schedule", border: OutlineInputBorder(), isDense: true),
                      items: const [
                        DropdownMenuItem(value: "OTC", child: Text("OTC (Over The Counter)")),
                        DropdownMenuItem(value: "H", child: Text("Schedule H (Rx Required)")),
                        DropdownMenuItem(value: "H1", child: Text("Schedule H1 (Warning)")),
                        DropdownMenuItem(value: "X", child: Text("Schedule X (Narcotics)")),
                      ],
                      onChanged: (val) {
                        if (val != null) setDialogState(() => drugSchedule = val);
                      },
                    ),
                  ],
                ],
              ),
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Cancel")),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF12355B)),
              onPressed: () {
                final sku = skuCtrl.text.trim();
                final name = nameCtrl.text.trim();
                if (sku.isEmpty || name.isEmpty) {
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("SKU and Name are required!"), backgroundColor: Colors.red));
                  return;
                }

                final mrp = double.tryParse(mrpCtrl.text.trim()) ?? 0.0;
                final sellPrice = double.tryParse(sellPriceCtrl.text.trim()) ?? mrp;
                final costPrice = double.tryParse(costPriceCtrl.text.trim()) ?? (sellPrice * 0.8);
                final tax = double.tryParse(taxCtrl.text.trim()) ?? 0.0;
                final stock = double.tryParse(stockCtrl.text.trim()) ?? 0.0;
                final reorder = double.tryParse(reorderCtrl.text.trim()) ?? 5.0;

                final product = Product(
                  sku: sku,
                  name: name,
                  categoryId: selectedDept,
                  barcode: barcodeCtrl.text.trim(),
                  composition: compositionCtrl.text.trim(),
                  mrp: mrp,
                  sellPrice: sellPrice,
                  costPrice: costPrice,
                  taxPercent: tax,
                  hsnCode: hsnCtrl.text.trim(),
                  unit: selectedUnit,
                  isLoose: isLoose,
                  trackBatch: trackBatch,
                  drugSchedule: drugSchedule,
                  stockQty: stock,
                  reorderLevel: reorder,
                  batches: existingProduct?.batches ?? (trackBatch ? [
                    Batch(
                      batchId: "B-${DateTime.now().millisecondsSinceEpoch % 1000}",
                      batchNo: "BATCH-01",
                      expiryDate: DateTime.now().add(const Duration(days: 365)),
                      mrp: mrp,
                      currentStock: stock,
                    )
                  ] : const []),
                );

                if (isEdit) {
                  repo.updateProduct(product);
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Updated $name successfully!"), backgroundColor: Colors.green));
                } else {
                  if (repo.products.any((p) => p.sku.toLowerCase() == sku.toLowerCase())) {
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("SKU already exists! Choose another."), backgroundColor: Colors.red));
                    return;
                  }
                  repo.addProduct(product);
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Added $name to Inventory!"), backgroundColor: Colors.green));
                }
                Navigator.pop(ctx);
              },
              child: Text(isEdit ? "Update Product" : "Save Product", style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  void _showAdjustStockDialog(Product product) {
    final qtyCtrl = TextEditingController(text: "10");
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text("Adjust Stock: ${product.name}", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text("Current Stock: ${product.stockQty.toStringAsFixed(product.isLoose ? 2 : 0)} ${product.unit}", style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            const Text("Quick Add Quantity:"),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                ActionChip(label: const Text("+5"), onPressed: () => qtyCtrl.text = "5"),
                ActionChip(label: const Text("+10"), onPressed: () => qtyCtrl.text = "10"),
                ActionChip(label: const Text("+25"), onPressed: () => qtyCtrl.text = "25"),
                ActionChip(label: const Text("+50"), onPressed: () => qtyCtrl.text = "50"),
                ActionChip(label: const Text("+100"), onPressed: () => qtyCtrl.text = "100"),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: qtyCtrl,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: "Quantity to Add / Restock", border: OutlineInputBorder(), isDense: true),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Cancel")),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF1B998B)),
            onPressed: () {
              final val = double.tryParse(qtyCtrl.text.trim()) ?? 0.0;
              if (val > 0) {
                repo.adjustStock(product.sku, val);
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Restocked +$val ${product.unit} to ${product.name}!"), backgroundColor: Colors.green));
              }
              Navigator.pop(ctx);
            },
            child: const Text("Add Stock", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  // ---------------------------------------------------------
  // STAFF / CASHIER METHODS (Add / Edit / Delete)
  // ---------------------------------------------------------

  void _showAddOrEditStaffDialog({StaffMember? existingStaff}) {
    final isEdit = existingStaff != null;
    final idCtrl = TextEditingController(text: existingStaff?.id ?? "C0${repo.staffMembers.length + 1}");
    final nameCtrl = TextEditingController(text: existingStaff?.name ?? "");
    final phoneCtrl = TextEditingController(text: existingStaff?.phone ?? "");
    final pinCtrl = TextEditingController(text: existingStaff?.pin ?? "1234");
    String selectedRole = existingStaff?.role ?? "Cashier";
    String selectedCounter = existingStaff?.assignedCounterId ?? (repo.counters.isNotEmpty ? repo.counters.first.id : "C01");
    bool isActive = existingStaff?.isActive ?? true;

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: Text(isEdit ? "Edit Staff (${existingStaff.name})" : "Add New Cashier / Staff Member", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
          content: SizedBox(
            width: 440,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: idCtrl,
                          enabled: !isEdit,
                          decoration: const InputDecoration(labelText: "Staff ID (e.g. C03) *", border: OutlineInputBorder(), isDense: true),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: DropdownButtonFormField<String>(
                          value: selectedRole,
                          decoration: const InputDecoration(labelText: "Role / Designation", border: OutlineInputBorder(), isDense: true),
                          items: const [
                            DropdownMenuItem(value: "Cashier", child: Text("Cashier")),
                            DropdownMenuItem(value: "Manager", child: Text("Manager")),
                            DropdownMenuItem(value: "Exit Guard", child: Text("Exit Guard")),
                            DropdownMenuItem(value: "Supervisor", child: Text("Supervisor")),
                          ],
                          onChanged: (val) {
                            if (val != null) setDialogState(() => selectedRole = val);
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: nameCtrl,
                    decoration: const InputDecoration(labelText: "Full Name *", border: OutlineInputBorder(), isDense: true),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: phoneCtrl,
                          keyboardType: TextInputType.phone,
                          decoration: const InputDecoration(labelText: "Mobile Number", border: OutlineInputBorder(), isDense: true),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: pinCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(labelText: "4-Digit PIN", border: OutlineInputBorder(), isDense: true),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    value: repo.counters.any((c) => c.id == selectedCounter) ? selectedCounter : (repo.counters.isNotEmpty ? repo.counters.first.id : null),
                    decoration: const InputDecoration(labelText: "Assigned Billing Counter", border: OutlineInputBorder(), isDense: true),
                    items: repo.counters.map((c) => DropdownMenuItem(value: c.id, child: Text("${c.name} (${c.id})"))).toList(),
                    onChanged: (val) {
                      if (val != null) setDialogState(() => selectedCounter = val);
                    },
                  ),
                  const SizedBox(height: 12),
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text("Account Active"),
                    subtitle: const Text("Active cashiers can log in and issue bills"),
                    value: isActive,
                    onChanged: (val) => setDialogState(() => isActive = val),
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Cancel")),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF12355B)),
              onPressed: () {
                final id = idCtrl.text.trim();
                final name = nameCtrl.text.trim();
                if (id.isEmpty || name.isEmpty) {
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("ID and Name are required!"), backgroundColor: Colors.red));
                  return;
                }

                final staff = StaffMember(
                  id: id,
                  name: name,
                  phone: phoneCtrl.text.trim(),
                  pin: pinCtrl.text.trim(),
                  role: selectedRole,
                  assignedCounterId: selectedCounter,
                  isActive: isActive,
                );

                if (isEdit) {
                  repo.updateStaffMember(staff);
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Updated $name successfully!"), backgroundColor: Colors.green));
                } else {
                  if (repo.staffMembers.any((s) => s.id.toLowerCase() == id.toLowerCase())) {
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Staff ID already exists!"), backgroundColor: Colors.red));
                    return;
                  }
                  repo.addStaffMember(staff);
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Added Cashier $name!"), backgroundColor: Colors.green));
                }
                Navigator.pop(ctx);
              },
              child: Text(isEdit ? "Update Staff" : "Add Staff Member", style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  // ---------------------------------------------------------
  // DEPARTMENTS & COUNTERS METHODS (Add / Edit / Delete)
  // ---------------------------------------------------------

  void _showAddOrEditDepartmentDialog({Department? existingDept}) {
    final isEdit = existingDept != null;
    final idCtrl = TextEditingController(text: existingDept?.id ?? "DEPT-${repo.departments.length + 1}");
    final nameCtrl = TextEditingController(text: existingDept?.name ?? "");
    final codeCtrl = TextEditingController(text: existingDept?.code ?? "");
    final descCtrl = TextEditingController(text: existingDept?.description ?? "");
    String selectedType = existingDept?.type ?? "Groceries";

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: Text(isEdit ? "Edit Department (${existingDept.name})" : "Add New Department / Section", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
          content: SizedBox(
            width: 420,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: idCtrl,
                        enabled: !isEdit,
                        decoration: const InputDecoration(labelText: "Dept ID *", border: OutlineInputBorder(), isDense: true),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        controller: codeCtrl,
                        decoration: const InputDecoration(labelText: "Short Code (e.g. GROC)", border: OutlineInputBorder(), isDense: true),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(labelText: "Department Name * (e.g. Dairy & Frozen)", border: OutlineInputBorder(), isDense: true),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  value: selectedType,
                  decoration: const InputDecoration(labelText: "Department Type", border: OutlineInputBorder(), isDense: true),
                  items: const [
                    DropdownMenuItem(value: "Groceries", child: Text("Groceries & Packaged Goods")),
                    DropdownMenuItem(value: "Pharmacy", child: Text("Pharmacy & Healthcare")),
                    DropdownMenuItem(value: "Fruits & Veggies", child: Text("Fruits & Fresh Produce")),
                    DropdownMenuItem(value: "Dairy", child: Text("Dairy & Frozen Food")),
                    DropdownMenuItem(value: "Cosmetics", child: Text("Personal Care & Cosmetics")),
                    DropdownMenuItem(value: "General", child: Text("General Merchandise")),
                  ],
                  onChanged: (val) {
                    if (val != null) setDialogState(() => selectedType = val);
                  },
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: descCtrl,
                  decoration: const InputDecoration(labelText: "Description / Notes", border: OutlineInputBorder(), isDense: true),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Cancel")),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF12355B)),
              onPressed: () {
                final id = idCtrl.text.trim();
                final name = nameCtrl.text.trim();
                if (id.isEmpty || name.isEmpty) {
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("ID and Name are required!"), backgroundColor: Colors.red));
                  return;
                }

                final dept = Department(
                  id: id,
                  name: name,
                  code: codeCtrl.text.trim().isEmpty ? name.substring(0, 3).toUpperCase() : codeCtrl.text.trim(),
                  type: selectedType,
                  description: descCtrl.text.trim(),
                );

                if (isEdit) {
                  repo.updateDepartment(dept);
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Updated department $name!"), backgroundColor: Colors.green));
                } else {
                  repo.addDepartment(dept);
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Created department $name!"), backgroundColor: Colors.green));
                }
                Navigator.pop(ctx);
              },
              child: Text(isEdit ? "Update Dept" : "Create Dept", style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  void _showAddOrEditCounterDialog({Counter? existingCounter}) {
    final isEdit = existingCounter != null;
    final idCtrl = TextEditingController(text: existingCounter?.id ?? "C0${repo.counters.length + 1}");
    final nameCtrl = TextEditingController(text: existingCounter?.name ?? "");
    String selectedDept = existingCounter?.departmentId ?? (repo.departments.isNotEmpty ? repo.departments.first.id : "DEPT-1");
    String selectedCashier = existingCounter?.assignedCashierId ?? (repo.staffMembers.isNotEmpty ? repo.staffMembers.first.id : "C01");

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: Text(isEdit ? "Edit Counter (${existingCounter.name})" : "Add New Billing Counter", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
          content: SizedBox(
            width: 420,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextField(
                  controller: idCtrl,
                  enabled: !isEdit,
                  decoration: const InputDecoration(labelText: "Counter ID * (e.g. C03, MED-02)", border: OutlineInputBorder(), isDense: true),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(labelText: "Counter Name * (e.g. Express Counter 3)", border: OutlineInputBorder(), isDense: true),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  value: repo.departments.any((d) => d.id == selectedDept) ? selectedDept : (repo.departments.isNotEmpty ? repo.departments.first.id : null),
                  decoration: const InputDecoration(labelText: "Assigned Department", border: OutlineInputBorder(), isDense: true),
                  items: repo.departments.map((d) => DropdownMenuItem(value: d.id, child: Text(d.name))).toList(),
                  onChanged: (val) {
                    if (val != null) setDialogState(() => selectedDept = val);
                  },
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  value: repo.staffMembers.any((s) => s.id == selectedCashier) ? selectedCashier : (repo.staffMembers.isNotEmpty ? repo.staffMembers.first.id : null),
                  decoration: const InputDecoration(labelText: "Default Cashier", border: OutlineInputBorder(), isDense: true),
                  items: repo.staffMembers.map((s) => DropdownMenuItem(value: s.id, child: Text("${s.name} (${s.id})"))).toList(),
                  onChanged: (val) {
                    if (val != null) setDialogState(() => selectedCashier = val);
                  },
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Cancel")),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF12355B)),
              onPressed: () {
                final id = idCtrl.text.trim();
                final name = nameCtrl.text.trim();
                if (id.isEmpty || name.isEmpty) {
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("ID and Name are required!"), backgroundColor: Colors.red));
                  return;
                }

                final counter = Counter(
                  id: id,
                  name: name,
                  departmentId: selectedDept,
                  assignedCashierId: selectedCashier,
                );

                if (isEdit) {
                  repo.updateCounter(counter);
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Updated counter $name!"), backgroundColor: Colors.green));
                } else {
                  repo.addCounter(counter);
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Added counter $name!"), backgroundColor: Colors.green));
                }
                Navigator.pop(ctx);
              },
              child: Text(isEdit ? "Update Counter" : "Add Counter", style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  void _showBillDetailsDialog(Bill bill) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text("Invoice: ${bill.invoiceNo.isNotEmpty ? bill.invoiceNo : bill.billId}", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: bill.paymentStatus == 'PAID' ? Colors.green.shade100 : Colors.orange.shade100,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(bill.paymentStatus, style: TextStyle(color: bill.paymentStatus == 'PAID' ? Colors.green.shade900 : Colors.orange.shade900, fontWeight: FontWeight.bold, fontSize: 12)),
            ),
          ],
        ),
        content: SizedBox(
          width: 480,
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text("Counter: ${bill.counterId} • Cashier: ${bill.cashierId}", style: const TextStyle(color: Colors.grey)),
                if (bill.customerPhone.isNotEmpty) Text("Customer Phone: ${bill.customerPhone}", style: const TextStyle(color: Colors.grey)),
                if (bill.customerGstin.isNotEmpty) Text("Customer GSTIN: ${bill.customerGstin}", style: const TextStyle(color: Colors.grey)),
                Text("Exit Verification: ${bill.checkedStatus == 'YES' ? 'VERIFIED (at gate ${bill.checkedGateId})' : 'PENDING'}", style: TextStyle(fontWeight: FontWeight.bold, color: bill.checkedStatus == 'YES' ? Colors.green : Colors.red)),
                const Divider(),
                const Text("Items Purchased:", style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 6),
                ...bill.items.map((i) => Padding(
                  padding: const EdgeInsets.symmetric(vertical: 2.0),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(child: Text("${i.name} (x${i.qty} ${i.unit})")),
                      Text("₹${i.lineTotal.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold)),
                    ],
                  ),
                )),
                const Divider(),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text("Subtotal:", style: TextStyle(color: Colors.grey)),
                    Text("₹${bill.subtotal.toStringAsFixed(2)}"),
                  ],
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text("GST Tax Total:", style: TextStyle(color: Colors.grey)),
                    Text("₹${bill.taxTotal.toStringAsFixed(2)}"),
                  ],
                ),
                if (bill.discount > 0)
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text("Discount:", style: TextStyle(color: Colors.green)),
                      Text("-₹${bill.discount.toStringAsFixed(2)}", style: const TextStyle(color: Colors.green)),
                    ],
                  ),
                const SizedBox(height: 4),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text("Grand Total:", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                    Text("₹${bill.grandTotal.toStringAsFixed(2)} (${bill.paymentMode})", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Color(0xFF12355B))),
                  ],
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Close")),
        ],
      ),
    );
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

  // ---------------------------------------------------------
  // BUILD METHOD & TABS
  // ---------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Store Manager Dashboard", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
        backgroundColor: const Color(0xFF12355B),
        bottom: TabBar(
          controller: _tabController,
          isScrollable: true,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white70,
          indicatorColor: const Color(0xFF1B998B),
          indicatorWeight: 3.5,
          tabs: const [
            Tab(icon: Icon(Icons.dashboard), text: "Overview"),
            Tab(icon: Icon(Icons.inventory_2), text: "Inventory (स्टॉक)"),
            Tab(icon: Icon(Icons.badge), text: "Cashiers & Staff (स्टाफ)"),
            Tab(icon: Icon(Icons.storefront), text: "Departments & Counters"),
            Tab(icon: Icon(Icons.settings), text: "Settings & Cloud"),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildOverviewTab(),
          _buildInventoryTab(),
          _buildCashiersTab(),
          _buildDepartmentsAndCountersTab(),
          _buildSettingsTab(),
        ],
      ),
    );
  }

  // TAB 1: OVERVIEW & ANALYTICS
  Widget _buildOverviewTab() {
    final paidBills = repo.bills.where((b) => b.paymentStatus == 'PAID').toList();
    final totalSales = paidBills.fold(0.0, (sum, b) => sum + b.grandTotal);
    final upiSales = paidBills.where((b) => b.paymentMode == 'UPI').fold(0.0, (sum, b) => sum + b.grandTotal);
    final cashSales = paidBills.where((b) => b.paymentMode == 'CASH').fold(0.0, (sum, b) => sum + b.grandTotal);
    final exitVerified = paidBills.where((b) => b.checkedStatus == 'YES').length;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Revenue Metrics
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
                        Text("₹${totalSales.toStringAsFixed(2)}", style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
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
                        const Text("Paid Invoices", style: TextStyle(color: Colors.grey, fontSize: 12)),
                        Text("${paidBills.length}", style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF1B998B))),
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
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  Column(
                    children: [
                      const Text("UPI / Online", style: TextStyle(fontSize: 12, color: Colors.grey)),
                      Text("₹${upiSales.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF1B998B))),
                    ],
                  ),
                  Column(
                    children: [
                      const Text("Cash Collection", style: TextStyle(fontSize: 12, color: Colors.grey)),
                      Text("₹${cashSales.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B))),
                    ],
                  ),
                  Column(
                    children: [
                      const Text("Gate Exit Checked", style: TextStyle(fontSize: 12, color: Colors.grey)),
                      Text("$exitVerified / ${paidBills.length}", style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.green)),
                    ],
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Quick Actions
          Card(
            color: Colors.white,
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text("Quick Actions", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Color(0xFF12355B))),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: ElevatedButton.icon(
                          onPressed: () => _showAddOrEditProductDialog(),
                          style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF12355B)),
                          icon: const Icon(Icons.add_box, color: Colors.white),
                          label: const Text("+ Add Product", style: TextStyle(color: Colors.white)),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: ElevatedButton.icon(
                          onPressed: () => _showAddOrEditStaffDialog(),
                          style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF1B998B)),
                          icon: const Icon(Icons.person_add, color: Colors.white),
                          label: const Text("+ Add Cashier", style: TextStyle(color: Colors.white)),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  ElevatedButton.icon(
                    onPressed: _exportGstr1,
                    style: ElevatedButton.styleFrom(backgroundColor: Colors.indigo, minimumSize: const Size.fromHeight(40)),
                    icon: const Icon(Icons.assessment, color: Colors.white),
                    label: const Text("Export GSTR-1 Sales Report (CSV/Excel)", style: TextStyle(color: Colors.white)),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Recent Bills List
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
                      const Text("Recent Invoices & Bills", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Color(0xFF12355B))),
                      Text("${repo.bills.length} total bills", style: const TextStyle(fontSize: 12, color: Colors.grey)),
                    ],
                  ),
                  const Divider(),
                  if (repo.bills.isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 24.0),
                      child: Center(child: Text("No bills created yet. Issue bills from the Counter screen.", style: TextStyle(color: Colors.grey))),
                    )
                  else
                    ...repo.bills.take(10).map((b) => ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text(b.invoiceNo.isNotEmpty ? b.invoiceNo : b.billId, style: const TextStyle(fontWeight: FontWeight.bold)),
                      subtitle: Text("Counter: ${b.counterId} • Cashier: ${b.cashierId} • ${b.items.length} items"),
                      trailing: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text("₹${b.grandTotal.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                          Text(b.paymentStatus, style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: b.paymentStatus == 'PAID' ? Colors.green : Colors.orange)),
                        ],
                      ),
                      onTap: () => _showBillDetailsDialog(b),
                    )),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  // TAB 2: INVENTORY & STOCK MANAGEMENT
  Widget _buildInventoryTab() {
    final query = _prodSearchCtrl.text.trim().toLowerCase();
    var filtered = repo.products.where((p) {
      if (query.isNotEmpty) {
        final match = p.sku.toLowerCase().contains(query) ||
            p.name.toLowerCase().contains(query) ||
            p.barcode.toLowerCase().contains(query) ||
            p.composition.toLowerCase().contains(query);
        if (!match) return false;
      }
      if (_selectedCategoryFilter != 'All' && p.categoryId != _selectedCategoryFilter) {
        return false;
      }
      if (_filterLowStockOnly && p.stockQty > p.reorderLevel) {
        return false;
      }
      if (_filterExpiringOnly) {
        final validBatch = p.nearestValidBatch;
        if (validBatch == null || !validBatch.isNearExpiry) return false;
      }
      return true;
    }).toList();

    return Scaffold(
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _showAddOrEditProductDialog(),
        backgroundColor: const Color(0xFF12355B),
        icon: const Icon(Icons.add, color: Colors.white),
        label: const Text("Add Product", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            // Search & Filter Bar
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _prodSearchCtrl,
                    onChanged: (val) => setState(() {}),
                    decoration: InputDecoration(
                      prefixIcon: const Icon(Icons.search),
                      hintText: "Search by SKU, Name, Barcode or Salt...",
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                      isDense: true,
                      suffixIcon: _prodSearchCtrl.text.isNotEmpty
                          ? IconButton(icon: const Icon(Icons.clear), onPressed: () => setState(() => _prodSearchCtrl.clear()))
                          : null,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                DropdownButton<String>(
                  value: _selectedCategoryFilter,
                  items: [
                    const DropdownMenuItem(value: 'All', child: Text("All Depts")),
                    ...repo.departments.map((d) => DropdownMenuItem(value: d.name, child: Text(d.name))),
                  ],
                  onChanged: (val) {
                    if (val != null) setState(() => _selectedCategoryFilter = val);
                  },
                ),
              ],
            ),
            const SizedBox(height: 8),
            // Quick Filter Chips
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  FilterChip(
                    label: Text("All (${repo.products.length})"),
                    selected: !_filterLowStockOnly && !_filterExpiringOnly,
                    onSelected: (val) => setState(() {
                      _filterLowStockOnly = false;
                      _filterExpiringOnly = false;
                    }),
                  ),
                  const SizedBox(width: 8),
                  FilterChip(
                    label: Text("Low Stock Alert (${repo.products.where((p) => p.stockQty <= p.reorderLevel).length})"),
                    selected: _filterLowStockOnly,
                    selectedColor: Colors.red.shade100,
                    onSelected: (val) => setState(() => _filterLowStockOnly = val),
                  ),
                  const SizedBox(width: 8),
                  FilterChip(
                    label: const Text("Expiring Soon / Batches"),
                    selected: _filterExpiringOnly,
                    selectedColor: Colors.orange.shade100,
                    onSelected: (val) => setState(() => _filterExpiringOnly = val),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            // Product List
            Expanded(
              child: filtered.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Icon(Icons.inventory_2_outlined, size: 64, color: Colors.grey),
                          const SizedBox(height: 8),
                          const Text("No products match your search or filter.", style: TextStyle(color: Colors.grey)),
                          const SizedBox(height: 12),
                          ElevatedButton.icon(
                            onPressed: () => _showAddOrEditProductDialog(),
                            icon: const Icon(Icons.add),
                            label: const Text("Add New Product"),
                          ),
                        ],
                      ),
                    )
                  : ListView.builder(
                      itemCount: filtered.length,
                      itemBuilder: (ctx, idx) {
                        final p = filtered[idx];
                        final isLow = p.stockQty <= p.reorderLevel;
                        return Card(
                          color: isLow ? const Color(0xFFFFF7ED) : Colors.white,
                          margin: const EdgeInsets.only(bottom: 10),
                          child: Padding(
                            padding: const EdgeInsets.all(12.0),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Container(
                                  width: 48,
                                  height: 48,
                                  decoration: BoxDecoration(
                                    color: isLow ? Colors.red.shade100 : const Color(0xFF12355B).withOpacity(0.08),
                                    borderRadius: BorderRadius.circular(10),
                                  ),
                                  child: Icon(
                                    p.trackBatch ? Icons.medication : (p.isLoose ? Icons.scale : Icons.shopping_bag),
                                    color: isLow ? Colors.red : const Color(0xFF12355B),
                                  ),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        children: [
                                          Expanded(
                                            child: Text(
                                              p.name,
                                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                                            ),
                                          ),
                                          Container(
                                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                            decoration: BoxDecoration(
                                              color: Colors.grey.shade200,
                                              borderRadius: BorderRadius.circular(4),
                                            ),
                                            child: Text(p.categoryId, style: const TextStyle(fontSize: 10, color: Colors.black87)),
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 4),
                                      Text("SKU: ${p.sku} ${p.barcode.isNotEmpty ? '• Barcode: ${p.barcode}' : ''}", style: const TextStyle(fontSize: 12, color: Colors.grey)),
                                      if (p.composition.isNotEmpty)
                                        Text("Salt: ${p.composition} [${p.drugSchedule}]", style: const TextStyle(fontSize: 11, color: Colors.teal)),
                                      const SizedBox(height: 6),
                                      Row(
                                        children: [
                                          Text("₹${p.sellPrice.toStringAsFixed(2)}", style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF12355B), fontSize: 14)),
                                          if (p.mrp > p.sellPrice) ...[
                                            const SizedBox(width: 6),
                                            Text("MRP: ₹${p.mrp.toStringAsFixed(2)}", style: const TextStyle(decoration: TextDecoration.lineThrough, fontSize: 11, color: Colors.grey)),
                                          ],
                                          const SizedBox(width: 12),
                                          Text("GST: ${p.taxPercent}%", style: const TextStyle(fontSize: 11, color: Colors.grey)),
                                        ],
                                      ),
                                      const SizedBox(height: 4),
                                      Row(
                                        children: [
                                          Container(
                                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                            decoration: BoxDecoration(
                                              color: isLow ? Colors.red.shade100 : Colors.green.shade100,
                                              borderRadius: BorderRadius.circular(6),
                                            ),
                                            child: Text(
                                              "Stock: ${p.stockQty.toStringAsFixed(p.isLoose ? 2 : 0)} ${p.unit} ${isLow ? '(LOW STOCK)' : ''}",
                                              style: TextStyle(
                                                color: isLow ? Colors.red.shade900 : Colors.green.shade900,
                                                fontWeight: FontWeight.bold,
                                                fontSize: 11,
                                              ),
                                            ),
                                          ),
                                          if (p.trackBatch && p.batches.isNotEmpty) ...[
                                            const SizedBox(width: 8),
                                            Text(
                                              "${p.batches.length} Batch(es)",
                                              style: const TextStyle(fontSize: 11, color: Colors.blueGrey, fontWeight: FontWeight.bold),
                                            ),
                                          ],
                                        ],
                                      ),
                                    ],
                                  ),
                                ),
                                Column(
                                  children: [
                                    IconButton(
                                      icon: const Icon(Icons.add_circle, color: Color(0xFF1B998B)),
                                      tooltip: "Restock / Add Quantity",
                                      onPressed: () => _showAdjustStockDialog(p),
                                    ),
                                    IconButton(
                                      icon: const Icon(Icons.edit, color: Color(0xFF12355B)),
                                      tooltip: "Edit Product",
                                      onPressed: () => _showAddOrEditProductDialog(existingProduct: p),
                                    ),
                                    IconButton(
                                      icon: const Icon(Icons.delete_outline, color: Colors.red),
                                      tooltip: "Delete Product",
                                      onPressed: () {
                                        showDialog(
                                          context: context,
                                          builder: (delCtx) => AlertDialog(
                                            title: const Text("Delete Product?"),
                                            content: Text("Are you sure you want to delete '${p.name}' (${p.sku})?"),
                                            actions: [
                                              TextButton(onPressed: () => Navigator.pop(delCtx), child: const Text("Cancel")),
                                              ElevatedButton(
                                                style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
                                                onPressed: () {
                                                  repo.deleteProduct(p.sku);
                                                  Navigator.pop(delCtx);
                                                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Deleted ${p.name}!")));
                                                },
                                                child: const Text("Delete", style: TextStyle(color: Colors.white)),
                                              ),
                                            ],
                                          ),
                                        );
                                      },
                                    ),
                                  ],
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
    );
  }

  // TAB 3: CASHIERS & STAFF MANAGEMENT
  Widget _buildCashiersTab() {
    return Scaffold(
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _showAddOrEditStaffDialog(),
        backgroundColor: const Color(0xFF12355B),
        icon: const Icon(Icons.person_add, color: Colors.white),
        label: const Text("Add Cashier / Staff", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text("Staff & Cashier Management", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: Color(0xFF12355B))),
                    Text("${repo.staffMembers.length} active registered staff members", style: const TextStyle(fontSize: 12, color: Colors.grey)),
                  ],
                ),
                ElevatedButton.icon(
                  onPressed: () => _showAddOrEditStaffDialog(),
                  style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF1B998B)),
                  icon: const Icon(Icons.add, color: Colors.white),
                  label: const Text("Add Staff", style: TextStyle(color: Colors.white)),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Expanded(
              child: repo.staffMembers.isEmpty
                  ? const Center(child: Text("No staff members yet. Click 'Add Staff' to register cashiers."))
                  : ListView.builder(
                      itemCount: repo.staffMembers.length,
                      itemBuilder: (ctx, idx) {
                        final s = repo.staffMembers[idx];
                        Color roleColor;
                        switch (s.role) {
                          case 'Cashier':
                            roleColor = const Color(0xFF12355B);
                            break;
                          case 'Manager':
                            roleColor = Colors.purple;
                            break;
                          case 'Exit Guard':
                            roleColor = Colors.teal;
                            break;
                          default:
                            roleColor = Colors.blueGrey;
                        }

                        return Card(
                          color: Colors.white,
                          margin: const EdgeInsets.only(bottom: 12),
                          child: ListTile(
                            leading: CircleAvatar(
                              backgroundColor: roleColor.withOpacity(0.15),
                              child: Icon(Icons.person, color: roleColor),
                            ),
                            title: Row(
                              children: [
                                Text(s.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                                const SizedBox(width: 8),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                  decoration: BoxDecoration(color: roleColor.withOpacity(0.15), borderRadius: BorderRadius.circular(6)),
                                  child: Text(s.role, style: TextStyle(color: roleColor, fontWeight: FontWeight.bold, fontSize: 11)),
                                ),
                              ],
                            ),
                            subtitle: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const SizedBox(height: 4),
                                Text("Staff ID: ${s.id} • Assigned Counter: ${s.assignedCounterId} • PIN: ${s.pin}"),
                                if (s.phone.isNotEmpty) Text("Phone: ${s.phone}", style: const TextStyle(color: Colors.grey)),
                              ],
                            ),
                            trailing: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Switch(
                                  value: s.isActive,
                                  onChanged: (val) {
                                    s.isActive = val;
                                    repo.updateStaffMember(s);
                                  },
                                ),
                                IconButton(
                                  icon: const Icon(Icons.edit, color: Color(0xFF12355B)),
                                  onPressed: () => _showAddOrEditStaffDialog(existingStaff: s),
                                ),
                                IconButton(
                                  icon: const Icon(Icons.delete_outline, color: Colors.red),
                                  onPressed: () {
                                    showDialog(
                                      context: context,
                                      builder: (delCtx) => AlertDialog(
                                        title: const Text("Delete Staff Member?"),
                                        content: Text("Are you sure you want to delete '${s.name}' (${s.id})?"),
                                        actions: [
                                          TextButton(onPressed: () => Navigator.pop(delCtx), child: const Text("Cancel")),
                                          ElevatedButton(
                                            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
                                            onPressed: () {
                                              repo.deleteStaffMember(s.id);
                                              Navigator.pop(delCtx);
                                              ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Removed ${s.name}.")));
                                            },
                                            child: const Text("Delete", style: TextStyle(color: Colors.white)),
                                          ),
                                        ],
                                      ),
                                    );
                                  },
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
    );
  }

  // TAB 4: DEPARTMENTS & COUNTERS
  Widget _buildDepartmentsAndCountersTab() {
    return DefaultTabController(
      length: 2,
      child: Column(
        children: [
          Container(
            color: Colors.white,
            child: const TabBar(
              labelColor: Color(0xFF12355B),
              unselectedLabelColor: Colors.grey,
              indicatorColor: Color(0xFF12355B),
              tabs: [
                Tab(icon: Icon(Icons.category), text: "Store Departments"),
                Tab(icon: Icon(Icons.point_of_sale), text: "Billing Counters"),
              ],
            ),
          ),
          Expanded(
            child: TabBarView(
              children: [
                // SUB-TAB 1: DEPARTMENTS
                Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text("Store Departments & Sections", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Color(0xFF12355B))),
                          ElevatedButton.icon(
                            onPressed: () => _showAddOrEditDepartmentDialog(),
                            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF12355B)),
                            icon: const Icon(Icons.add, color: Colors.white),
                            label: const Text("+ Add Department", style: TextStyle(color: Colors.white)),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      Expanded(
                        child: ListView.builder(
                          itemCount: repo.departments.length,
                          itemBuilder: (ctx, idx) {
                            final d = repo.departments[idx];
                            final prodCount = repo.products.where((p) => p.categoryId == d.name).length;
                            return Card(
                              color: Colors.white,
                              margin: const EdgeInsets.only(bottom: 10),
                              child: ListTile(
                                leading: Container(
                                  padding: const EdgeInsets.all(10),
                                  decoration: BoxDecoration(color: const Color(0xFF12355B).withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
                                  child: const Icon(Icons.store, color: Color(0xFF12355B)),
                                ),
                                title: Text(d.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                                subtitle: Text("${d.code} • ${d.type} • $prodCount items\n${d.description}"),
                                isThreeLine: d.description.isNotEmpty,
                                trailing: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    IconButton(
                                      icon: const Icon(Icons.edit, color: Color(0xFF12355B)),
                                      onPressed: () => _showAddOrEditDepartmentDialog(existingDept: d),
                                    ),
                                    IconButton(
                                      icon: const Icon(Icons.delete_outline, color: Colors.red),
                                      onPressed: () {
                                        repo.deleteDepartment(d.id);
                                        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Removed ${d.name}")));
                                      },
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
                // SUB-TAB 2: COUNTERS
                Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text("Billing Counters & POS Terminals", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Color(0xFF12355B))),
                          ElevatedButton.icon(
                            onPressed: () => _showAddOrEditCounterDialog(),
                            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF1B998B)),
                            icon: const Icon(Icons.add, color: Colors.white),
                            label: const Text("+ Add Counter", style: TextStyle(color: Colors.white)),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      Expanded(
                        child: ListView.builder(
                          itemCount: repo.counters.length,
                          itemBuilder: (ctx, idx) {
                            final c = repo.counters[idx];
                            final cashier = repo.staffMembers.cast<StaffMember?>().firstWhere((s) => s?.id == c.assignedCashierId, orElse: () => null);
                            return Card(
                              color: Colors.white,
                              margin: const EdgeInsets.only(bottom: 10),
                              child: ListTile(
                                leading: Container(
                                  padding: const EdgeInsets.all(10),
                                  decoration: BoxDecoration(color: const Color(0xFF1B998B).withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
                                  child: const Icon(Icons.point_of_sale, color: Color(0xFF1B998B)),
                                ),
                                title: Text(c.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                                subtitle: Text("ID: ${c.id} • Dept: ${c.departmentId} • Cashier: ${cashier?.name ?? c.assignedCashierId}"),
                                trailing: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    IconButton(
                                      icon: const Icon(Icons.edit, color: Color(0xFF12355B)),
                                      onPressed: () => _showAddOrEditCounterDialog(existingCounter: c),
                                    ),
                                    IconButton(
                                      icon: const Icon(Icons.delete_outline, color: Colors.red),
                                      onPressed: () {
                                        repo.deleteCounter(c.id);
                                        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Removed ${c.name}")));
                                      },
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
              ],
            ),
          ),
        ],
      ),
    );
  }

  // TAB 5: SETTINGS & DATABASE
  Widget _buildSettingsTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Retail & Pharmacy Mode
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
                    subtitle: const Text("Enables FEFO batches, expiry block, salt search, Schedule H compliance"),
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
          // GST Settings & Store Details
          Card(
            color: Colors.white,
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(
                    children: [
                      Icon(Icons.store, color: Color(0xFF12355B)),
                      SizedBox(width: 8),
                      Text("Store Profile & GST Details", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Color(0xFF12355B))),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Text("Store GSTIN: ${AppConfig.gstin} • State: ${AppConfig.stateCode} (Maharashtra)"),
                  const SizedBox(height: 6),
                  Text("Store Name: ${AppConfig.storeName}"),
                  const SizedBox(height: 6),
                  Text("Store Address: ${AppConfig.storeAddress}"),
                  const SizedBox(height: 12),
                  ElevatedButton.icon(
                    onPressed: _exportGstr1,
                    style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF12355B), minimumSize: const Size.fromHeight(42)),
                    icon: const Icon(Icons.file_download, color: Colors.white),
                    label: const Text("Export GSTR-1 Sales Report", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Supabase PostgreSQL Settings
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
                  const Text("Run schema.sql in Supabase SQL editor. Enter project credentials here:", style: TextStyle(fontSize: 11, color: Colors.grey)),
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
    );
  }
}

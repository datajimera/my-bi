import 'package:flutter/material.dart';
import 'features/counter/counter_screen.dart';
import 'features/manager/manager_screen.dart';
import 'features/checking/checking_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const SmartBillingUnifiedApp());
}

class SmartBillingUnifiedApp extends StatelessWidget {
  const SmartBillingUnifiedApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Smart Billing System',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF12355B),
          primary: const Color(0xFF12355B),
          secondary: const Color(0xFF1B998B),
        ),
        scaffoldBackgroundColor: const Color(0xFFF8FAFC),
      ),
      home: const DepartmentPortalScreen(),
    );
  }
}

class DepartmentPortalScreen extends StatefulWidget {
  const DepartmentPortalScreen({super.key});

  @override
  State<DepartmentPortalScreen> createState() => _DepartmentPortalScreenState();
}

class _DepartmentPortalScreenState extends State<DepartmentPortalScreen> {
  int _currentIndex = 0;

  final List<Widget> _screens = const [
    CounterScreen(),
    CheckingScreen(),
    ManagerScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: _screens,
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (idx) => setState(() => _currentIndex = idx),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.point_of_sale),
            label: 'Counter (C01)',
          ),
          NavigationDestination(
            icon: Icon(Icons.security),
            label: 'Exit Check / Stock',
          ),
          NavigationDestination(
            icon: Icon(Icons.admin_panel_settings),
            label: 'Store Manager',
          ),
        ],
      ),
    );
  }
}

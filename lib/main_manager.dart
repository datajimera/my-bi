import 'package:flutter/material.dart';
import 'features/manager/manager_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const ManagerApp());
}

class ManagerApp extends StatelessWidget {
  const ManagerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Store Manager',
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
      home: const ManagerScreen(),
    );
  }
}

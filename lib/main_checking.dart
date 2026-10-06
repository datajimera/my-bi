import 'package:flutter/material.dart';
import 'features/checking/checking_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const CheckingApp());
}

class CheckingApp extends StatelessWidget {
  const CheckingApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Exit Checking Guard',
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
      home: const CheckingScreen(),
    );
  }
}

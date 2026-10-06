import 'package:flutter/material.dart';
import 'features/counter/counter_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const CounterApp());
}

class CounterApp extends StatelessWidget {
  const CounterApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Billing Counter',
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
      home: const CounterScreen(),
    );
  }
}

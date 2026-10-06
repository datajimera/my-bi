import 'dart:convert';
import 'package:http/http.dart' as http;
import 'config.dart';

class ApiClient {
  static Future<Map<String, dynamic>> postAction(
    String action,
    Map<String, dynamic> payload, {
    String? customUrl,
    String? customApiKey,
  }) async {
    final url = customUrl ?? AppConfig.appsScriptUrl;
    final apiKey = customApiKey ?? AppConfig.apiKey;

    if (url.trim().isEmpty) {
      return {'ok': false, 'error': 'Apps Script Web App URL is not set'};
    }

    try {
      final response = await http.post(
        Uri.parse(url),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'action': action,
          'apiKey': apiKey,
          'payload': payload,
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 302) {
        final decoded = jsonDecode(response.body);
        return decoded as Map<String, dynamic>;
      } else {
        return {
          'ok': false,
          'error': 'Server error (${response.statusCode}): ${response.body}'
        };
      }
    } catch (e) {
      return {'ok': false, 'error': e.toString()};
    }
  }

  static Future<Map<String, dynamic>> testConnection(String url, String apiKey) async {
    return await postAction('ping', {}, customUrl: url, customApiKey: apiKey);
  }
}

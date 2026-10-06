import 'dart:convert';
import 'package:crypto/crypto.dart';

class HmacHelper {
  static String generateReceiptSignature(String billId, String secretKey) {
    final key = utf8.encode(secretKey);
    final bytes = utf8.encode(billId);
    final hmacSha256 = Hmac(sha256, key);
    final digest = hmacSha256.convert(bytes);
    return digest.toString().substring(0, 16).toUpperCase();
  }

  static String buildReceiptQrContent(String billId, int timestamp, String secretKey) {
    final sig = generateReceiptSignature(billId, secretKey);
    return "$billId|$timestamp|$sig";
  }

  static Map<String, dynamic> verifyReceiptQr(String qrContent, String secretKey) {
    final parts = qrContent.split('|');
    if (parts.length < 3) {
      if (qrContent.startsWith("C") && qrContent.contains("-")) {
        return {'isValid': true, 'billId': qrContent.trim()};
      }
      return {'isValid': false, 'billId': ''};
    }
    final billId = parts[0].trim();
    final signature = parts[2].trim();
    final expectedSig = generateReceiptSignature(billId, secretKey);
    return {
      'isValid': signature.toUpperCase() == expectedSig.toUpperCase(),
      'billId': billId
    };
  }
}

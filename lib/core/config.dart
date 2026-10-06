class AppConfig {
  static const String appName = "Smart Billing System";
  static const String appVersion = "3.0.0";

  // Google Apps Script / Custom API Backend (for sync if used)
  static String appsScriptUrl = "";
  static String apiKey = "SmartBillingSecretKey2026";

  // Supabase Backend Credentials (Update v3)
  static String supabaseUrl = "https://your-project-id.supabase.co";
  static String supabaseAnonKey = "your-anon-key-here";

  // Security
  static String hmacSecret = "SecretKeyBilling2026Salted";

  // Store Defaults
  static String storeName = "Smart Supermarket & Pharmacy";
  static String storeAddress = "Shop 101, Main Avenue, Mumbai";
  static String storePhone = "+91 98765 43210";
  static String gstin = "27AAAAA0000A1Z5";
  static String stateCode = "27";
  static String upiVpa = "smartbilling@upi";
  static String upiPayeeName = "Smart Supermarket Billing";
  static String currency = "₹";

  // Retail & Pharmacy Mode settings
  static bool pharmacyMode = true;
  static int nearExpiryDays = 90;
  static String scannerMode = "Both"; // "Camera", "Gun", "Both"
}

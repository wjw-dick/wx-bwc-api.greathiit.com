class AppConfig {
  static const String baseUrl = String.fromEnvironment(
    'BWC_BASE_URL',
    defaultValue: 'https://wx-bwc-api.greathiit.com/api',
  );

  static const String defaultTenant = String.fromEnvironment(
    'BWC_TENANT',
    defaultValue: 'DEMO',
  );

  static const String appName = '校园保卫';
  static const String version = '1.0.0';
  static const String legalDomain = 'wx-bwc-api.greathiit.com';
}

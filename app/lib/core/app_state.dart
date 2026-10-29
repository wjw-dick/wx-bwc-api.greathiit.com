import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'api_client.dart';
import 'config.dart';

class AppState extends ChangeNotifier {
  AppState() {
    api = ApiClient()..onUnauthorized = _handleUnauthorized;
  }

  late final ApiClient api;
  late SharedPreferences _prefs;

  String? token;
  Map<String, dynamic>? user;
  Map<String, dynamic>? tenant;
  List<Map<String, dynamic>> menus = [];
  String locale = 'zh-CN';
  String tenantCode = AppConfig.defaultTenant;
  bool initialized = false;

  bool get loggedIn => token != null && token!.isNotEmpty;
  bool get isLeader {
    final role = (user?['roleCode'] ?? '').toString();
    return role == 'LEADER' || role == 'VISITOR';
  }

  String get realName => (user?['realName'] ?? '').toString().isEmpty
      ? (user?['username'] ?? '')
      : user!['realName'].toString();

  Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
    token = _prefs.getString('bwc_token');
    locale = _prefs.getString('bwc_locale') ?? 'zh-CN';
    tenantCode = _prefs.getString('bwc_tenant_code') ?? AppConfig.defaultTenant;
    user = _decodeMap(_prefs.getString('bwc_user'));
    tenant = _decodeMap(_prefs.getString('bwc_tenant'));
    api.token = token;
    initialized = true;
    notifyListeners();
    if (loggedIn) {
      await loadMenus();
    }
  }

  Map<String, dynamic>? _decodeMap(String? text) {
    if (text == null || text.isEmpty) return null;
    try {
      final data = jsonDecode(text);
      if (data is Map<String, dynamic>) return data;
    } catch (_) {}
    return null;
  }

  Future<void> login({
    required String username,
    required String password,
    required String code,
    String loginType = 'tenant',
  }) async {
    final data = await api.post('/auth/login', body: {
      'username': username,
      'password': password,
      'tenantCode': code,
      'loginType': loginType,
    });
    token = (data['token'] ?? '').toString();
    user = (data['user'] as Map?)?.cast<String, dynamic>();
    tenant = (data['tenant'] as Map?)?.cast<String, dynamic>();
    tenantCode = (tenant?['code'] ?? code).toString();
    api.token = token;
    await _prefs.setString('bwc_token', token ?? '');
    await _prefs.setString('bwc_user', jsonEncode(user ?? {}));
    await _prefs.setString('bwc_tenant', jsonEncode(tenant ?? {}));
    await _prefs.setString('bwc_tenant_code', tenantCode);
    notifyListeners();
    await loadMenus();
  }

  Future<void> loadMenus() async {
    try {
      final data = await api.get('/public/tenant/$tenantCode/menus', query: {
        'platform': 'app',
      });
      if (data is List) {
        menus = data.whereType<Map>().map((e) => e.cast<String, dynamic>()).toList();
        notifyListeners();
      }
    } catch (_) {
      // 后台菜单不可用时保留本地默认入口
    }
  }

  Future<void> setLocale(String value) async {
    locale = value;
    await _prefs.setString('bwc_locale', value);
    notifyListeners();
  }

  Future<void> refreshProfile() async {
    if (!loggedIn) return;
    try {
      final data = await api.get('/auth/me');
      if (data['user'] != null) {
        user = (data['user'] as Map).cast<String, dynamic>();
        await _prefs.setString('bwc_user', jsonEncode(user));
        notifyListeners();
      }
    } catch (_) {}
  }

  Future<void> logout() async {
    token = null;
    user = null;
    tenant = null;
    menus = [];
    api.token = null;
    await _prefs.remove('bwc_token');
    await _prefs.remove('bwc_user');
    await _prefs.remove('bwc_tenant');
    notifyListeners();
  }

  void _handleUnauthorized() {
    token = null;
    user = null;
    api.token = null;
    notifyListeners();
  }
}

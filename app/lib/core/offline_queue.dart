import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import 'api_client.dart';

class OfflineQueue {
  OfflineQueue(this.api);

  static const String _key = 'bwc_offline_queue';
  final ApiClient api;

  Future<List<Map<String, dynamic>>> all() async {
    final prefs = await SharedPreferences.getInstance();
    final text = prefs.getString(_key);
    if (text == null || text.isEmpty) return [];
    try {
      final data = jsonDecode(text);
      if (data is List) {
        return data.whereType<Map>().map((e) => e.cast<String, dynamic>()).toList();
      }
    } catch (_) {}
    return [];
  }

  Future<int> count() async => (await all()).length;

  Future<void> enqueue(String type, Map<String, dynamic> payload) async {
    final items = await all();
    items.add({
      'id': DateTime.now().microsecondsSinceEpoch.toString(),
      'type': type,
      'payload': payload,
      'createdAt': DateTime.now().toIso8601String(),
      'retry': 0,
    });
    await _save(items);
  }

  Future<void> _save(List<Map<String, dynamic>> items) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_key, jsonEncode(items));
  }

  Future<Map<String, int>> sync() async {
    final items = await all();
    var success = 0;
    var failed = 0;
    final remain = <Map<String, dynamic>>[];
    for (final item in items) {
      final type = item['type'];
      final payload = (item['payload'] as Map?)?.cast<String, dynamic>() ?? {};
      try {
        if (type == 'patrol') {
          await api.post('/patrol/checkin', body: payload);
        } else if (type == 'fire') {
          await api.post('/fire/inspect', body: payload);
        } else if (type == 'event') {
          await api.post('/event/report', body: payload);
        }
        success++;
      } catch (_) {
        failed++;
        final retry = (item['retry'] is int ? item['retry'] as int : 0) + 1;
        remain.add({...item, 'retry': retry});
      }
    }
    await _save(remain);
    return {'success': success, 'failed': failed, 'remaining': remain.length};
  }
}

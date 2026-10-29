import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/app_state.dart';
import '../core/i18n.dart';
import '../core/offline_queue.dart';
import '../core/theme.dart';
import '../widgets/common.dart';

class PatrolPage extends StatefulWidget {
  const PatrolPage({super.key});

  @override
  State<PatrolPage> createState() => _PatrolPageState();
}

class _PatrolPageState extends State<PatrolPage> {
  List<Map<String, dynamic>> _routes = [];
  List<Map<String, dynamic>> _points = [];
  List<Map<String, dynamic>> _records = [];
  Map<String, dynamic>? _route;
  Map<String, dynamic>? _point;
  final _abnormal = TextEditingController();
  bool _loading = true;
  bool _submitting = false;
  int _offlineCount = 0;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _init());
  }

  Future<void> _init() async {
    await Future.wait([_loadRoutes(), _loadRecords(), _countOffline()]);
  }

  @override
  void dispose() {
    _abnormal.dispose();
    super.dispose();
  }

  Future<void> _countOffline() async {
    final count = await OfflineQueue(context.read<AppState>().api).count();
    if (mounted) setState(() => _offlineCount = count);
  }

  Future<void> _loadRoutes() async {
    final api = context.read<AppState>().api;
    try {
      final data = await api.get('/patrol/routes');
      final routes = data is List
          ? data.whereType<Map>().map((e) => e.cast<String, dynamic>()).toList()
          : <Map<String, dynamic>>[];
      if (!mounted) return;
      setState(() {
        _routes = routes;
        _route = routes.isNotEmpty ? routes.first : null;
      });
      if (_route != null) await _loadPoints(_route!['id'] as int);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _loadPoints(int routeId) async {
    try {
      final data = await context.read<AppState>().api.get('/patrol/points', query: {'routeId': routeId});
      final points = data is List
          ? data.whereType<Map>().map((e) => e.cast<String, dynamic>()).toList()
          : <Map<String, dynamic>>[];
      if (!mounted) return;
      setState(() {
        _points = points;
        _point = points.isNotEmpty ? points.first : null;
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    }
  }

  Future<void> _loadRecords() async {
    try {
      final data = await context.read<AppState>().api.get('/patrol/records', query: {'page': 1, 'size': 20});
      final rows = data is Map && data['records'] is List
          ? (data['records'] as List).whereType<Map>().map((e) => e.cast<String, dynamic>()).toList()
          : <Map<String, dynamic>>[];
      if (mounted) setState(() => _records = rows);
    } catch (_) {}
  }

  Future<void> _checkin({String method = 'GPS', String? qrCode}) async {
    if (_point == null) {
      setState(() => _error = '请先选择巡逻点位');
      return;
    }
    setState(() {
      _submitting = true;
      _error = null;
    });
    double? lat;
    double? lon;
    if (method == 'GPS') {
      try {
        var permission = await Geolocator.checkPermission();
        if (permission == LocationPermission.denied) {
          permission = await Geolocator.requestPermission();
        }
        if (permission == LocationPermission.whileInUse || permission == LocationPermission.always) {
          final position = await Geolocator.getCurrentPosition(
            desiredAccuracy: LocationAccuracy.high,
            timeLimit: const Duration(seconds: 12),
          );
          lat = position.latitude;
          lon = position.longitude;
        }
      } catch (_) {
        // 定位失败仍允许打卡，由后端标记缺少坐标
      }
    }

    final payload = <String, dynamic>{
      'pointId': _point!['id'],
      'method': method,
      'longitude': lon,
      'latitude': lat,
      'abnormalDesc': _abnormal.text.trim().isEmpty ? null : _abnormal.text.trim(),
      'qrCode': qrCode,
    };
    try {
      await context.read<AppState>().api.post('/patrol/checkin', body: payload);
      if (!mounted) return;
      _abnormal.clear();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(I18n.t(context.read<AppState>().locale, 'success'))),
      );
      await _loadRecords();
    } on ApiException catch (e) {
      if (e.message.contains('网络') || e.statusCode == null) {
        await OfflineQueue(context.read<AppState>().api).enqueue('patrol', payload);
        await _countOffline();
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(I18n.t(context.read<AppState>().locale, 'networkOffline'))),
        );
      } else {
        setState(() => _error = e.message);
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _scan() async {
    final code = await Navigator.push<String>(
      context,
      MaterialPageRoute(builder: (_) => const _ScannerPage()),
    );
    if (code == null) return;
    Map<String, dynamic>? matched;
    for (final point in _points) {
      if ((point['qrCode'] ?? '').toString() == code || point['id'].toString() == code) {
        matched = point;
        break;
      }
    }
    if (matched != null) {
      setState(() => _point = matched);
      await _checkin(method: 'QR', qrCode: code);
    } else {
      setState(() => _error = '二维码不属于当前路线点位');
    }
  }

  Future<void> _sync() async {
    final result = await OfflineQueue(context.read<AppState>().api).sync();
    await _countOffline();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('补传 ${result['success']} 条，失败 ${result['failed']} 条')),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final t = (String key) => I18n.t(state.locale, key);
    return Scaffold(
      appBar: AppBar(
        title: Text(t('patrolCheckin')),
        actions: [
          IconButton(onPressed: _sync, icon: const Icon(Icons.cloud_upload_outlined)),
          IconButton(onPressed: _scan, icon: const Icon(Icons.qr_code_scanner)),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _init,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            if (_offlineCount > 0)
              Card(
                color: const Color(0xFFFFF4DD),
                child: ListTile(
                  leading: const Icon(Icons.cloud_off, color: AppTheme.warning),
                  title: Text('${t('offlineQueue')}: $_offlineCount'),
                  trailing: TextButton(onPressed: _sync, child: Text(t('syncNow'))),
                ),
              ),
            if (_loading) const LoadingView(),
            DropdownButtonFormField<Map<String, dynamic>>(
              value: _route,
              decoration: InputDecoration(labelText: t('selectRoute'), prefixIcon: const Icon(Icons.route_outlined)),
              items: _routes.map((route) {
                return DropdownMenuItem(
                  value: route,
                  child: Text((route['name'] ?? '').toString()),
                );
              }).toList(),
              onChanged: (value) {
                if (value == null) return;
                setState(() => _route = value);
                _loadPoints(value['id'] as int);
              },
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<Map<String, dynamic>>(
              value: _point,
              decoration: InputDecoration(labelText: t('selectPoint'), prefixIcon: const Icon(Icons.location_on_outlined)),
              items: _points.map((point) {
                return DropdownMenuItem(
                  value: point,
                  child: Text((point['name'] ?? '').toString()),
                );
              }).toList(),
              onChanged: (value) => setState(() => _point = value),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _abnormal,
              maxLines: 3,
              decoration: InputDecoration(
                labelText: t('abnormal'),
                hintText: t('abnormalHint'),
                alignLabelWithHint: true,
              ),
            ),
            if (_error != null) ...[
              const SizedBox(height: 10),
              Text(_error!, style: const TextStyle(color: AppTheme.danger, fontSize: 12)),
            ],
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: _submitting ? null : () => _checkin(),
              icon: const Icon(Icons.my_location),
              label: Text(_submitting ? t('loading') : t('gpsCheckin')),
            ),
            const SizedBox(height: 10),
            OutlinedButton.icon(
              onPressed: _submitting ? null : _scan,
              icon: const Icon(Icons.qr_code_scanner),
              label: Text(t('scanQr')),
            ),
            SectionTitle(t('eventList')),
            if (_records.isEmpty)
              EmptyState(text: t('empty'), icon: Icons.check_circle_outline)
            else
              ..._records.map((record) {
                return Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    leading: const Icon(Icons.check_circle_outline, color: AppTheme.success),
                    title: Text((record['guardName'] ?? state.realName).toString()),
                    subtitle: Text('${record['checkTime'] ?? ''} · ${record['method'] ?? ''}'),
                    trailing: StatusChip(status: (record['status'] ?? 'NORMAL').toString()),
                  ),
                );
              }),
          ],
        ),
      ),
    );
  }
}

class _ScannerPage extends StatefulWidget {
  const _ScannerPage();

  @override
  State<_ScannerPage> createState() => _ScannerPageState();
}

class _ScannerPageState extends State<_ScannerPage> {
  bool _handled = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('扫码打卡')),
      body: MobileScanner(
        onDetect: (capture) {
          if (_handled) return;
          final barcodes = capture.barcodes;
          if (barcodes.isEmpty) return;
          final code = barcodes.first.rawValue;
          if (code == null || code.isEmpty) return;
          _handled = true;
          Navigator.pop(context, code);
        },
      ),
    );
  }
}

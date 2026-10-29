import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/app_state.dart';
import '../core/i18n.dart';
import '../core/theme.dart';
import '../widgets/common.dart';

class ApprovalPage extends StatefulWidget {
  const ApprovalPage({super.key});

  @override
  State<ApprovalPage> createState() => _ApprovalPageState();
}

class _ApprovalPageState extends State<ApprovalPage> {
  List<Map<String, dynamic>> _visitors = [];
  List<Map<String, dynamic>> _vehicles = [];
  List<Map<String, dynamic>> _claims = [];
  List<Map<String, dynamic>> _reports = [];
  List<Map<String, dynamic>> _monitors = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<Map<String, dynamic>> _safe(Future<dynamic> future) async {
    try {
      final data = await future;
      if (data is Map) return data.cast<String, dynamic>();
    } catch (_) {}
    return {};
  }

  List<Map<String, dynamic>> _rows(Map<String, dynamic> data) {
    if (data['records'] is List) {
      return (data['records'] as List).whereType<Map>().map((e) => e.cast<String, dynamic>()).toList();
    }
    return [];
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    final api = context.read<AppState>().api;
    try {
      final results = await Future.wait([
        _safe(api.get('/visitor/appointments', query: {'page': 1, 'size': 20, 'status': 'PENDING'})),
        _safe(api.get('/vehicle/appointments', query: {'page': 1, 'size': 20, 'status': 'PENDING'})),
        _safe(api.get('/lost/claims', query: {'page': 1, 'size': 20, 'status': 'PENDING'})),
        _safe(api.get('/report/applies', query: {'page': 1, 'size': 20, 'status': 'PENDING'})),
        _safe(api.get('/report/monitor-access', query: {'page': 1, 'size': 20, 'status': 'PENDING'})),
      ]);
      if (!mounted) return;
      setState(() {
        _visitors = _rows(results[0]);
        _vehicles = _rows(results[1]);
        _claims = _rows(results[2]);
        _reports = _rows(results[3]);
        _monitors = _rows(results[4]);
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _approve(String kind, int id, bool approved) async {
    String path;
    switch (kind) {
      case 'visitor':
        path = '/visitor/appointments/$id/approve';
        break;
      case 'vehicle':
        path = '/vehicle/appointments/$id/approve';
        break;
      case 'claim':
        path = '/lost/claims/$id/approve';
        break;
      case 'report':
        path = '/report/applies/$id/approve';
        break;
      default:
        return;
    }
    try {
      await context.read<AppState>().api.post(path, body: {'approved': approved});
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(I18n.t(context.read<AppState>().locale, approved ? 'success' : 'failed'))),
      );
      await _load();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }
  }

  Widget _approvalCard({
    required String kind,
    required Map<String, dynamic> item,
    required String title,
    required String subtitle,
    String? extra,
    bool actionable = true,
  }) {
    final state = context.watch<AppState>();
    final t = (String key) => I18n.t(state.locale, key);
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 12, 10, 6),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(title, style: const TextStyle(fontWeight: FontWeight.bold, color: AppTheme.primary)),
                ),
                const StatusChip(status: 'PENDING'),
              ],
            ),
            const SizedBox(height: 4),
            Text(subtitle, style: const TextStyle(fontSize: 12, color: Color(0xFF64748B))),
            if (extra != null && extra.isNotEmpty)
              Text(extra, style: const TextStyle(fontSize: 11, color: Color(0xFF94A3B8))),
            if (actionable)
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton(
                    onPressed: state.isLeader ? null : () => _approve(kind, item['id'] as int, false),
                    child: Text(t('reject'), style: const TextStyle(color: AppTheme.danger)),
                  ),
                  FilledButton(
                    onPressed: state.isLeader ? null : () => _approve(kind, item['id'] as int, true),
                    style: FilledButton.styleFrom(minimumSize: const Size(70, 34)),
                    child: Text(t('approve')),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final t = (String key) => I18n.t(state.locale, key);
    final total = _visitors.length + _vehicles.length + _claims.length + _reports.length + _monitors.length;
    return Scaffold(
      appBar: AppBar(
        title: Text(t('approvals')),
        actions: [IconButton(onPressed: _load, icon: const Icon(Icons.refresh))],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Row(
              children: [
                StatCard(label: '待办总数', value: '$total', color: AppTheme.warning),
                const SizedBox(width: 10),
                StatCard(label: '访客 / 车辆', value: '${_visitors.length}/${_vehicles.length}'),
              ],
            ),
            if (_loading) const LoadingView(),
            if (_error != null)
              Text(_error!, style: const TextStyle(color: AppTheme.danger, fontSize: 12)),
            if (!_loading && total == 0) EmptyState(text: t('empty'), icon: Icons.task_alt),
            if (_visitors.isNotEmpty) ...[
              SectionTitle('${t('visitorApproval')}（${_visitors.length}）'),
              ..._visitors.map((item) => _approvalCard(
                    kind: 'visitor',
                    item: item,
                    title: '${item['visitorName'] ?? ''} 来访',
                    subtitle: '${item['visitTime'] ?? ''} · ${item['reason'] ?? ''}',
                    extra: '电话 ${item['phoneMask'] ?? '-'}${item['plateNo'] == null ? '' : ' · 车牌 ${item['plateNo']}'}',
                  )),
            ],
            if (_vehicles.isNotEmpty) ...[
              SectionTitle('${t('vehicleApproval')}（${_vehicles.length}）'),
              ..._vehicles.map((item) => _approvalCard(
                    kind: 'vehicle',
                    item: item,
                    title: '车辆 ${item['plateNo'] ?? ''}',
                    subtitle: '${item['visitTime'] ?? ''} · ${item['visitorName'] ?? ''}',
                    extra: '电话 ${item['phoneMask'] ?? '-'}',
                  )),
            ],
            if (_claims.isNotEmpty) ...[
              SectionTitle('${t('lostApproval')}（${_claims.length}）'),
              ..._claims.map((item) => _approvalCard(
                    kind: 'claim',
                    item: item,
                    title: '${item['claimant'] ?? ''} 申请认领',
                    subtitle: '物品 #${item['itemId']} · ${item['claimTime'] ?? ''}',
                    extra: '电话 ${item['phoneMask'] ?? '-'}',
                  )),
            ],
            if (_reports.isNotEmpty) ...[
              SectionTitle('${t('reportApproval')}（${_reports.length}）'),
              ..._reports.map((item) => _approvalCard(
                    kind: 'report',
                    item: item,
                    title: (item['title'] ?? '报备').toString(),
                    subtitle: '${item['type'] ?? ''} · ${item['startTime'] ?? ''}',
                    extra: (item['location'] ?? '').toString(),
                  )),
            ],
            if (_monitors.isNotEmpty) ...[
              SectionTitle('${t('monitorApproval')}（${_monitors.length}）'),
              ..._monitors.map((item) => _approvalCard(
                    kind: 'monitor',
                    item: item,
                    title: (item['applyNo'] ?? '调阅申请').toString(),
                    subtitle: '${item['area'] ?? ''} · ${item['timeRange'] ?? ''}',
                    extra: '需管理端两级审批',
                    actionable: false,
                  )),
            ],
          ],
        ),
      ),
    );
  }
}

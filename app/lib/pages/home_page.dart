import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/app_state.dart';
import '../core/i18n.dart';
import '../core/offline_queue.dart';
import '../core/theme.dart';
import '../widgets/common.dart';
import 'event_page.dart';
import 'fire_page.dart';
import 'hazard_page.dart';
import 'message_page.dart';
import 'patrol_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  Map<String, dynamic> _stats = {};
  List<Map<String, dynamic>> _warnings = [];
  int _offlineCount = 0;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _load();
      _loadOffline();
    });
  }

  Future<void> _loadOffline() async {
    final count = await OfflineQueue(context.read<AppState>().api).count();
    if (mounted) setState(() => _offlineCount = count);
  }

  Future<void> _load() async {
    final state = context.read<AppState>();
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        state.api.get('/dashboard'),
        state.api.get('/public/tenant/${state.tenantCode}/warnings'),
      ]);
      if (!mounted) return;
      setState(() {
        _stats = (results[0] as Map?)?.cast<String, dynamic>() ?? {};
        final warnings = results[1];
        _warnings = warnings is List
            ? warnings.whereType<Map>().map((e) => e.cast<String, dynamic>()).toList()
            : [];
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _openMenu(String path) {
    switch (path) {
      case '/patrol':
        Navigator.push(context, MaterialPageRoute(builder: (_) => const PatrolPage()));
        break;
      case '/messages':
        Navigator.push(context, MaterialPageRoute(builder: (_) => const MessagePage()));
        break;
      case '/events':
        Navigator.push(context, MaterialPageRoute(builder: (_) => const EventPage()));
        break;
      default:
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final t = (String key) => I18n.t(state.locale, key);
    final menus = state.menus.isEmpty
        ? <Map<String, dynamic>>[
            {'name': '巡逻', 'nameEn': 'Patrol', 'path': '/patrol', 'icon': 'qr'},
            {'name': '事件', 'nameEn': 'Events', 'path': '/events', 'icon': 'alert'},
            {'name': '消息', 'nameEn': 'Messages', 'path': '/messages', 'icon': 'message'},
          ]
        : state.menus;

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('${state.tenant?['name'] ?? t('appName')}', style: const TextStyle(fontSize: 17)),
            Text(t('dashboard'), style: const TextStyle(fontSize: 11, color: Color(0xFFC7D7EE))),
          ],
        ),
        actions: [
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 30),
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${t('home')}，${state.realName}', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: AppTheme.primary)),
                      const SizedBox(height: 2),
                      Text(
                        state.isLeader ? t('readOnly') : '${state.user?['roleCode'] ?? ''} · ${state.user?['dept'] ?? ''}',
                        style: const TextStyle(color: Color(0xFF7B8CA6), fontSize: 12),
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.shield_outlined, size: 42, color: Color(0xFFDBE5F2)),
              ],
            ),
            if (_error != null)
              Card(
                color: const Color(0xFFFFF1F2),
                child: ListTile(
                  leading: const Icon(Icons.wifi_off, color: AppTheme.danger),
                  title: Text(_error!, style: const TextStyle(fontSize: 12, color: AppTheme.danger)),
                  trailing: TextButton(onPressed: _load, child: Text(t('retry'))),
                ),
              ),
            const SizedBox(height: 14),
            if (_loading) const LinearProgressIndicator(minHeight: 2),
            Row(
              children: [
                StatCard(label: t('todayVisitors'), value: '${_stats['visitorToday'] ?? 0}'),
                const SizedBox(width: 10),
                StatCard(label: t('pendingVisitors'), value: '${_stats['visitorPending'] ?? 0}', color: AppTheme.warning),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                StatCard(label: t('openHazards'), value: '${_stats['hazardOpen'] ?? 0}', color: AppTheme.danger),
                const SizedBox(width: 10),
                StatCard(label: t('patrolToday'), value: '${_stats['patrolRecordToday'] ?? 0}', color: AppTheme.success),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                StatCard(label: t('fireAlarms'), value: '${_stats['fireAlarmToday'] ?? 0}', color: AppTheme.danger),
                const SizedBox(width: 10),
                StatCard(label: t('eventsToday'), value: '${_stats['eventOpen'] ?? 0}', color: AppTheme.warning),
              ],
            ),
            SectionTitle(t('quickActions')),
            GridView.count(
              crossAxisCount: 4,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: 10,
              crossAxisSpacing: 10,
              childAspectRatio: 0.9,
              children: [
                _QuickTile(icon: Icons.fact_check_outlined, label: t('patrolCheckin'), onTap: () {
                  Navigator.push(context, MaterialPageRoute(builder: (_) => const PatrolPage()));
                }),
                _QuickTile(icon: Icons.local_fire_department_outlined, label: t('fireInspection'), onTap: () {
                  Navigator.push(context, MaterialPageRoute(builder: (_) => const FirePage()));
                }),
                _QuickTile(icon: Icons.warning_amber_outlined, label: t('hazardReport'), onTap: () {
                  Navigator.push(context, MaterialPageRoute(builder: (_) => const HazardPage()));
                }),
                _QuickTile(icon: Icons.notifications_active_outlined, label: t('events'), onTap: () {
                  Navigator.push(context, MaterialPageRoute(builder: (_) => const EventPage()));
                }),
                ...menus.take(4).map((menu) {
                  final name = state.locale == 'zh-CN'
                      ? (menu['name'] ?? '')
                      : (menu['nameEn'] ?? menu['name'] ?? '');
                  return _QuickTile(
                    icon: Icons.grid_view_outlined,
                    label: name.toString(),
                    onTap: () => _openMenu((menu['path'] ?? '').toString()),
                  );
                }),
              ],
            ),
            SectionTitle(t('warnings')),
            if (_warnings.isEmpty)
              const EmptyState(text: '暂无预警', icon: Icons.verified_outlined)
            else
              Card(
                color: const Color(0xFFFFFDF8),
                child: Column(
                  children: _warnings.take(5).map((item) {
                    return ListTile(
                      dense: true,
                      leading: const Icon(Icons.warning_amber_rounded, color: AppTheme.warning),
                      title: Text((item['title'] ?? '').toString(), style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                      subtitle: Text((item['content'] ?? '').toString(), maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11)),
                    );
                  }).toList(),
                ),
              ),
            SectionTitle(
              '${t('offlineQueue')}（$_offlineCount）',
              trailing: TextButton(
                onPressed: () async {
                  final queue = OfflineQueue(state.api);
                  final result = await queue.sync();
                  await _loadOffline();
                  if (!mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(
                        (result['success'] ?? 0) > 0 ? t('syncSuccess') : t('syncFailed'),
                      ),
                    ),
                  );
                },
                child: Text(t('syncNow')),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _QuickTile extends StatelessWidget {
  const _QuickTile({required this.icon, required this.label, required this.onTap});

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: const Color(0xFFE6EDF6)),
        ),
        padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 4),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 38,
              height: 38,
              decoration: BoxDecoration(color: const Color(0xFFEEF4FF), borderRadius: BorderRadius.circular(11)),
              child: Icon(icon, color: AppTheme.accent, size: 20),
            ),
            const SizedBox(height: 7),
            Text(label, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 10, color: Color(0xFF334155))),
          ],
        ),
      ),
    );
  }
}

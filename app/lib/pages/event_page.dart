import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/app_state.dart';
import '../core/i18n.dart';
import '../core/theme.dart';
import '../widgets/common.dart';

class EventPage extends StatefulWidget {
  const EventPage({super.key});

  @override
  State<EventPage> createState() => _EventPageState();
}

class _EventPageState extends State<EventPage> {
  List<Map<String, dynamic>> _events = [];
  bool _loading = true;
  bool _submitting = false;
  bool _formVisible = false;
  String _type = 'SECURITY';
  String _urgency = 'NORMAL';
  final _location = TextEditingController();
  final _description = TextEditingController();
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  @override
  void dispose() {
    _location.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final data = await context.read<AppState>().api.get('/event/events', query: {'page': 1, 'size': 30});
      final rows = data is Map && data['records'] is List
          ? (data['records'] as List).whereType<Map>().map((e) => e.cast<String, dynamic>()).toList()
          : <Map<String, dynamic>>[];
      if (mounted) setState(() => _events = rows);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _report({bool alarm = false}) async {
    if (!alarm && (_location.text.trim().isEmpty || _description.text.trim().isEmpty)) {
      setState(() => _error = '请填写位置和事件描述');
      return;
    }
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await context.read<AppState>().api.post(
        alarm ? '/event/alarm' : '/event/report',
        body: {
          'eventType': alarm ? 'EMERGENCY' : _type,
          'urgency': alarm ? 'CRITICAL' : _urgency,
          'location': alarm ? 'App 一键报警' : _location.text.trim(),
          'description': alarm ? '保安一线端一键报警，请立即处置' : _description.text.trim(),
          'source': 'APP',
          'reporter': context.read<AppState>().realName,
        },
      );
      if (!mounted) return;
      _location.clear();
      _description.clear();
      setState(() => _formVisible = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(I18n.t(context.read<AppState>().locale, 'success'))),
      );
      await _load();
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _handle(Map<String, dynamic> event) async {
    final controller = TextEditingController();
    final result = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('处置上报'),
        content: TextField(
          controller: controller,
          maxLines: 3,
          decoration: const InputDecoration(hintText: '填写处置结果'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
          FilledButton(onPressed: () => Navigator.pop(context, controller.text.trim()), child: const Text('提交')),
        ],
      ),
    );
    controller.dispose();
    if (result == null || result.isEmpty) return;
    try {
      await context.read<AppState>().api.post('/event/${event['id']}/handle', body: {'handleResult': result});
      await _load();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final t = (String key) => I18n.t(state.locale, key);
    return Scaffold(
      appBar: AppBar(
        title: Text(t('eventList')),
        actions: [
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
          IconButton(
            tooltip: '一键报警',
            onPressed: _submitting
                ? null
                : () async {
                    final ok = await showDialog<bool>(
                      context: context,
                      builder: (context) => AlertDialog(
                        title: const Text('一键报警'),
                        content: const Text('确认立即向保卫处指挥中心发送紧急报警？'),
                        actions: [
                          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
                          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('确认报警')),
                        ],
                      ),
                    );
                    if (ok == true) _report(alarm: true);
                  },
            icon: const Icon(Icons.sos, color: Colors.redAccent),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => setState(() => _formVisible = !_formVisible),
        icon: Icon(_formVisible ? Icons.close : Icons.add_alert_outlined),
        label: Text(t('reportEvent')),
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 90),
          children: [
            if (_formVisible)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    children: [
                      DropdownButtonFormField<String>(
                        value: _type,
                        decoration: InputDecoration(labelText: t('eventType')),
                        items: const [
                          DropdownMenuItem(value: 'THEFT', child: Text('失窃')),
                          DropdownMenuItem(value: 'DISPUTE', child: Text('纠纷')),
                          DropdownMenuItem(value: 'HELP', child: Text('求助')),
                          DropdownMenuItem(value: 'SECURITY', child: Text('治安')),
                          DropdownMenuItem(value: 'EMERGENCY', child: Text('应急')),
                        ],
                        onChanged: (value) => setState(() => _type = value ?? 'SECURITY'),
                      ),
                      const SizedBox(height: 10),
                      DropdownButtonFormField<String>(
                        value: _urgency,
                        decoration: InputDecoration(labelText: '紧急程度'),
                        items: const [
                          DropdownMenuItem(value: 'NORMAL', child: Text('普通')),
                          DropdownMenuItem(value: 'URGENT', child: Text('紧急')),
                          DropdownMenuItem(value: 'CRITICAL', child: Text('特急')),
                        ],
                        onChanged: (value) => setState(() => _urgency = value ?? 'NORMAL'),
                      ),
                      const SizedBox(height: 10),
                      TextField(
                        controller: _location,
                        decoration: InputDecoration(labelText: t('location')),
                      ),
                      const SizedBox(height: 10),
                      TextField(
                        controller: _description,
                        maxLines: 3,
                        decoration: InputDecoration(labelText: t('description'), alignLabelWithHint: true),
                      ),
                      if (_error != null) ...[
                        const SizedBox(height: 8),
                        Text(_error!, style: const TextStyle(color: AppTheme.danger, fontSize: 12)),
                      ],
                      const SizedBox(height: 14),
                      FilledButton(
                        onPressed: _submitting ? null : () => _report(),
                        child: Text(_submitting ? t('loading') : t('submit')),
                      ),
                    ],
                  ),
                ),
              ),
            if (_loading) const LoadingView(),
            if (!_loading && _events.isEmpty) EmptyState(text: t('empty')),
            ..._events.map((event) {
              final status = (event['status'] ?? 'PENDING').toString();
              return Card(
                margin: const EdgeInsets.only(bottom: 10),
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              (event['eventType'] ?? event['eventNo'] ?? '事件').toString(),
                              style: const TextStyle(fontWeight: FontWeight.bold, color: AppTheme.primary),
                            ),
                          ),
                          StatusChip(status: status),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Text((event['description'] ?? '').toString(), maxLines: 2, overflow: TextOverflow.ellipsis),
                      const SizedBox(height: 6),
                      Text(
                        '${event['location'] ?? '-'} · ${event['createTime'] ?? ''}',
                        style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 11),
                      ),
                      if (status != 'CLOSED')
                        Align(
                          alignment: Alignment.centerRight,
                          child: TextButton(
                            onPressed: state.isLeader ? null : () => _handle(event),
                            child: Text(t('handle')),
                          ),
                        ),
                    ],
                  ),
                ),
              );
            }),
          ],
        ),
      ),
    );
  }
}

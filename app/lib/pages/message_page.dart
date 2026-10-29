import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/app_state.dart';
import '../core/i18n.dart';
import '../core/theme.dart';
import '../widgets/common.dart';

class MessagePage extends StatefulWidget {
  const MessagePage({super.key});

  @override
  State<MessagePage> createState() => _MessagePageState();
}

class _MessagePageState extends State<MessagePage> {
  List<Map<String, dynamic>> _messages = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final data = await context.read<AppState>().api.get('/system/messages', query: {'page': 1, 'size': 50});
      final rows = data is Map && data['records'] is List
          ? (data['records'] as List).whereType<Map>().map((e) => e.cast<String, dynamic>()).toList()
          : <Map<String, dynamic>>[];
      if (mounted) setState(() => _messages = rows);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _open(Map<String, dynamic> message) async {
    if (message['readFlag'] != 1) {
      try {
        await context.read<AppState>().api.post('/system/messages/${message['id']}/read');
        await _load();
      } catch (_) {}
    }
    if (!mounted) return;
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text((message['title'] ?? '').toString()),
        content: SingleChildScrollView(
          child: Text((message['content'] ?? '').toString()),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('关闭')),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final t = (String key) => I18n.t(state.locale, key);
    final unread = _messages.where((e) => e['readFlag'] != 1).length;
    return Scaffold(
      appBar: AppBar(
        title: Text(t('messageCenter')),
        actions: [IconButton(onPressed: _load, icon: const Icon(Icons.refresh))],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Row(
              children: [
                StatCard(label: '消息总数', value: '${_messages.length}'),
                const SizedBox(width: 10),
                StatCard(label: t('unread'), value: '$unread', color: AppTheme.warning),
              ],
            ),
            if (_loading) const LoadingView(),
            if (_error != null) Text(_error!, style: const TextStyle(color: AppTheme.danger, fontSize: 12)),
            if (!_loading && _messages.isEmpty) EmptyState(text: t('empty'), icon: Icons.notifications_none),
            ..._messages.map((message) {
              final read = message['readFlag'] == 1;
              return Card(
                margin: const EdgeInsets.only(bottom: 8),
                child: ListTile(
                  onTap: () => _open(message),
                  leading: Icon(
                    read ? Icons.mark_email_read_outlined : Icons.mark_email_unread_outlined,
                    color: read ? const Color(0xFF94A3B8) : AppTheme.accent,
                  ),
                  title: Text(
                    (message['title'] ?? '').toString(),
                    style: TextStyle(fontWeight: read ? FontWeight.normal : FontWeight.bold, fontSize: 14),
                  ),
                  subtitle: Text(
                    (message['content'] ?? '').toString(),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 11),
                  ),
                  trailing: Text(
                    (message['createTime'] ?? '').toString(),
                    style: const TextStyle(fontSize: 10, color: Color(0xFF94A3B8)),
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

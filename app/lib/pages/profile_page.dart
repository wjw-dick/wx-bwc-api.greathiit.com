import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/config.dart';
import '../core/app_state.dart';
import '../core/i18n.dart';
import '../core/offline_queue.dart';
import '../core/theme.dart';
import 'message_page.dart';
import 'password_page.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  int _offlineCount = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadOffline());
  }

  Future<void> _loadOffline() async {
    final count = await OfflineQueue(context.read<AppState>().api).count();
    if (mounted) setState(() => _offlineCount = count);
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final t = (String key) => I18n.t(state.locale, key);
    final user = state.user ?? {};
    return Scaffold(
      appBar: AppBar(title: Text(t('profile'))),
      body: ListView(
        children: [
          Container(
            color: AppTheme.primary,
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 30,
                  backgroundColor: Colors.white.withOpacity(0.16),
                  child: Text(
                    state.realName.isEmpty ? '安' : state.realName.substring(0, 1),
                    style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(state.realName, style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 2),
                      Text(
                        '${user['roleCode'] ?? ''} · ${user['dept'] ?? ''}',
                        style: const TextStyle(color: Color(0xFFC7D7EE), fontSize: 12),
                      ),
                      Text(
                        '${state.tenant?['name'] ?? ''}（${state.tenantCode}）',
                        style: const TextStyle(color: Color(0xFFC7D7EE), fontSize: 11),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          Card(
            margin: const EdgeInsets.symmetric(horizontal: 16),
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.person_outline),
                  title: Text(user['username']?.toString() ?? '-'),
                  subtitle: const Text('账号'),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.badge_outlined),
                  title: Text(user['roleCode']?.toString() ?? '-'),
                  subtitle: Text(state.isLeader ? '${t('role')} · ${t('readOnly')}' : t('role')),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.message_outlined),
                  title: Text(t('messageCenter')),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const MessagePage())),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          Card(
            margin: const EdgeInsets.symmetric(horizontal: 16),
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.language),
                  title: Text(t('language')),
                  trailing: TextButton(
                    onPressed: () => state.setLocale(state.locale == 'zh-CN' ? 'en-US' : 'zh-CN'),
                    child: Text(I18n.name(state.locale)),
                  ),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.cloud_off_outlined),
                  title: Text(t('offlineQueue')),
                  trailing: Text('$_offlineCount 条'),
                  onTap: () async {
                    final queue = OfflineQueue(state.api);
                    final result = await queue.sync();
                    await _loadOffline();
                    if (!mounted) return;
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('补传 ${result['success']} 条，剩余 ${result['remaining']} 条')),
                    );
                  },
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.lock_outline),
                  title: Text(t('changePassword')),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const PasswordPage())),
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.info_outline),
                  title: Text(t('about')),
                  subtitle: Text('${AppConfig.version} · ${AppConfig.legalDomain}'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: OutlinedButton.icon(
              onPressed: () async {
                final ok = await showDialog<bool>(
                  context: context,
                  builder: (context) => AlertDialog(
                    title: Text(t('logout')),
                    content: const Text('确认退出当前账号？'),
                    actions: [
                      TextButton(onPressed: () => Navigator.pop(context, false), child: Text(t('cancel'))),
                      FilledButton(onPressed: () => Navigator.pop(context, true), child: Text(t('confirm'))),
                    ],
                  ),
                );
                if (ok == true) await state.logout();
              },
              icon: const Icon(Icons.logout, color: AppTheme.danger),
              label: Text(t('logout'), style: const TextStyle(color: AppTheme.danger)),
            ),
          ),
          const SizedBox(height: 26),
        ],
      ),
    );
  }
}

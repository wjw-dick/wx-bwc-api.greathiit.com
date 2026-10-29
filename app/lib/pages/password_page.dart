import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/app_state.dart';
import '../core/i18n.dart';
import '../core/theme.dart';

class PasswordPage extends StatefulWidget {
  const PasswordPage({super.key});

  @override
  State<PasswordPage> createState() => _PasswordPageState();
}

class _PasswordPageState extends State<PasswordPage> {
  final _old = TextEditingController();
  final _new = TextEditingController();
  final _confirm = TextEditingController();
  bool _submitting = false;
  String? _error;

  @override
  void dispose() {
    _old.dispose();
    _new.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final state = context.read<AppState>();
    final t = (String key) => I18n.t(state.locale, key);
    if (_old.text.isEmpty) {
      setState(() => _error = '请输入原密码');
      return;
    }
    if (_new.text.length < 8 || _new.text.length > 32) {
      setState(() => _error = '新密码长度需为 8-32 位');
      return;
    }
    if (!RegExp(r'[A-Za-z]').hasMatch(_new.text) || !RegExp(r'\d').hasMatch(_new.text)) {
      setState(() => _error = '新密码需同时包含字母和数字');
      return;
    }
    if (_new.text != _confirm.text) {
      setState(() => _error = '两次输入的新密码不一致');
      return;
    }
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await state.api.post('/auth/change-password', body: {
        'oldPassword': _old.text,
        'newPassword': _new.text,
      });
      if (!mounted) return;
      await showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: Text(t('success')),
          content: const Text('密码已修改，请重新登录。'),
          actions: [
            FilledButton(
              onPressed: () => Navigator.pop(context),
              child: Text(t('confirm')),
            ),
          ],
        ),
      );
      await state.logout();
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final t = (String key) => I18n.t(state.locale, key);
    return Scaffold(
      appBar: AppBar(title: Text(t('changePassword'))),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  TextField(
                    controller: _old,
                    obscureText: true,
                    decoration: InputDecoration(labelText: t('oldPassword'), prefixIcon: const Icon(Icons.lock_outline)),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _new,
                    obscureText: true,
                    decoration: InputDecoration(labelText: t('newPassword'), prefixIcon: const Icon(Icons.lock_reset)),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _confirm,
                    obscureText: true,
                    decoration: InputDecoration(labelText: t('confirmPassword'), prefixIcon: const Icon(Icons.lock_reset)),
                  ),
                  if (_error != null) ...[
                    const SizedBox(height: 10),
                    Text(_error!, style: const TextStyle(color: AppTheme.danger, fontSize: 12)),
                  ],
                  const SizedBox(height: 18),
                  FilledButton(
                    onPressed: _submitting ? null : _submit,
                    child: Text(_submitting ? t('loading') : t('submit')),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

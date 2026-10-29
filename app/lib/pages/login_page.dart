import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/app_state.dart';
import '../core/config.dart';
import '../core/i18n.dart';
import '../core/theme.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _username = TextEditingController();
  final _password = TextEditingController();
  final _tenant = TextEditingController();
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tenant.text = context.read<AppState>().tenantCode;
  }

  @override
  void dispose() {
    _username.dispose();
    _password.dispose();
    _tenant.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await context.read<AppState>().login(
            username: _username.text.trim(),
            password: _password.text,
            code: _tenant.text.trim().toUpperCase(),
          );
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final t = (String key) => I18n.t(state.locale, key);
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF0B2547), Color(0xFF123A6D), Color(0xFFF6F9FE)],
            stops: [0, 0.45, 0.45],
          ),
        ),
        child: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 48, 24, 32),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      width: 56,
                      height: 56,
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.14),
                        borderRadius: BorderRadius.circular(18),
                        border: Border.all(color: Colors.white24),
                      ),
                      alignment: Alignment.center,
                      child: const Text('安', style: TextStyle(color: Colors.white, fontSize: 26, fontWeight: FontWeight.bold)),
                    ),
                    const SizedBox(width: 14),
                    const Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('校园保卫', style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold)),
                          Text('Campus Security', style: TextStyle(color: Color(0xFFC7D7EE), fontSize: 12)),
                        ],
                      ),
                    ),
                    TextButton(
                      onPressed: () => state.setLocale(state.locale == 'zh-CN' ? 'en-US' : 'zh-CN'),
                      child: Text(
                        state.locale == 'zh-CN' ? 'EN' : '中文',
                        style: const TextStyle(color: Colors.white),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 42),
                Text(t('login'), style: const TextStyle(fontSize: 34, fontWeight: FontWeight.bold, color: Color(0xFF0F2A52))),
                const SizedBox(height: 6),
                Text(t('loginHint'), style: const TextStyle(color: Color(0xFF7B8CA6), fontSize: 13)),
                const SizedBox(height: 22),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        children: [
                          TextFormField(
                            controller: _username,
                            textInputAction: TextInputAction.next,
                            decoration: InputDecoration(labelText: t('username'), prefixIcon: const Icon(Icons.person_outline)),
                            validator: (v) => (v == null || v.trim().isEmpty) ? t('required') : null,
                          ),
                          const SizedBox(height: 14),
                          TextFormField(
                            controller: _password,
                            obscureText: true,
                            onFieldSubmitted: (_) => _submit(),
                            decoration: InputDecoration(labelText: t('password'), prefixIcon: const Icon(Icons.lock_outline)),
                            validator: (v) => (v == null || v.isEmpty) ? t('required') : null,
                          ),
                          const SizedBox(height: 14),
                          TextFormField(
                            controller: _tenant,
                            textCapitalization: TextCapitalization.characters,
                            decoration: InputDecoration(labelText: t('tenantCode'), prefixIcon: const Icon(Icons.school_outlined)),
                            validator: (v) => (v == null || v.trim().isEmpty) ? t('required') : null,
                          ),
                          if (_error != null) ...[
                            const SizedBox(height: 14),
                            Container(
                              width: double.infinity,
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: const Color(0xFFFFF1F2),
                                borderRadius: BorderRadius.circular(10),
                              ),
                              child: Text(_error!, style: const TextStyle(color: AppTheme.danger, fontSize: 13)),
                            ),
                          ],
                          const SizedBox(height: 20),
                          FilledButton(
                            onPressed: _loading ? null : _submit,
                            child: _loading
                                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                                : Text(t('login')),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 18),
                Center(
                  child: Text(
                    '合法域名 ${AppConfig.legalDomain}',
                    style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 11),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

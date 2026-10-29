import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';

import 'core/app_state.dart';
import 'core/theme.dart';
import 'pages/login_page.dart';
import 'pages/shell_page.dart';

class BwcApp extends StatelessWidget {
  const BwcApp({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    return MaterialApp(
      title: '校园保卫',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      locale: Locale(state.locale),
      supportedLocales: const [Locale('zh', 'CN'), Locale('en', 'US')],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: !state.initialized
          ? const _SplashPage()
          : state.loggedIn
              ? const ShellPage()
              : const LoginPage(),
    );
  }
}

class _SplashPage extends StatelessWidget {
  const _SplashPage();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('校园保卫', style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold)),
            SizedBox(height: 18),
            CircularProgressIndicator(),
          ],
        ),
      ),
    );
  }
}

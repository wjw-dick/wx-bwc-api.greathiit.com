import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/app_state.dart';
import '../core/i18n.dart';
import 'approval_page.dart';
import 'event_page.dart';
import 'home_page.dart';
import 'patrol_page.dart';
import 'profile_page.dart';

class ShellPage extends StatefulWidget {
  const ShellPage({super.key});

  @override
  State<ShellPage> createState() => _ShellPageState();
}

class _ShellPageState extends State<ShellPage> {
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final t = (String key) => I18n.t(state.locale, key);
    final pages = const [
      HomePage(),
      PatrolPage(),
      EventPage(),
      ApprovalPage(),
      ProfilePage(),
    ];
    return Scaffold(
      body: IndexedStack(index: _index, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (value) => setState(() => _index = value),
        destinations: [
          NavigationDestination(icon: const Icon(Icons.home_outlined), selectedIcon: const Icon(Icons.home), label: t('home')),
          NavigationDestination(icon: const Icon(Icons.route_outlined), selectedIcon: const Icon(Icons.route), label: t('patrol')),
          NavigationDestination(icon: const Icon(Icons.notifications_active_outlined), selectedIcon: const Icon(Icons.notifications_active), label: t('events')),
          NavigationDestination(icon: const Icon(Icons.fact_check_outlined), selectedIcon: const Icon(Icons.fact_check), label: t('approvals')),
          NavigationDestination(icon: const Icon(Icons.person_outline), selectedIcon: const Icon(Icons.person), label: t('profile')),
        ],
      ),
    );
  }
}

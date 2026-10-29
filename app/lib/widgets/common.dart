import 'package:flutter/material.dart';

import '../core/theme.dart';

class SectionTitle extends StatelessWidget {
  const SectionTitle(this.title, {super.key, this.trailing});

  final String title;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 22, bottom: 12),
      child: Row(
        children: [
          Expanded(
            child: Text(title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: AppTheme.primary)),
          ),
          if (trailing != null) trailing!,
        ],
      ),
    );
  }
}

class StatCard extends StatelessWidget {
  const StatCard({super.key, required this.label, required this.value, this.color = AppTheme.accent});

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: const TextStyle(color: Color(0xFF7B8CA6), fontSize: 12)),
              const SizedBox(height: 6),
              Text(value, style: TextStyle(color: color, fontSize: 26, fontWeight: FontWeight.bold)),
            ],
          ),
        ),
      ),
    );
  }
}

class EmptyState extends StatelessWidget {
  const EmptyState({super.key, required this.text, this.icon = Icons.inbox_outlined});

  final String text;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 38),
      child: Center(
        child: Column(
          children: [
            Icon(icon, size: 38, color: const Color(0xFFCBD5E1)),
            const SizedBox(height: 8),
            Text(text, style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 13)),
          ],
        ),
      ),
    );
  }
}

class StatusChip extends StatelessWidget {
  const StatusChip({super.key, required this.status});

  final String status;

  static const Map<String, String> _labels = {
    'PENDING': '待审批',
    'APPROVED': '已通过',
    'REJECTED': '已驳回',
    'NORMAL': '正常',
    'HANDLING': '处理中',
    'DONE': '已完成',
    'CLOSED': '已结案',
    'FOUND': '待认领',
    'CLAIMED': '已认领',
    'PUBLISHED': '已发布',
  };

  Color get _color {
    if (['APPROVED', 'NORMAL', 'DONE', 'CLOSED', 'CLAIMED', 'PUBLISHED'].contains(status)) {
      return AppTheme.success;
    }
    if (['PENDING', 'HANDLING'].contains(status)) return AppTheme.warning;
    if (['REJECTED', 'EXPIRED', 'DISABLED'].contains(status)) return AppTheme.danger;
    return const Color(0xFF64748B);
  }

  @override
  Widget build(BuildContext context) {
    final color = _color;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(_labels[status] ?? status, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w600)),
    );
  }
}

class LoadingView extends StatelessWidget {
  const LoadingView({super.key});

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 36),
      child: Center(child: CircularProgressIndicator()),
    );
  }
}

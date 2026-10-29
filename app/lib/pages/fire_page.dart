import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/app_state.dart';
import '../core/i18n.dart';
import '../core/offline_queue.dart';
import '../core/theme.dart';
import '../widgets/common.dart';
import '../widgets/qr_scanner.dart';

class FirePage extends StatefulWidget {
  const FirePage({super.key});

  @override
  State<FirePage> createState() => _FirePageState();
}

class _FirePageState extends State<FirePage> {
  List<Map<String, dynamic>> _facilities = [];
  Map<String, dynamic>? _facility;
  String _result = 'NORMAL';
  final _remark = TextEditingController();
  File? _photo;
  bool _loading = true;
  bool _submitting = false;
  String? _error;
  String? _qrCode;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  @override
  void dispose() {
    _remark.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final data = await context.read<AppState>().api.get('/fire/facilities', query: {'page': 1, 'size': 100});
      final rows = data is Map && data['records'] is List
          ? (data['records'] as List).whereType<Map>().map((e) => e.cast<String, dynamic>()).toList()
          : <Map<String, dynamic>>[];
      if (!mounted) return;
      setState(() {
        _facilities = rows;
        _facility = rows.isNotEmpty ? rows.first : null;
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _scan() async {
    final code = await Navigator.push<String>(
      context,
      MaterialPageRoute(builder: (_) => const QrScannerPage()),
    );
    if (code == null) return;
    Map<String, dynamic>? matched;
    for (final facility in _facilities) {
      if ((facility['facilityNo'] ?? '').toString() == code ||
          (facility['qrCode'] ?? '').toString() == code ||
          facility['id'].toString() == code) {
        matched = facility;
        break;
      }
    }
    setState(() {
      _qrCode = code;
      if (matched != null) _facility = matched;
    });
    if (matched == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('设施不在本台账，可直接提交二维码由后台匹配')),
      );
    }
  }

  Future<void> _pickPhoto() async {
    final picked = await ImagePicker().pickImage(source: ImageSource.camera, imageQuality: 70);
    if (picked != null) setState(() => _photo = File(picked.path));
  }

  Future<void> _submit() async {
    setState(() {
      _submitting = true;
      _error = null;
    });
    String? photos;
    if (_photo != null) {
      try {
        final uploaded = await context.read<AppState>().api.upload(_photo!.path, bizType: 'fire-inspection');
        photos = (uploaded['url'] ?? uploaded['path'] ?? '').toString();
      } catch (_) {
        // 图片上传失败不阻断巡检提交
      }
    }
    final payload = <String, dynamic>{
      'facilityId': _facility?['id'],
      'qrCode': _qrCode,
      'result': _result,
      'photos': photos,
      'remark': _remark.text.trim().isEmpty ? null : _remark.text.trim(),
    };
    try {
      await context.read<AppState>().api.post('/fire/inspect', body: payload);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(I18n.t(context.read<AppState>().locale, 'success'))),
      );
      _remark.clear();
      setState(() {
        _photo = null;
        _qrCode = null;
      });
    } on ApiException catch (e) {
      if (e.message.contains('网络') || e.statusCode == null) {
        await OfflineQueue(context.read<AppState>().api).enqueue('fire', payload);
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(I18n.t(context.read<AppState>().locale, 'networkOffline'))),
        );
      } else {
        setState(() => _error = e.message);
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppState>();
    final t = (String key) => I18n.t(state.locale, key);
    return Scaffold(
      appBar: AppBar(
        title: Text(t('fireInspection')),
        actions: [IconButton(onPressed: _scan, icon: const Icon(Icons.qr_code_scanner))],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (_loading) const LoadingView(),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(t('facility'), style: const TextStyle(fontWeight: FontWeight.bold, color: AppTheme.primary)),
                  const SizedBox(height: 10),
                  DropdownButtonFormField<Map<String, dynamic>>(
                    value: _facility,
                    isExpanded: true,
                    decoration: const InputDecoration(prefixIcon: Icon(Icons.fire_extinguisher_outlined)),
                    items: _facilities.map((facility) {
                      return DropdownMenuItem(
                        value: facility,
                        child: Text(
                          '${facility['name'] ?? facility['facilityNo'] ?? ''} · ${facility['location'] ?? ''}',
                          overflow: TextOverflow.ellipsis,
                        ),
                      );
                    }).toList(),
                    onChanged: (value) => setState(() => _facility = value),
                  ),
                  const SizedBox(height: 14),
                  Text(t('inspectionResult'), style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                  const SizedBox(height: 8),
                  SegmentedButton<String>(
                    segments: [
                      ButtonSegment(value: 'NORMAL', label: Text(t('normal'))),
                      ButtonSegment(value: 'ABNORMAL', label: Text(t('abnormalResult'))),
                    ],
                    selected: {_result},
                    onSelectionChanged: (value) => setState(() => _result = value.first),
                  ),
                  const SizedBox(height: 14),
                  TextField(
                    controller: _remark,
                    maxLines: 3,
                    decoration: InputDecoration(labelText: t('description'), alignLabelWithHint: true),
                  ),
                  const SizedBox(height: 14),
                  Row(
                    children: [
                      OutlinedButton.icon(
                        onPressed: _pickPhoto,
                        icon: const Icon(Icons.camera_alt_outlined),
                        label: Text(t('takePhoto')),
                      ),
                      const SizedBox(width: 12),
                      if (_photo != null)
                        ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: Image.file(_photo!, width: 52, height: 52, fit: BoxFit.cover),
                        ),
                    ],
                  ),
                  if (_error != null) ...[
                    const SizedBox(height: 10),
                    Text(_error!, style: const TextStyle(color: AppTheme.danger, fontSize: 12)),
                  ],
                  const SizedBox(height: 18),
                  FilledButton.icon(
                    onPressed: _submitting ? null : _submit,
                    icon: const Icon(Icons.check_circle_outline),
                    label: Text(_submitting ? t('loading') : t('submit')),
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

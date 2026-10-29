import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';

import '../core/app_state.dart';
import '../core/i18n.dart';
import '../core/theme.dart';

class HazardPage extends StatefulWidget {
  const HazardPage({super.key});

  @override
  State<HazardPage> createState() => _HazardPageState();
}

class _HazardPageState extends State<HazardPage> {
  final _location = TextEditingController();
  final _description = TextEditingController();
  String _level = 'GENERAL';
  File? _photo;
  bool _submitting = false;
  String? _error;

  @override
  void dispose() {
    _location.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _pickPhoto() async {
    final picked = await ImagePicker().pickImage(source: ImageSource.camera, imageQuality: 70);
    if (picked != null) setState(() => _photo = File(picked.path));
  }

  Future<void> _submit() async {
    if (_location.text.trim().isEmpty || _description.text.trim().isEmpty) {
      setState(() => _error = '请填写位置和隐患描述');
      return;
    }
    setState(() {
      _submitting = true;
      _error = null;
    });
    final state = context.read<AppState>();
    String? photos;
    if (_photo != null) {
      try {
        final uploaded = await state.api.upload(_photo!.path, bizType: 'hazard');
        photos = (uploaded['url'] ?? uploaded['path'] ?? '').toString();
      } catch (_) {}
    }
    try {
      await state.api.post('/fire/hazards', body: {
        'hazardLevel': _level,
        'location': _location.text.trim(),
        'description': _description.text.trim(),
        'photos': photos,
        'reporter': state.realName,
        'source': 'APP',
      });
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(I18n.t(state.locale, 'success'))),
      );
      Navigator.pop(context);
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
      appBar: AppBar(title: Text(t('hazardReport'))),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  DropdownButtonFormField<String>(
                    value: _level,
                    decoration: InputDecoration(labelText: t('hazardLevel'), prefixIcon: const Icon(Icons.priority_high)),
                    items: const [
                      DropdownMenuItem(value: 'GENERAL', child: Text('一般')),
                      DropdownMenuItem(value: 'MAJOR', child: Text('较大')),
                      DropdownMenuItem(value: 'CRITICAL', child: Text('重大')),
                    ],
                    onChanged: (value) => setState(() => _level = value ?? 'GENERAL'),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _location,
                    decoration: InputDecoration(labelText: t('location'), prefixIcon: const Icon(Icons.location_on_outlined)),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _description,
                    maxLines: 4,
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
                          child: Image.file(_photo!, width: 56, height: 56, fit: BoxFit.cover),
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
                    icon: const Icon(Icons.report_outlined),
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

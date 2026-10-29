import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:flutter/material.dart';

class QrScannerPage extends StatefulWidget {
  const QrScannerPage({super.key});

  @override
  State<QrScannerPage> createState() => _QrScannerPageState();
}

class _QrScannerPageState extends State<QrScannerPage> {
  bool _handled = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('扫码')),
      body: MobileScanner(
        onDetect: (capture) {
          if (_handled) return;
          final barcodes = capture.barcodes;
          if (barcodes.isEmpty) return;
          final code = barcodes.first.rawValue;
          if (code == null || code.isEmpty) return;
          _handled = true;
          Navigator.pop(context, code);
        },
      ),
    );
  }
}

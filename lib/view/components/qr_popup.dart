import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:revo/controller/extensions/theme.dart';

class QrCodePopup {
  final BuildContext context;
  final String data;

  QrCodePopup({
    required this.context,
    required this.data,
  });

  Future<void> show() async {
    await showDialog(
      context: context,
      builder: (_) => _buildDialog(),
    );
  }

  Widget _buildDialog() {
    return Dialog(
      backgroundColor: context.colorScheme.surfaceContainer,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Scan to add contact',
              style: GoogleFonts.outfit(
                color: context.colorScheme.onSurface,
                fontSize: 20,
              ),
            ),
            const SizedBox(height: 20),
            QrImageView(
              data: data,
              size: 280,
              backgroundColor: Colors.white,
            ),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }
}

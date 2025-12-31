import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:qr_flutter/qr_flutter.dart';

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
      barrierColor: Colors.black.withOpacity(0.5),
      builder: (BuildContext dialogContext) {
        return _buildDialog(dialogContext);
      },
    );
  }

  Widget _buildDialog(BuildContext dialogContext) {
    final colorScheme = Theme.of(dialogContext).colorScheme;

    return AlertDialog(
      backgroundColor: colorScheme.surfaceContainerHighest,
      surfaceTintColor: colorScheme.primary,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(32)),
      title: Text(
        'Share Contact',
        textAlign: TextAlign.center,
        style: GoogleFonts.outfit(
          fontWeight: FontWeight.w600,
          color: colorScheme.onSurface,
        ),
      ),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: 12),
            Container(
              decoration: BoxDecoration(
                color: colorScheme.secondaryContainer,
                borderRadius: BorderRadius.circular(24),
              ),
              padding: const EdgeInsets.all(16),
              child: SizedBox(
                width: 220,
                height: 220,
                child: QrImageView(
                  data: data,
                  version: QrVersions.auto,
                  gapless: false,
                  eyeStyle: QrEyeStyle(
                    eyeShape: QrEyeShape.circle,
                    color: colorScheme.primary,
                  ),
                  dataModuleStyle: QrDataModuleStyle(
                    dataModuleShape: QrDataModuleShape.circle,
                    color: colorScheme.primary,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'Show this code to another user to share your contact details.',
              textAlign: TextAlign.center,
              style: GoogleFonts.outfit(
                color: colorScheme.onSurfaceVariant,
                fontSize: 14,
                height: 1.4,
              ),
            ),
          ],
        ),
      ),
      actions: [
        SizedBox(
          width: double.infinity,
          child: FilledButton.tonal(
            onPressed: () => Navigator.pop(dialogContext),
            style: FilledButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 16),
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16)),
            ),
            child: Text('Done',
                style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
          ),
        ),
      ],
    );
  }
}

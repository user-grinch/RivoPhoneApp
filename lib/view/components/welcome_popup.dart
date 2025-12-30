import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';

class WelcomePopup {
  final BuildContext context;
  final String version;
  final String changelog;

  WelcomePopup({
    required this.context,
    required this.version,
    required this.changelog,
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
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 24.0, horizontal: 24.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            // Close button at top-right
            Align(
              alignment: Alignment.topRight,
              child: IconButton(
                icon: Icon(Icons.close, color: context.colorScheme.onSurface),
                onPressed: () => Navigator.of(context).pop(),
              ),
            ),
            const SizedBox(height: 8),
            Image.asset(
              'assets/icon.png',
              width: 70,
              height: 70,
              fit: BoxFit.cover,
            ),
            const SizedBox(height: 16),
            Text(
              'Rivo',
              style: GoogleFonts.raleway(
                color: context.colorScheme.onSurface,
                fontSize: 28,
                fontWeight: FontWeight.bold,
              ),
            ),
            Text(
              'Version: $version',
              style: GoogleFonts.raleway(
                color: context.colorScheme.onSurfaceVariant,
                fontSize: 16,
              ),
            ),
            const SizedBox(height: 32),
            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                'Changelog:',
                style: GoogleFonts.raleway(
                  color: context.colorScheme.onSurface,
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              changelog,
              style: GoogleFonts.raleway(
                color: context.colorScheme.onSurfaceVariant,
                fontSize: 15,
                height: 1.4,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

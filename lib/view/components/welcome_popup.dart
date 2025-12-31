import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';

class WelcomePopup {
  final BuildContext context;
  final String version;
  final String buildNumber;
  final String changelog;

  WelcomePopup({
    required this.context,
    required this.version,
    required this.buildNumber,
    required this.changelog,
  });

  Future<void> show() async {
    await showDialog(
      context: context,
      builder: (_) => _buildDialog(),
    );
  }

  Widget _buildDialog() {
    final colorScheme = context.colorScheme;

    return AlertDialog(
      backgroundColor: colorScheme.surface,
      surfaceTintColor: colorScheme.surfaceTint,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      icon: Container(
        width: 64,
        height: 64,
        decoration: BoxDecoration(
          color: colorScheme.secondaryContainer,
          borderRadius: BorderRadius.circular(50),
        ),
        child: Padding(
          padding: const EdgeInsets.all(12.0),
          child: Image.asset('assets/icon.png', fit: BoxFit.contain),
        ),
      ),
      title: Column(
        children: [
          Text(
            'Rivo Updated',
            style: GoogleFonts.outfit(
              fontSize: 24,
              fontWeight: FontWeight.w400,
              color: colorScheme.onSurface,
            ),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              _buildPill(
                  version, colorScheme.primary, colorScheme.primaryContainer),
              const SizedBox(width: 8),
              _buildPill('Build $buildNumber', colorScheme.secondary,
                  colorScheme.secondaryContainer),
            ],
          ),
        ],
      ),
      content: Container(
        constraints: const BoxConstraints(maxHeight: 200),
        width: double.maxFinite,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: colorScheme.surfaceContainerLow,
          borderRadius: BorderRadius.circular(16),
        ),
        child: SingleChildScrollView(
          physics: const BouncingScrollPhysics(),
          child: Text(
            changelog,
            style: GoogleFonts.outfit(
              fontSize: 14,
              height: 1.5,
              color: colorScheme.onSurfaceVariant,
            ),
          ),
        ),
      ),
      actions: [
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            style: FilledButton.styleFrom(
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12)),
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
            onPressed: () => Navigator.pop(context),
            child: const Text('Got it'),
          ),
        ),
      ],
      actionsPadding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
    );
  }

  Widget _buildPill(String text, Color textColor, Color bgColor) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        text,
        style: GoogleFonts.outfit(
          fontSize: 11,
          fontWeight: FontWeight.bold,
          color: textColor,
        ),
      ),
    );
  }
}

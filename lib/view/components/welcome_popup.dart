import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';

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
    await showGeneralDialog(
      context: context,
      barrierDismissible: true,
      barrierLabel: '',
      barrierColor: Colors.black54,
      pageBuilder: (context, anim1, anim2) => _buildDialog(),
    );
  }

  Widget _buildDialog() {
    final colorScheme = context.colorScheme;

    return Center(
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 24),
        decoration: BoxDecoration(
          color: colorScheme.surface,
          borderRadius: BorderRadius.circular(40),
        ),
        child: Material(
          color: Colors.transparent,
          child: Padding(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _buildHeader(colorScheme),
                const SizedBox(height: 24),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    _buildPill('v$version', colorScheme.primary,
                        colorScheme.primaryContainer),
                    const SizedBox(width: 8),
                    _buildPill('Build $buildNumber', colorScheme.secondary,
                        colorScheme.secondaryContainer),
                  ],
                ),
                const SizedBox(height: 28),
                _buildChangelogArea(colorScheme),
                const SizedBox(height: 32),
                _buildActionButton(colorScheme),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildHeader(ColorScheme colorScheme) {
    return Column(
      children: [
        Container(
          height: 84,
          width: 84,
          decoration: BoxDecoration(
            color: colorScheme.secondaryContainer.withOpacity(0.4),
            borderRadius: BorderRadius.circular(28),
          ),
          child: Center(
            child: Image.asset('assets/icon.png', height: 48, width: 48),
          ),
        ),
        const SizedBox(height: 16),
        Text(
          'Freshly Baked',
          style: GoogleFonts.outfit(
            fontSize: 28,
            fontWeight: FontWeight.bold,
            color: colorScheme.onSurface,
            letterSpacing: -0.5,
          ),
        ),
      ],
    );
  }

  Widget _buildChangelogArea(ColorScheme colorScheme) {
    return Container(
      constraints: const BoxConstraints(maxHeight: 200),
      width: double.infinity,
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withOpacity(0.3),
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: colorScheme.outlineVariant.withOpacity(0.3)),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          physics: const BouncingScrollPhysics(),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(FluentIcons.sparkle_16_regular,
                      size: 16, color: colorScheme.primary),
                  const SizedBox(width: 8),
                  Text(
                    'HIGHLIGHTS',
                    style: GoogleFonts.outfit(
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                      letterSpacing: 1.5,
                      color: colorScheme.primary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                changelog,
                style: GoogleFonts.outfit(
                  fontSize: 15,
                  height: 1.6,
                  color: colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildActionButton(ColorScheme colorScheme) {
    return SizedBox(
      width: double.infinity,
      height: 64,
      child: FilledButton(
        onPressed: () => Navigator.pop(context),
        style: FilledButton.styleFrom(
          backgroundColor: colorScheme.primary,
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
          elevation: 0,
        ),
        child: Text(
          'Let\'s Go',
          style: GoogleFonts.outfit(fontSize: 18, fontWeight: FontWeight.bold),
        ),
      ),
    );
  }

  Widget _buildPill(String text, Color textColor, Color bgColor) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
      decoration: BoxDecoration(
        color: bgColor.withOpacity(0.7),
        borderRadius: BorderRadius.circular(100),
      ),
      child: Text(
        text,
        style: GoogleFonts.outfit(
          fontSize: 12,
          fontWeight: FontWeight.bold,
          color: textColor,
        ),
      ),
    );
  }
}

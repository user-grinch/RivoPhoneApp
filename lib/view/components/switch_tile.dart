import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';

class SwitchTileWidget extends StatelessWidget {
  final String title;
  final String subtitle;
  final bool value;
  final ValueChanged<bool> onChanged;
  final bool isFirst;
  final bool isLast;

  const SwitchTileWidget({
    super.key,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onChanged,
    this.isFirst = false,
    this.isLast = false,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = context.colorScheme;

    // Consistent Expressive radius
    const double radius = 28.0;

    final borderRadius = BorderRadius.vertical(
      top: isFirst ? const Radius.circular(radius) : Radius.zero,
      bottom: isLast ? const Radius.circular(radius) : Radius.zero,
    );

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 1.0),
      child: Container(
        decoration: BoxDecoration(
          color: colorScheme.secondaryContainer.withOpacity(0.35),
          borderRadius: borderRadius,
        ),
        child: SwitchListTile(
          value: value,
          onChanged: onChanged,
          shape: RoundedRectangleBorder(borderRadius: borderRadius),
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 24.0, vertical: 8.0),
          title: Text(
            title,
            style: GoogleFonts.outfit(
              fontSize: 17,
              fontWeight: FontWeight.w600,
              color: colorScheme.onSurface,
            ),
          ),
          subtitle: Text(
            subtitle,
            style: TextStyle(
              fontSize: 13,
              color: colorScheme.onSurfaceVariant.withOpacity(0.8),
            ),
          ),
          activeColor: colorScheme.primary,
          activeTrackColor: colorScheme.primaryContainer,
        ),
      ),
    );
  }
}

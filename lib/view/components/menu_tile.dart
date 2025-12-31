import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';

class MenuTile extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final VoidCallback? onTap;
  final bool isFirst;
  final bool isLast;

  const MenuTile({
    super.key,
    required this.title,
    required this.subtitle,
    required this.icon,
    this.onTap,
    this.isFirst = false,
    this.isLast = false,
  });

  @override
  Widget build(BuildContext context) {
    final bool isEnabled = onTap != null;
    final colorScheme = context.colorScheme;
    final double radius = 28.0;
    final borderRadius = BorderRadius.vertical(
      top: isFirst ? Radius.circular(radius) : Radius.zero,
      bottom: isLast ? Radius.circular(radius) : Radius.zero,
    );

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 1.0),
      child: Material(
        color: Colors.transparent,
        child: Container(
          decoration: BoxDecoration(
            color: isEnabled
                ? colorScheme.secondaryContainer.withAlpha(150)
                : colorScheme.surfaceVariant.withAlpha(50),
            borderRadius: borderRadius,
          ),
          child: ListTile(
            onTap: onTap,
            shape: RoundedRectangleBorder(borderRadius: borderRadius),
            contentPadding:
                const EdgeInsets.symmetric(horizontal: 24.0, vertical: 4.0),
            leading: Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: isEnabled ? colorScheme.surface : Colors.transparent,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Icon(
                icon,
                color: isEnabled
                    ? colorScheme.primary
                    : colorScheme.onSurface.withOpacity(0.38),
                size: 26,
              ),
            ),
            title: Text(
              title,
              style: GoogleFonts.outfit(
                fontSize: 17,
                fontWeight: FontWeight.w600,
                color: isEnabled
                    ? colorScheme.onSurface
                    : colorScheme.onSurface.withOpacity(0.38),
              ),
            ),
            subtitle: Text(
              subtitle,
              style: TextStyle(
                fontSize: 13,
                color: isEnabled
                    ? colorScheme.onSurfaceVariant.withOpacity(0.7)
                    : colorScheme.onSurface.withOpacity(0.38),
              ),
            ),
            trailing: isEnabled
                ? Icon(Icons.chevron_right,
                    color: colorScheme.onSurfaceVariant.withOpacity(0.3))
                : null,
          ),
        ),
      ),
    );
  }
}

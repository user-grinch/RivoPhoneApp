import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/view/components/rounded_icon_btn.dart';

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

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Material(
          color: Colors.transparent,
          child: Container(
            decoration: BoxDecoration(
              color: isEnabled
                  ? colorScheme.secondaryContainer.withOpacity(0.35)
                  : colorScheme.surfaceVariant.withOpacity(0.15),
              borderRadius: borderRadius,
            ),
            child: Column(
              children: [
                ListTile(
                  onTap: onTap,
                  shape: RoundedRectangleBorder(borderRadius: borderRadius),
                  contentPadding: const EdgeInsets.symmetric(
                      horizontal: 24.0, vertical: 4.0),
                  leading: RoundedIconButton(
                    icon,
                    size: 50,
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
                if (!isLast)
                  Padding(
                    padding: const EdgeInsets.only(left: 76.0, right: 24.0),
                    child: Divider(
                      height: 1,
                      thickness: 1,
                      color: colorScheme.outlineVariant.withOpacity(0.2),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/controller/extensions/theme.dart';

class DialActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Function()? func;

  const DialActionButton({
    required this.icon,
    required this.label,
    this.func,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = context.colorScheme;

    return ButtonM3E(
      onPressed: func,
      size: ButtonM3ESize.md,
      label: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 24),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 24,
              color: colorScheme.onPrimary,
            ),
            const SizedBox(width: 8),
            Text(
              label,
              style: GoogleFonts.outfit(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: colorScheme.onPrimary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

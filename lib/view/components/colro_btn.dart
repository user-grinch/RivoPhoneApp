import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class LinkButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;
  final bool outlined; // if true, shows outline instead of filled

  const LinkButton({
    Key? key,
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
    this.outlined = false, // default is filled
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final buttonStyle = outlined
        ? OutlinedButton.styleFrom(
            side: BorderSide(color: color, width: 2),
            foregroundColor: color,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(24),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 16),
          )
        : ElevatedButton.styleFrom(
            backgroundColor: color,
            foregroundColor: Colors.white,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(24),
            ),
            elevation: 2,
            padding: const EdgeInsets.symmetric(horizontal: 16),
          );

    final buttonChild = Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 20),
        const SizedBox(width: 8),
        Text(
          label,
          style: GoogleFonts.outfit(
            fontWeight: FontWeight.w600,
            fontSize: 14,
          ),
        ),
      ],
    );

    return SizedBox(
      height: 48,
      child: outlined
          ? OutlinedButton(
              onPressed: onTap, style: buttonStyle, child: buttonChild)
          : ElevatedButton(
              onPressed: onTap, style: buttonStyle, child: buttonChild),
    );
  }
}

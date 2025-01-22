import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/theme.dart';

class RoundedIconButton extends StatelessWidget {
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;
  final IconData icon;
  final double size;
  final String text;

  const RoundedIconButton(
    BuildContext context, {
    super.key,
    required this.icon,
    required this.size,
    this.text = '',
    this.onTap,
    this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        GestureDetector(
          onTap: onTap,
          onLongPress: onLongPress,
          child: Container(
            decoration: BoxDecoration(
              color: context.colorScheme.primaryContainer,
              shape: BoxShape.circle,
            ),
            width: size,
            height: size,
            child: Icon(
              icon,
              color: context.colorScheme.onPrimaryContainer,
              size: size / 1.75,
            ),
          ),
        ),
        const SizedBox(height: 8),
        if (text.isNotEmpty)
          Text(
            text,
            style: GoogleFonts.cabin(
              textStyle: context.textTheme.bodyLarge,
              color: context.colorScheme.onSurface,
              fontSize: 12,
            ),
          ),
      ],
    );
  }
}

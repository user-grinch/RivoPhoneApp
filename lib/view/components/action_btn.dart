import 'package:flutter/material.dart';

class CallActionButton extends StatelessWidget {
  final String label;
  final IconData icon;
  final Color backgroundColor;
  final Color foregroundColor;
  final VoidCallback onTap;
  final bool isLarge;
  final bool isSuperLarge;

  const CallActionButton({
    super.key,
    required this.label,
    required this.icon,
    required this.backgroundColor,
    required this.foregroundColor,
    required this.onTap,
    this.isLarge = false,
    this.isSuperLarge = false,
  });

  @override
  Widget build(BuildContext context) {
    // Both Large and SuperLarge should probably show the text label
    final showText = isLarge || isSuperLarge;

    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      curve: Curves.fastOutSlowIn,
      child: Material(
        color: backgroundColor,
        shape: showText
            ? const StadiumBorder()
            : RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          child: Container(
            height: 72,
            width: isSuperLarge ? double.infinity : null,
            padding: EdgeInsets.symmetric(horizontal: showText ? 32 : 24),
            child: Row(
              mainAxisSize: isSuperLarge ? MainAxisSize.max : MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(icon, color: foregroundColor, size: 28),
                if (showText) ...[
                  const SizedBox(width: 12),
                  Text(
                    label,
                    style: TextStyle(
                      color: foregroundColor,
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                      letterSpacing: 0.2,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:revo/controller/extensions/theme.dart';

class RoundedIconButton extends StatelessWidget {
  final VoidCallback? onPressed;
  final VoidCallback? onLongPress;
  final IconData icon;
  final bool isActive;
  final double size;

  const RoundedIconButton(
    this.icon, {
    super.key,
    this.isActive = false,
    this.size = 60,
    this.onPressed,
    this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onPressed,
      onLongPress: onLongPress,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        curve: Curves.easeInOut,
        width: size,
        height: size,
        decoration: BoxDecoration(
            color: isActive
                ? context.colorScheme.primaryContainer
                : context.colorScheme.secondaryContainer.withAlpha(150),
            borderRadius: BorderRadius.circular(size * 0.3)),
        child: Icon(
          icon,
          color: context.colorScheme.primary,
          size: size * 0.45,
        ),
      ),
    );
  }
}

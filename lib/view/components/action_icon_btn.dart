import 'package:flutter/material.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/controller/extensions/theme.dart';

class ActionIconButton extends StatelessWidget {
  final VoidCallback? onPressed;
  final VoidCallback? onLongPress;
  final IconData icon;
  final bool isActive;
  final double size;

  const ActionIconButton(
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
      onLongPress: onLongPress,
      child: ButtonM3E(
        onPressed: onPressed,
        label: Icon(
          icon,
          color: context.colorScheme.primary,
          size: size * 0.45,
        ),
        style: ButtonM3EStyle.tonal,
        size: size > 50 ? ButtonM3ESize.md : ButtonM3ESize.sm,
      ),
    );
  }
}

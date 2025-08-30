import 'package:flutter/material.dart';
import 'package:revo/extensions/theme.dart';

class DialActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Function()? func;

  const DialActionButton(
      {required this.icon, required this.label, this.func, super.key});

  @override
  Widget build(BuildContext context) {
    return TextButton(
      style: TextButton.styleFrom(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(50),
        ),
        backgroundColor: context.colorScheme.secondaryContainer.withAlpha(150),
        elevation: 0,
        padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 10),
      ),
      onPressed: func,
      child: Row(
        children: [
          Icon(
            icon,
            color: context.colorScheme.onSurface,
          ),
          SizedBox(
            width: 2,
          ),
          Text(
            label,
            style:
                TextStyle(fontSize: 18, color: context.colorScheme.onSurface),
          ),
        ],
      ),
    );
  }
}

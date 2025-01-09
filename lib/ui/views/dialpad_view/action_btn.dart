import 'package:flutter/material.dart';
import 'package:revo/extentions/theme.dart';

class DialActionButton extends StatelessWidget {
  final IconData icon;
  final String label;

  const DialActionButton({required this.icon, required this.label, super.key});

  @override
  Widget build(BuildContext context) {
    return TextButton(
      style: TextButton.styleFrom(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(50),
        ),
        backgroundColor: context.colorScheme.primaryContainer,
        elevation: 0,
        padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 10),
      ),
      onPressed: () {},
      child: Row(
        children: [
          Icon(Icons.sim_card),
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

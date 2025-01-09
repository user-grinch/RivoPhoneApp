import 'package:flutter/material.dart';
import 'package:revo/extentions/theme.dart';

class DialPadButton extends StatelessWidget {
  final String mainText;
  final String? subText;
  final Function(String) onUpdate;

  const DialPadButton({
    required this.mainText,
    this.subText,
    super.key,
    required this.onUpdate,
  });

  @override
  Widget build(BuildContext context) {
    return TextButton(
      style: TextButton.styleFrom(
        elevation: 0,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(50)),
        ),
        backgroundColor: context.colorScheme.surface.withAlpha(180),
      ),
      onPressed: () {
        onUpdate(mainText);
      },
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            mainText,
            style: const TextStyle(fontSize: 24),
          ),
          if (subText != null)
            Text(
              subText!,
              style: const TextStyle(fontSize: 10),
            ),
        ],
      ),
    );
  }
}

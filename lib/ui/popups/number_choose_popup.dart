import 'package:flutter/material.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/extensions/theme.dart';
import 'package:revo/utils/center_text.dart';

Widget numberChooserDialog(
  BuildContext context,
  List<String> numbers,
  void Function(String)? onTap,
) {
  return Dialog(
    backgroundColor: context.colorScheme.surfaceContainer,
    shape: RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(24),
    ),
    alignment: Alignment.bottomCenter,
    child: Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CenterText(
            text: "Choose a number",
            size: 24,
          ),
          const SizedBox(height: 8),
          Column(
            children: numbers.map((number) {
              return _buildNumberOption(context, number, onTap);
            }).toList(),
          ),
        ],
      ),
    ),
  );
}

Widget _buildNumberOption(
  BuildContext context,
  String number,
  void Function(String)? onTap,
) {
  return Card(
    elevation: 0,
    margin: const EdgeInsets.symmetric(vertical: 4),
    shape: RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(24),
    ),
    color: context.colorScheme.secondaryContainer,
    child: InkWell(
      onTap: () {
        if (onTap != null) {
          onTap(number);
        }
      },
      borderRadius: BorderRadius.circular(20),
      child: Padding(
        padding: const EdgeInsets.all(6.0),
        child: Row(
          children: [
            CircleAvatar(
              radius: 18,
              backgroundColor: Theme.of(context).colorScheme.primary,
              child: Icon(
                HugeIcons.strokeRoundedSmartPhone01,
                color: context.colorScheme.onPrimary,
                size: 18,
              ),
            ),
            const SizedBox(width: 16),
            CenterText(
              text: number,
              size: 18,
            ),
          ],
        ),
      ),
    ),
  );
}

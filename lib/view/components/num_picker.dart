import 'package:flutter/material.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/view/utils/center_text.dart';

class NumberPicker {
  final BuildContext context;
  final List<String> numbers;
  final void Function(String)? onTap;

  NumberPicker({
    required this.context,
    required this.numbers,
    this.onTap,
  });

  Future<void> show() async {
    if (numbers.isEmpty) return;

    await showDialog(
      context: context,
      builder: (_) => _buildDialog(),
    );
  }

  Widget _buildDialog() {
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
            const CenterText(
              text: "Choose a number",
              size: 24,
            ),
            const SizedBox(height: 8),
            ...numbers.map((number) => _buildNumberOption(number)),
          ],
        ),
      ),
    );
  }

  Widget _buildNumberOption(String number) {
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
            onTap!(number);
          }
          Navigator.of(context).pop();
        },
        borderRadius: BorderRadius.circular(24),
        child: Padding(
          padding: const EdgeInsets.all(6.0),
          child: Row(
            children: [
              CircleAvatar(
                radius: 18,
                backgroundColor: context.colorScheme.primary,
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
}

import 'package:flutter/material.dart';

class SwitchTileWidget extends StatelessWidget {
  final String title;
  final String subtitle;
  final bool value;
  final ValueChanged<bool> onChanged;
  final bool isFirst;
  final bool isLast;

  const SwitchTileWidget({
    super.key,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onChanged,
    this.isFirst = false,
    this.isLast = false,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(2.0),
      child: Container(
        decoration: BoxDecoration(
          color:
              Theme.of(context).colorScheme.secondaryContainer.withAlpha(110),
          borderRadius: BorderRadius.vertical(
            top: isFirst ? const Radius.circular(15) : Radius.zero,
            bottom: isLast ? const Radius.circular(15) : Radius.zero,
          ),
        ),
        child: SwitchListTile(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.only(
              topLeft: isFirst ? const Radius.circular(15) : Radius.zero,
              topRight: isFirst ? const Radius.circular(15) : Radius.zero,
              bottomLeft: isLast ? const Radius.circular(15) : Radius.zero,
              bottomRight: isLast ? const Radius.circular(15) : Radius.zero,
            ),
          ),
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 20.0, vertical: 2),
          title: Text(
            title,
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
          ),
          subtitle: Text(
            subtitle,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w500,
              color: Theme.of(context)
                  .colorScheme
                  .onSecondaryContainer
                  .withAlpha(180),
            ),
          ),
          value: value,
          onChanged: onChanged,
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';

import 'package:google_fonts/google_fonts.dart';
import 'package:revo/view/components/action_icon_btn.dart';
import 'package:revo/view/screen/settings/appbarcomponent.dart';

class RadioSelectionPage<T> extends StatefulWidget {
  final String title;
  final List<({String title, String subtitle, IconData icon, T value})> options;
  final T initialValue;
  final ValueChanged<T> onSelected;

  const RadioSelectionPage({
    super.key,
    required this.title,
    required this.options,
    required this.initialValue,
    required this.onSelected,
  });

  @override
  State<RadioSelectionPage<T>> createState() => _RadioSelectionPageState<T>();
}

class _RadioSelectionPageState<T> extends State<RadioSelectionPage<T>> {
  late T _selectedValue = widget.initialValue;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surface,
      appBar: AppBarComponent(widget.title),
      body: ListView.builder(
        padding: const EdgeInsets.all(20),
        itemCount: widget.options.length,
        itemBuilder: (context, index) {
          final item = widget.options[index];
          final isSelected = item.value == _selectedValue;

          // Determine rounding based on position
          final isFirst = index == 0;
          final isLast = index == widget.options.length - 1;
          final borderRadius = BorderRadius.vertical(
            top: isFirst ? const Radius.circular(28) : Radius.zero,
            bottom: isLast ? const Radius.circular(28) : Radius.zero,
          );

          return Container(
            decoration: BoxDecoration(
              borderRadius: borderRadius,
              color: colorScheme.secondaryContainer.withOpacity(0.35),
            ),
            child: Column(
              children: [
                ListTile(
                  onTap: () {
                    setState(() => _selectedValue = item.value);
                    widget.onSelected(item.value);
                    Future.delayed(const Duration(milliseconds: 200),
                        () => Navigator.of(context).pop());
                  },
                  shape: RoundedRectangleBorder(borderRadius: borderRadius),
                  contentPadding:
                      const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
                  leading: ActionIconButton(item.icon, size: 50),
                  title: Text(item.title,
                      style: GoogleFonts.outfit(
                          fontSize: 17, fontWeight: FontWeight.w600)),
                  subtitle: Text(item.subtitle,
                      style: TextStyle(
                          fontSize: 13,
                          color:
                              colorScheme.onSurfaceVariant.withOpacity(0.7))),
                  trailing: Radio<T>(
                    value: item.value,
                    groupValue: _selectedValue,
                    onChanged: (val) {
                      setState(() => _selectedValue = val as T);
                      widget.onSelected(item.value);
                      Future.delayed(const Duration(milliseconds: 200),
                          () => Navigator.of(context).pop());
                    },
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(left: 24.0, right: 24.0),
                  child: Divider(
                    height: 1,
                    thickness: 1,
                    color: colorScheme.outlineVariant.withOpacity(0.2),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

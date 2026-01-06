import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:phone_numbers_parser/phone_numbers_parser.dart' as pnp;
import 'package:revo/controller/extensions/theme.dart';

class NumberPicker {
  final BuildContext context;
  final List<pnp.PhoneNumber> numbers;
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
      barrierDismissible: true,
      builder: (_) => _buildDialog(),
    );
  }

  Widget _buildDialog() {
    return Dialog(
      backgroundColor: context.colorScheme.surfaceContainerHigh,
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(32)),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              FluentIcons.phone_24_regular,
              color: context.colorScheme.primary,
              size: 32,
            ),
            const SizedBox(height: 16),
            Text(
              "Select Number",
              style: GoogleFonts.outfit(
                fontSize: 22,
                fontWeight: FontWeight.w600,
                color: context.colorScheme.onSurface,
              ),
            ),
            const SizedBox(height: 24),
            // Map the list of strings to the modern picker items
            ...numbers.map((number) => _buildNumberItem(number.international)),
          ],
        ),
      ),
    );
  }

  Widget _buildNumberItem(String number) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Material(
        color: context.colorScheme.secondaryContainer,
        borderRadius: BorderRadius.circular(28),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: () {
            if (onTap != null) onTap!(number);
            context.pop();
          },
          splashFactory: InkSparkle.splashFactory,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: context.colorScheme.primary,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Center(
                    child: Icon(
                      FluentIcons.call_24_regular,
                      color: context.colorScheme.onPrimary,
                      size: 20,
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        number,
                        style: GoogleFonts.outfit(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: context.colorScheme.onSecondaryContainer,
                        ),
                      ),
                      Text(
                        "Phone Number",
                        style: GoogleFonts.outfit(
                          fontSize: 13,
                          color: context.colorScheme.onSecondaryContainer
                              .withOpacity(0.7),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

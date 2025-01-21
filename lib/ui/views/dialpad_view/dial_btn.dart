import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
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
        backgroundColor: context.colorScheme.surface.withAlpha(150),
        overlayColor: context.colorScheme.onSurface,
      ),
      onPressed: () {
        onUpdate(mainText);
      },
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            mainText,
            style: GoogleFonts.cabin(
              fontSize: 28,
              fontWeight: FontWeight.bold,
              color: context.colorScheme.onSurface,
            ),
          ),
          if (subText != null)
            Text(
              subText!,
              style: GoogleFonts.cabin(
                fontSize: 12,
                color: context.colorScheme.onSurface,
              ),
            ),
        ],
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_dtmf/dtmf.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/pref_service.dart';
import 'package:revo/controller/utils/utils.dart';

class DialPadButton extends StatefulWidget {
  final String mainText;
  final String? subText;
  final Function(String) onUpdate;

  const DialPadButton({
    required this.mainText,
    this.subText,
    required this.onUpdate,
    super.key,
  });

  @override
  State<DialPadButton> createState() => _DialPadButtonState();
}

class _DialPadButtonState extends State<DialPadButton> {
  @override
  Widget build(BuildContext context) {
    final colorScheme = context.colorScheme;
    bool letters = SharedPrefService().getBool(PREF_DIALPAD_LETTERS, def: true);

    double textSz = widget.mainText == "*" ? 40 : 32;
    if (!letters) {
      textSz += 4;
    }

    return TextButton(
      style: TextButton.styleFrom(
        elevation: 0,
        padding: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(28),
        ),
        backgroundColor: colorScheme.secondaryContainer.withOpacity(0.5),
        overlayColor: colorScheme.primary,
      ),
      onPressed: () async {
        if (SharedPrefService().getBool(PREF_DTMF_TONE, def: true)) {
          await Dtmf.playTone(digits: widget.mainText, volume: 1);
        }
        hapticVibration();
        widget.onUpdate(widget.mainText);
      },
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            widget.mainText,
            style: GoogleFonts.outfit(
              fontSize: textSz,
              fontWeight: FontWeight.w500,
              color: colorScheme.onSecondaryContainer,
              height: 1.0,
            ),
          ),
          if (widget.subText != null && letters)
            Padding(
              padding: const EdgeInsets.only(top: 2),
              child: Text(
                widget.subText!,
                style: GoogleFonts.outfit(
                  fontSize: 11,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 1.5,
                  color: colorScheme.onSecondaryContainer.withOpacity(0.5),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

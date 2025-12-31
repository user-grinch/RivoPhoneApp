import 'package:flutter/material.dart';
import 'package:flutter_dtmf/dtmf.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/pref_service.dart';
import 'package:revo/view/utils/utils.dart';

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
    bool letters = SharedPrefService().getBool(PREF_DIALPAD_LETTERS, def: true);
    double textSz = widget.mainText == "*" ? 45 : 32;

    if (!letters) {
      textSz += 8;
    }

    return TextButton(
      style: TextButton.styleFrom(
        elevation: 0,
        padding: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(24),
        ),
        backgroundColor: context.colorScheme.secondaryContainer,
        overlayColor: context.colorScheme.onSecondaryContainer,
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
              fontWeight: FontWeight.w400,
              color: context.colorScheme.onSecondaryContainer,
              height: 1.1,
            ),
          ),
          if (widget.subText != null && letters)
            Text(
              widget.subText!,
              style: GoogleFonts.outfit(
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color:
                    context.colorScheme.onSecondaryContainer.withOpacity(0.4),
              ),
            ),
        ],
      ),
    );
  }
}

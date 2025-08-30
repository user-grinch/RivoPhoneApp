import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/extentions/theme.dart';
import 'package:flutter_dtmf/dtmf.dart';
import 'package:flutter/services.dart';
import 'package:revo/services/prefservice.dart';
import 'package:revo/utils/utils.dart';

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
  _DialPadButtonState createState() => _DialPadButtonState();
}

class _DialPadButtonState extends State<DialPadButton> {
  @override
  Widget build(BuildContext context) {
    bool letters = SharedPrefService().getBool(PREF_DIALPAD_LETTERS, def: true);
    double textSz = widget.mainText == "*" ? 45 : 20;

    if (!letters) {
      textSz += 10;
    }
    return TextButton(
      style: TextButton.styleFrom(
        elevation: 0,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(50)),
        ),
        backgroundColor: context.colorScheme.secondaryContainer.withAlpha(150),
        overlayColor: context.colorScheme.onSurface,
      ),
      onPressed: () async {
        if (SharedPrefService().getBool(PREF_DTMF_TONE, def: true)) {
          await Dtmf.playTone(digits: widget.mainText, volume: 3);
        }
        hapticVibration();
        widget.onUpdate(widget.mainText);
      },
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Expanded(
            child: Text(
              widget.mainText,
              style: GoogleFonts.raleway(
                fontSize: textSz,
                fontWeight: FontWeight.normal,
                color: context.colorScheme.onSurface,
              ),
            ),
          ),
          if (widget.subText != null)
            Text(
              widget.subText!,
              style: GoogleFonts.raleway(
                fontSize: 12,
                color: context.colorScheme.onSurface,
              ),
            ),
        ],
      ),
    );
  }
}

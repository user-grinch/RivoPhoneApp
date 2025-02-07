import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/theme.dart';

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
  void initState() {
    super.initState();
  }

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
      onPressed: () async {
        widget.onUpdate(widget.mainText);
      },
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            widget.mainText,
            style: GoogleFonts.raleway(
              fontSize: 28,
              fontWeight: FontWeight.normal,
              color: context.colorScheme.onSurface,
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

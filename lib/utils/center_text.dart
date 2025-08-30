import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extensions/theme.dart';

class CenterText extends StatelessWidget {
  final String text;
  final double size;
  const CenterText({super.key, required this.text, this.size = 16.0});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Text(
        text,
        style: GoogleFonts.raleway(
          fontSize: size,
          color: context.colorScheme.onSurface,
        ),
      ),
    );
  }
}

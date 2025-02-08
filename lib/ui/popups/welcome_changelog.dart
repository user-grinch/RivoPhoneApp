import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/theme.dart';

Widget welcomePopup(
  BuildContext context,
  String version,
  String changelog,
) {
  return Dialog(
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
    backgroundColor: context.colorScheme.surfaceContainer,
    child: Padding(
      padding: const EdgeInsets.symmetric(vertical: 20.0, horizontal: 24.0),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 12.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Image.asset(
              'assets/icon.png',
              width: 70,
              height: 70,
              fit: BoxFit.cover,
            ),
            SizedBox(height: 16),
            Text(
              'Rivo',
              style: GoogleFonts.raleway(
                color: context.colorScheme.onSurface,
                fontSize: 30,
                fontWeight: FontWeight.bold,
              ),
            ),
            Text(
              'Version: $version',
              style: GoogleFonts.raleway(
                color: context.colorScheme.onSurfaceVariant,
                fontSize: 16,
              ),
            ),
            SizedBox(height: 50),
            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                'Changelog:',
                style: GoogleFonts.raleway(
                  color: context.colorScheme.onSurface,
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            SizedBox(height: 8),
            Text(
              changelog,
              style: GoogleFonts.raleway(
                color: context.colorScheme.onSurfaceVariant,
                fontSize: 16,
                height: 1.3,
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

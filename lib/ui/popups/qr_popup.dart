import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:revo/extentions/theme.dart';

Widget qrCodePopup(
  BuildContext context,
  String data,
) {
  return Dialog(
    backgroundColor: context.colorScheme.surfaceContainer,
    child: Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            'Scan to add contact',
            style: GoogleFonts.raleway(
              color: context.colorScheme.onSurface,
              fontSize: 20,
            ),
          ),
          SizedBox(height: 20),
          QrImageView(
            data: data,
            size: 280,
            backgroundColor: Colors.white,
          ),
          SizedBox(height: 20),
        ],
      ),
    ),
  );
}

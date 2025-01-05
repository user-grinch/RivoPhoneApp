import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:revo/extentions/theme.dart';

class QRCodePopup extends StatelessWidget {
  final String data;

  QRCodePopup({required this.data});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            QrImageView(
              data: data,
              size: 300,
              backgroundColor: Colors.white,
            ),
            SizedBox(height: 40),
            Text(
              'Scan the QR Code to add contact',
              style: GoogleFonts.cabin(
                color: context.colorScheme.primary,
                fontSize: 20,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

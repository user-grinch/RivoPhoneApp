import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:revo/services/cubit/contact_service.dart';

class QRScannerView extends StatelessWidget {
  const QRScannerView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: Icon(HugeIcons.strokeRoundedArrowLeft01),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text('Scan QR Code to add contact'),
      ),
      body: Stack(
        children: [
          MobileScanner(
            scanWindow: Rect.fromCenter(
              center: Offset(MediaQuery.of(context).size.width / 2,
                  MediaQuery.of(context).size.height / 2),
              width: 300,
              height: 400,
            ),
            onDetect: (capture) async {
              final List<Barcode> barcodes = capture.barcodes;
              final Barcode barcode = barcodes.first;
              if (barcode != null && barcode.rawValue != null) {
                String data = barcode.rawValue!;
                if (_isValidVCard(data)) {
                  context.read<ContactService>().insertContactFromVCard(data);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                        content: Text('Contact added successfully!')),
                  );
                  Navigator.of(context).pop();
                } else {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Invalid vCard format!')),
                  );
                }
              }
            },
          ),
          Center(
            child: Container(
              width: 300,
              height: 400,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.white, width: 3),
                borderRadius: BorderRadius.circular(16),
              ),
            ),
          ),
          Align(
            alignment: Alignment.bottomCenter,
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Text(
                'Position the QR code within the frame to scan.',
                style: TextStyle(
                  fontSize: 16,
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                  shadows: [
                    Shadow(
                      color: Colors.black.withAlpha(128),
                      offset: Offset(0, 1),
                      blurRadius: 2,
                    ),
                  ],
                ),
                textAlign: TextAlign.center,
              ),
            ),
          ),
        ],
      ),
    );
  }

  bool _isValidVCard(String data) {
    return data.startsWith("BEGIN:VCARD") && data.endsWith("END:VCARD");
  }
}

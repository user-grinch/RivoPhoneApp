import 'package:flutter/material.dart';
import 'package:flutter_contacts/flutter_contacts.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:flutter_contacts/properties/phone.dart' as fc_phone;
import 'package:flutter_contacts/properties/email.dart' as fc_mail;

class QRScannerView extends StatelessWidget {
  const QRScannerView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
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
              final Barcode? barcode = barcodes.first;
              if (barcode != null && barcode.rawValue != null) {
                String data = barcode.rawValue!;
                if (_isValidVCard(data)) {
                  await _addContact(data);
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

  Future<void> _addContact(String vCardData) async {
    if (await FlutterContacts.requestPermission()) {
      final Map<String, String> contactDetails = _parseVCard(vCardData);

      final Contact newContact = Contact(
        name: Name(first: contactDetails['FN'] ?? '', last: ''),
        phones: contactDetails['TEL'] != null
            ? [fc_phone.Phone(contactDetails['TEL']!)]
            : [],
        emails: contactDetails['EMAIL'] != null
            ? [fc_mail.Email(contactDetails['EMAIL']!)]
            : [],
      );

      try {
        await FlutterContacts.insertContact(newContact);
        print('Contact added successfully!');
      } catch (e) {
        print('Error adding contact: $e');
      }
    } else {
      print('Permission to access contacts denied!');
    }
  }

  Map<String, String> _parseVCard(String vCardData) {
    final Map<String, String> contactDetails = {};

    for (String line in vCardData.split('\n')) {
      if (line.startsWith('FN:')) {
        contactDetails['FN'] = line.replaceFirst('FN:', '').trim();
      } else if (line.startsWith('TEL:')) {
        contactDetails['TEL'] = line.replaceFirst('TEL:', '').trim();
      } else if (line.startsWith('EMAIL:')) {
        contactDetails['EMAIL'] = line.replaceFirst('EMAIL:', '').trim();
      }
    }

    return contactDetails;
  }
}

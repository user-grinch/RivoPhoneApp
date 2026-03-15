import 'dart:math';

import 'package:android_intent_plus/android_intent.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/controller/services/pref_service.dart';
import 'package:revo/model/contact.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:phone_numbers_parser/phone_numbers_parser.dart';

String normalizePhoneNumber(String phoneNumber, {String? countryCode}) {
  try {
    phoneNumber = phoneNumber.replaceAll(RegExp(r'\s+|-'), '');

    IsoCode? isoCode;
    if (countryCode != null) {
      isoCode = IsoCode.values.byName(countryCode.toUpperCase());
    }

    if (!phoneNumber.startsWith('+') && phoneNumber.startsWith('0')) {
      phoneNumber = phoneNumber.substring(1);
    }

    final parsed = phoneNumber.startsWith('+')
        ? PhoneNumber.parse(phoneNumber) // international already
        : PhoneNumber.parse(phoneNumber, callerCountry: isoCode);

    return parsed.international;
  } catch (e) {
    return phoneNumber.replaceAll(RegExp(r'[^0-9+]'), '');
  }
}

bool isSameNumber(String n1, String n2) {
  // equality check should be enough now but just in case
  return n1 == n2 || n1.endsWith(n2) || n2.endsWith(n1);
}

String convertSecondsToHMS(int totalSeconds) {
  int hours = totalSeconds ~/ 3600;
  int minutes = (totalSeconds % 3600) ~/ 60;
  int seconds = totalSeconds % 60;

  String result = '';
  if (hours > 0) {
    result += '$hours hour${hours > 1 ? 's' : ''} ';
  }
  if (minutes > 0) {
    result += '$minutes min${minutes > 1 ? 's' : ''} ';
  }
  if (seconds > 0 || result.isEmpty) {
    result += '$seconds sec${seconds > 1 ? 's' : ''}';
  }

  return result.trim();
}

Future<void> launchURL(String url) async {
  try {
    final Uri uri = Uri.parse(url);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } else {
      debugPrint('Cannot launch URL: $url');
      throw Exception('Could not launch $url');
    }
  } catch (e) {
    debugPrint('Error occurred: $e');
  }
}

bool isUSSDCode(String number) {
  return (number.startsWith('*') || number.startsWith('#')) &&
      number.endsWith('#');
}

bool isSecretCode(String number) {
  if (number == '*#06#') {
    return true;
  }
  if (number.startsWith('*#*#') && number.endsWith('#*#*')) {
    return true;
  }
  return false;
}

void handleSecretCode(String code) {
  String cleanCode = code;
  if (code.startsWith('*#*#') && code.endsWith('#*#*')) {
    cleanCode = code.substring(4, code.length - 4);
  } else if (code.startsWith('*#') && code.endsWith('#')) {
    cleanCode = code.substring(2, code.length - 1);
  }

  final intent = AndroidIntent(
    action: 'android.provider.Telephony.SecretCode.SECRET_CODE_ACTION',
    data: 'android_secret_code://$cleanCode',
  );

  try {
    intent.launch().catchError((e) {
      debugPrint("Failed to launch secret code: $e");
      return null;
    });
  } catch (e) {
    debugPrint("Error triggering secret code: $e");
  }
}

void hapticVibration() {
  if (SharedPrefService().getBool(PREF_DIALPAD_VIBRATION, def: true)) {
    HapticFeedback.lightImpact();
  }
}

String generateVCardString(Contact contact) {
  String str = '''
BEGIN:VCARD
VERSION:3.0
FN:${contact.name}''';

  for (var phone in contact.numbers) {
    str += 'TEL:$phone\n';
  }
  str += 'END:VCARD';
  return str;
}

Color colorFromContact(String id) {
  final colors = [
    Colors.red.shade300,
    Colors.blue.shade300,
    Colors.green.shade300,
    Colors.orange.shade300,
    Colors.purple.shade300,
    Colors.teal.shade300,
    Colors.amber.shade300,
  ];
  int hash = id.hashCode;
  return colors[hash % colors.length];
}

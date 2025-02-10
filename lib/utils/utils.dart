import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/services/prefservice.dart';
import 'package:url_launcher/url_launcher.dart';

String normalizePhoneNumber(String phoneNumber) {
  return phoneNumber.replaceAll(RegExp(r'[^0-9+]'), '');
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

void hapticVibration() {
  if (SharedPrefService().getBool(PREF_DIALPAD_VIBRATION, def: true)) {
    HapticFeedback.lightImpact();
  }
}

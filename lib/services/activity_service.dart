import 'dart:async';

import 'package:direct_caller_sim_choice/direct_caller_sim_choice.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:url_launcher/url_launcher.dart';

class ActivityService {
  ActivityService._internal();
  static final ActivityService _instance = ActivityService._internal();
  factory ActivityService() {
    return _instance;
  }

  Completer<void>? _permissionsCompleter;

  Future<void> requestPermissions() async {
    if (_permissionsCompleter == null) {
      _permissionsCompleter = Completer<void>();
      try {
        await [
          Permission.sms,
          Permission.phone,
          Permission.contacts,
          Permission.camera,
        ].request();
        _permissionsCompleter!.complete();
      } catch (e) {
        _permissionsCompleter!.completeError(e);
      } finally {
        _permissionsCompleter = null;
      }
    } else {
      await _permissionsCompleter!.future;
    }
  }

  Future<void> makePhoneCall(String phoneNumber, int simSlot) async {
    if (await Permission.phone.status.isGranted) {
      final DirectCaller directCaller = DirectCaller();
      directCaller.makePhoneCall(phoneNumber, simSlot: simSlot + 1);
    }
  }

  Future<void> sendSMS(String phoneNumber) async {
    if (await Permission.sms.status.isGranted) {
      final Uri smsUri = Uri(scheme: 'sms', path: phoneNumber);
      if (await canLaunchUrl(smsUri)) {
        await launchUrl(smsUri);
      } else {
        throw 'Could not send SMS.';
      }
    }
  }

  Future<void> makeVideoCall(String phoneNumber) async {
    if (await Permission.phone.status.isGranted) {
      final Uri videoCallUri = Uri(scheme: 'tel', path: phoneNumber);
      if (await canLaunchUrl(videoCallUri)) {
        await launchUrl(videoCallUri);
      } else {
        throw 'Could not start the video call.';
      }
    }
  }

  void openWhatsApp(String phoneNumber) async {
    final url = 'https://wa.me/$phoneNumber';
    if (await canLaunchUrl(Uri.parse(url))) {
      await launchUrl(Uri.parse(url));
    } else {
      throw 'Could not launch $url';
    }
  }

  void openTelegram(String phoneNumber) async {
    final url = 'https://t.me/$phoneNumber';
    if (await canLaunchUrl(Uri.parse(url))) {
      await launchUrl(Uri.parse(url));
    } else {
      throw 'Could not launch $url';
    }
  }
}

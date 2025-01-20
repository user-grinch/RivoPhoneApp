import 'dart:io';

import 'package:direct_caller_sim_choice/direct_caller_sim_choice.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:url_launcher/url_launcher.dart';

class ActivityService {
  ActivityService._internal();
  static final ActivityService _instance = ActivityService._internal();
  factory ActivityService() {
    return _instance;
  }

  Future<void> makePhoneCall(String phoneNumber, int simSlot) async {
    final DirectCaller directCaller = DirectCaller();
    directCaller.makePhoneCall(phoneNumber, simSlot: simSlot + 1);
  }

  Future<void> sendSMS(String phoneNumber) async {
    if (Platform.isAndroid) {
      await Permission.sms.request();
    }
    final Uri smsUri = Uri(scheme: 'sms', path: phoneNumber);
    if (await canLaunchUrl(smsUri)) {
      await launchUrl(smsUri);
    } else {
      throw 'Could not send SMS.';
    }
  }

  Future<void> makeVideoCall(String phoneNumber) async {
    final Uri videoCallUri = Uri(scheme: 'tel', path: phoneNumber);
    if (await canLaunchUrl(videoCallUri)) {
      await launchUrl(videoCallUri);
    } else {
      throw 'Could not start the video call.';
    }
  }
}

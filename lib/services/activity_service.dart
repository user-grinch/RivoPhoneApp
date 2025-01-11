import 'package:direct_caller_sim_choice/direct_caller_sim_choice.dart';
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

  // Future<void> makePhoneCall(String phoneNumber) async {
  //   final Uri phoneUri = Uri(scheme: 'tel', path: phoneNumber);
  //   if (await canLaunchUrl(phoneUri)) {
  //     await launchUrl(phoneUri);
  //   } else {
  //     throw 'Could not launch $phoneUri';
  //   }
  // }
}

import 'package:awesome_notifications/awesome_notifications.dart';
import 'package:flutter/material.dart';
import 'package:revo/constants/app_routes.dart';
import 'package:revo/controller/services/telephony_service.dart';
import 'package:revo/main.dart';

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();

  factory NotificationService() {
    return _instance;
  }

  NotificationService._internal();

  static const int incomingCallId = 10;
  static const int ongoingCallId = 11;

  void init() {
    AwesomeNotifications().initialize(
        null,
        [
          NotificationChannel(
            channelGroupKey: 'call_channel_group',
            channelKey: 'call_channel',
            channelName: 'Incoming Calls',
            channelDescription: 'Notification channel for ringing calls',
            defaultColor: const Color(0xFF1A73E8), // Google Dialer Blue
            ledColor: Colors.white,
            importance: NotificationImportance.Max,
            channelShowBadge: true,
            locked: true,
            defaultRingtoneType: DefaultRingtoneType.Ringtone,
            criticalAlerts: true,
            playSound: true,
            enableVibration: true,
            vibrationPattern: highVibrationPattern,
            defaultPrivacy: NotificationPrivacy.Public,
          ),
          NotificationChannel(
            channelGroupKey: 'call_channel_group',
            channelKey: 'ongoing_call_channel',
            channelName: 'Ongoing Calls',
            channelDescription: 'Notification channel for active calls',
            defaultColor: const Color(0xFF1A73E8),
            importance: NotificationImportance.High,
            locked: true,
            playSound: false,
            enableVibration: false,
            channelShowBadge: false,
          )
        ],
        channelGroups: [
          NotificationChannelGroup(
              channelGroupKey: 'call_channel_group',
              channelGroupName: 'Call Group')
        ],
        debug: true);

    AwesomeNotifications().setListeners(
      onActionReceivedMethod: NotificationService.onActionReceivedMethod,
    );
  }

  @pragma("vm:entry-point")
  static Future<void> onActionReceivedMethod(
      ReceivedAction receivedAction) async {
    final telephonyNotifier = gProvider.read(telephonyServiceProvider.notifier);

    if (receivedAction.channelKey == 'call_channel') {
      if (receivedAction.buttonKeyPressed == 'ACCEPT') {
        telephonyNotifier.acceptCall();
        gNavigatorKey.currentState?.pushNamed(AppRoutes.callScreenRoute);
      } else if (receivedAction.buttonKeyPressed == 'DECLINE') {
        telephonyNotifier.declineCall();
      } else {
        gNavigatorKey.currentState?.pushNamed(AppRoutes.callScreenRoute);
      }
    } else if (receivedAction.channelKey == 'ongoing_call_channel') {
      if (receivedAction.buttonKeyPressed == 'HANGUP') {
        telephonyNotifier.declineCall();
      } else {
        gNavigatorKey.currentState?.pushNamed(AppRoutes.callScreenRoute);
      }
    }
  }

  static Future<void> showIncomingCallNotification(
      String name, String number) async {
    await AwesomeNotifications().createNotification(
      content: NotificationContent(
        id: incomingCallId,
        channelKey: 'call_channel',
        title: name,
        body: '$number',
        category: NotificationCategory.Call,
        notificationLayout: NotificationLayout.Default,
        locked: true,
        wakeUpScreen: true,
        fullScreenIntent: true,
        autoDismissible: false,
        backgroundColor: Colors.white,
        displayOnForeground: true,
        displayOnBackground: true,
      ),
      actionButtons: [
        NotificationActionButton(
          key: 'DECLINE',
          label: 'Decline',
          color: const Color(0xFFD93025),
          autoDismissible: false,
          actionType: ActionType.SilentAction,
        ),
        NotificationActionButton(
          key: 'ACCEPT',
          label: 'Accept',
          color: const Color(0xFF188038),
          autoDismissible: false,
          actionType: ActionType.Default,
        ),
      ],
    );
  }

  static Future<void> showOngoingCallNotification(String name, String number,
      {String? duration}) async {
    await cancelNotification(incomingCallId);

    final durationText = duration != null ? ' • $duration' : '';

    await AwesomeNotifications().createNotification(
      content: NotificationContent(
        id: ongoingCallId,
        channelKey: 'ongoing_call_channel',
        title: 'Ongoing call',
        body: '<b>$name</b>$durationText',
        category: NotificationCategory.Status,
        notificationLayout: NotificationLayout.Default,
        locked: true,
        autoDismissible: false,
        backgroundColor: Colors.white,
        displayOnForeground: true,
        displayOnBackground: true,
      ),
      actionButtons: [
        NotificationActionButton(
          key: 'HANGUP',
          label: 'Hang up',
          color: const Color(0xFFD93025),
          autoDismissible: false,
          actionType: ActionType.SilentAction,
        ),
      ],
    );
  }

  static Future<void> cancelNotification(int id) async {
    await AwesomeNotifications().cancel(id);
  }

  static Future<void> cancelAllCallNotifications() async {
    await AwesomeNotifications().cancel(incomingCallId);
    await AwesomeNotifications().cancel(ongoingCallId);
  }
}

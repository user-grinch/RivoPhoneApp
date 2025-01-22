import 'package:flutter/material.dart';
import 'package:hugeicons/hugeicons.dart';

enum CallType {
  incoming,
  outgoing,
  missed,
  rejected,
  blocked,
  unknown,
}

extension CallTypeHelper on CallType {
  IconData getIcon() {
    switch (this) {
      case CallType.incoming:
        return HugeIcons.strokeRoundedCallReceived;
      case CallType.outgoing:
        return HugeIcons.strokeRoundedCallOutgoing01;
      case CallType.rejected:
        return HugeIcons.strokeRoundedCallDisabled02;
      case CallType.blocked:
        return HugeIcons.strokeRoundedCallBlocked;
      default:
        return HugeIcons.strokeRoundedCallMissed01;
    }
  }

  String getText() {
    switch (this) {
      case CallType.incoming:
        return 'Incoming';
      case CallType.outgoing:
        return 'Outgoing';
      case CallType.rejected:
        return 'Rejected';
      case CallType.missed:
        return 'Missed';
      case CallType.blocked:
        return 'Blocked';
      default:
        return '';
    }
  }

  Color getColor() {
    switch (this) {
      case CallType.incoming:
        return Colors.blue.withAlpha(200);
      case CallType.outgoing:
        return Colors.green.withAlpha(200);
      case CallType.rejected:
        return Colors.red.withAlpha(200);
      case CallType.missed:
        return Colors.red.withAlpha(200);
      case CallType.blocked:
        return Colors.grey.withAlpha(200);
      default:
        return Colors.white.withAlpha(200);
    }
  }
}

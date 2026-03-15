import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';

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
        return FluentIcons.call_inbound_24_filled;
      case CallType.outgoing:
        return FluentIcons.call_outbound_24_filled;
      case CallType.rejected:
        return FluentIcons.call_end_24_filled;
      case CallType.blocked:
        return FluentIcons.call_prohibited_24_filled;
      default:
        return FluentIcons.call_missed_24_filled;
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
        return 'None';
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

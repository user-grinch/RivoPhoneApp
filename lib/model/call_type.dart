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
        return Icons.call_received;
      case CallType.outgoing:
        return Icons.call_made;
      case CallType.rejected:
        return Icons.call_end;
      case CallType.blocked:
        return Icons.block;
      default:
        return Icons.call_missed;
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
        return Colors.red.withAlpha(140);
      case CallType.missed:
        return Colors.red.withAlpha(200);
      case CallType.blocked:
        return Colors.grey;
      default:
        return Colors.white;
    }
  }
}

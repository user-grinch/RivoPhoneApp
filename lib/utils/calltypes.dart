import 'package:flutter/material.dart';
import 'package:revo/modal/call_log_type.dart';

IconData getCallIcon(CallLogType type) {
  if (type == CallLogType.incoming) {
    return Icons.call_received;
  } else if (type == CallLogType.outgoing) {
    return Icons.call_made;
  } else if (type == CallLogType.rejected) {
    return Icons.call_end;
  } else if (type == CallLogType.blocked) {
    return Icons.block;
  }
  return Icons.call_missed;
}

String getCallText(CallLogType type) {
  if (type == CallLogType.incoming) {
    return 'Incoming';
  } else if (type == CallLogType.outgoing) {
    return 'Outgoing';
  } else if (type == CallLogType.rejected) {
    return 'Rejected';
  } else if (type == CallLogType.missed) {
    return 'Missed';
  } else if (type == CallLogType.blocked) {
    return 'Blocked';
  }
  return '';
}

Color getCallColor(CallLogType type, dynamic context) {
  if (type == CallLogType.incoming) {
    return Colors.blue.withAlpha(200);
  } else if (type == CallLogType.outgoing) {
    return Colors.green.withAlpha(200);
  } else if (type == CallLogType.rejected) {
    return Colors.red.withAlpha(140);
  } else if (type == CallLogType.missed) {
    return Colors.red.withAlpha(200);
  } else if (type == CallLogType.blocked) {
    return Colors.grey;
  }
  return context.colorScheme.primary;
}

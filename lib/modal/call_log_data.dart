import 'dart:typed_data';
import 'package:call_e_log/call_log.dart';
import 'package:revo/modal/call_log_type.dart';

String getDisplayName(entry) {
  if (entry.name != null && entry.name!.isNotEmpty) {
    return entry.name!;
  } else if (entry.number != null) {
    return entry.number!;
  } else {
    return 'Unknown';
  }
}

class CallLogData {
  final Uint8List? profile;
  final String? name;
  final String number;
  final String simDisplayName;
  final DateTime date;
  final String duration;
  final CallLogType type;

  CallLogData(this.profile,
      {required this.name,
      required this.number,
      required this.simDisplayName,
      required this.date,
      required this.duration,
      required this.type});

  factory CallLogData.fromEntry(
      {required CallLogEntry entry, Uint8List? profile}) {
    return CallLogData(
      profile,
      name: entry.name,
      number: entry.number ?? 'Unknown',
      simDisplayName: entry.simDisplayName ?? 'Unknown',
      date: DateTime.fromMillisecondsSinceEpoch(entry.timestamp ?? 0),
      duration: entry.duration.toString(),
      type: entry.callType == CallType.incoming
          ? CallLogType.incoming
          : entry.callType == CallType.outgoing
              ? CallLogType.outgoing
              : entry.callType == CallType.rejected
                  ? CallLogType.rejected
                  : entry.callType == CallType.blocked
                      ? CallLogType.blocked
                      : entry.callType == CallType.missed
                          ? CallLogType.missed
                          : CallLogType.unknown,
    );
  }
}

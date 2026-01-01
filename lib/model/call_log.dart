import 'dart:typed_data';
import 'package:call_log/call_log.dart' as lib;
import 'package:revo/model/call_type.dart';

class CallLog {
  final Uint8List? profile;
  final String name, simDisplayName;
  final String number;
  final DateTime date;
  final String duration;
  final CallType type;
  final String accountId;

  CallLog(
    this.profile, {
    required this.name,
    required this.number,
    required this.simDisplayName,
    required this.date,
    required this.duration,
    required this.type,
    required this.accountId,
  });

  factory CallLog.fromInternal({
    required lib.CallLogEntry entry,
    Uint8List? profile,
  }) {
    return CallLog(
      profile,
      name: entry.name ?? '',
      number: entry.number ?? '',
      simDisplayName: entry.simDisplayName ?? '',
      date: DateTime.fromMillisecondsSinceEpoch(entry.timestamp ?? 0),
      duration: entry.duration.toString(),
      type: _convertFromInternalType(entry.callType ?? lib.CallType.unknown),
      accountId: entry.phoneAccountId ?? '',
    );
  }

  static CallType _convertFromInternalType(lib.CallType type) {
    return type == lib.CallType.incoming
        ? CallType.incoming
        : type == lib.CallType.outgoing
            ? CallType.outgoing
            : type == lib.CallType.rejected
                ? CallType.rejected
                : type == lib.CallType.blocked
                    ? CallType.blocked
                    : type == lib.CallType.missed
                        ? CallType.missed
                        : CallType.unknown;
  }

  String get displayName {
    if (name.isNotEmpty) {
      return name;
    } else if (number.isNotEmpty) {
      return number;
    } else {
      return 'Unknown';
    }
  }
}

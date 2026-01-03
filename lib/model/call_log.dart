import 'dart:math';
import 'dart:typed_data';
import 'dart:ui';
import 'package:call_log/call_log.dart' as lib;
import 'package:flutter/material.dart';
import 'package:revo/controller/utils/utils.dart';
import 'package:revo/model/call_type.dart';
import 'package:phone_numbers_parser/phone_numbers_parser.dart' as pnp;

class CallLog {
  final Uint8List? profile;
  final String name, simDisplayName;
  final pnp.PhoneNumber number;
  final DateTime date;
  final String duration;
  final CallType type;
  final String accountId;
  final Color color;

  CallLog(
    this.profile, {
    required this.name,
    required this.number,
    required this.simDisplayName,
    required this.date,
    required this.duration,
    required this.type,
    required this.accountId,
    this.color = Colors.blueGrey,
  });

  factory CallLog.fromInternal({
    required lib.CallLogEntry entry,
    Uint8List? profile,
    String? countryCode,
    Color? col,
  }) {
    pnp.PhoneNumber phoneNumber;
    try {
      phoneNumber = pnp.PhoneNumber.parse(entry.number ?? '0',
          callerCountry: countryCode != null
              ? pnp.IsoCode.values.byName(countryCode.toUpperCase())
              : null);
    } catch (e) {
      phoneNumber = pnp.PhoneNumber.parse('0');
    }
    return CallLog(
      profile,
      name: entry.name ?? '',
      number: phoneNumber,
      simDisplayName: entry.simDisplayName ?? '',
      date: DateTime.fromMillisecondsSinceEpoch(entry.timestamp ?? 0),
      duration: entry.duration.toString(),
      type: _convertFromInternalType(entry.callType ?? lib.CallType.unknown),
      accountId: entry.phoneAccountId ?? '',
      color: col ?? colorFromContact(entry.name ?? phoneNumber.international),
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
}

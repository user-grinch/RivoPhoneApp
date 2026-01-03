import 'package:revo/controller/utils/utils.dart';
import 'package:revo/model/call_log.dart';
import 'package:revo/model/call_type.dart' show CallType;

class GroupedCallLog {
  final List<CallLog> logs;

  GroupedCallLog(this.logs);

  CallLog get latest => logs.first;
  int get count => logs.length;
}

List<GroupedCallLog> groupCallLogs(
  List<CallLog> logs,
  CallType filterType,
) {
  if (logs.isEmpty) return [];

  bool isSameDay(DateTime a, DateTime b) =>
      a.year == b.year && a.month == b.month && a.day == b.day;

  final filteredLogs = filterType == CallType.unknown
      ? logs
      : logs.where((l) => l.type == filterType).toList();

  if (filteredLogs.isEmpty) return [];

  final List<GroupedCallLog> grouped = [];
  List<CallLog> currentGroup = [filteredLogs.first];

  for (int i = 1; i < filteredLogs.length; i++) {
    final prev = filteredLogs[i - 1];
    final curr = filteredLogs[i];

    if (isSameDay(prev.date, curr.date) &&
        isSameNumber(
          prev.number.international,
          curr.number.international,
        )) {
      currentGroup.add(curr);
    } else {
      grouped.add(GroupedCallLog(currentGroup));
      currentGroup = [curr];
    }
  }

  grouped.add(GroupedCallLog(currentGroup));
  return grouped;
}

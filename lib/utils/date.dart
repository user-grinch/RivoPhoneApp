import 'package:intl/intl.dart';

String getContextAwareDate(DateTime date) {
  final now = DateTime.now();
  final today = DateTime(now.year, now.month, now.day);
  final yesterday = today.subtract(Duration(days: 1));
  final aWeekAgo = today.subtract(Duration(days: 7));

  if (date.isAfter(today)) {
    return 'Today';
  } else if (date.isAfter(yesterday)) {
    return 'Yesterday';
  } else if (date.isAfter(aWeekAgo)) {
    return DateFormat.EEEE().format(date);
  } else {
    return DateFormat('d MMM, y').format(date);
  }
}

String getContextAwareDateTime(DateTime date) {
  final now = DateTime.now();
  final today = DateTime(now.year, now.month, now.day);
  final yesterday = today.subtract(Duration(days: 1));
  final aWeekAgo = today.subtract(Duration(days: 7));

  if (date.isAfter(today)) {
    return '${DateFormat.jm().format(date)}, Today';
  } else if (date.isAfter(yesterday)) {
    return '${DateFormat.jm().format(date)}, Yesterday';
  } else if (date.isAfter(aWeekAgo)) {
    return '${DateFormat.jm().format(date)}, ${DateFormat.EEEE().format(date)}';
  } else {
    return '${DateFormat.jm().format(date)}, ${DateFormat('d MMM, y').format(date)}';
  }
}

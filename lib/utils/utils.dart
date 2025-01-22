String normalizePhoneNumber(String phoneNumber) {
  return phoneNumber.replaceAll(RegExp(r'[^0-9+]'), '');
}

String convertSecondsToHMS(int totalSeconds) {
  int hours = totalSeconds ~/ 3600;
  int minutes = (totalSeconds % 3600) ~/ 60;
  int seconds = totalSeconds % 60;

  String result = '';
  if (hours > 0) {
    result += '$hours hour${hours > 1 ? 's' : ''} ';
  }
  if (minutes > 0) {
    result += '$minutes min${minutes > 1 ? 's' : ''} ';
  }
  if (seconds > 0 || result.isEmpty) {
    result += '$seconds sec${seconds > 1 ? 's' : ''}';
  }

  return result.trim();
}

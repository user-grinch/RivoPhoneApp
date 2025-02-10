import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/services/prefservice.dart';

ThemeData getTheme(
  ColorScheme? dynamicCol,
  ThemeProvider provider,
  bool isDark,
) {
  ColorScheme defScheme = ColorScheme.fromSeed(
    seedColor: Colors.blue,
    brightness: isDark ? Brightness.dark : Brightness.light,
  );
  if (provider.isAmoled) {
    return _getAmoledTheme();
  }

  return ThemeData(
    colorScheme: provider.isDynamic ? dynamicCol ?? defScheme : defScheme,
    useMaterial3: true,
    textTheme: GoogleFonts.cabinTextTheme(),
  );
}

ThemeData _getAmoledTheme() {
  return ThemeData(
    brightness: Brightness.dark,
    scaffoldBackgroundColor: Colors.black,
    primaryColor: Colors.black,
    cardColor: Colors.black,
    dialogBackgroundColor: Colors.black,
    appBarTheme: AppBarTheme(
      backgroundColor: Colors.black,
      foregroundColor: Colors.white,
    ),
    colorScheme: ColorScheme.dark(
      primary: Colors.grey.shade800,
      onPrimary: Colors.white,
      primaryContainer: Colors.blueGrey.shade900,
      secondary: Colors.grey.shade800,
      onSecondary: Colors.white,
      secondaryContainer: Colors.blueGrey.shade900,
      onSecondaryContainer: Colors.white,
      background: Colors.black,
      surface: Colors.blueGrey.withAlpha(30),
    ),
    textTheme: GoogleFonts.cabinTextTheme(),
  );
}

class ThemeProvider extends ChangeNotifier {
  bool _isAmoled = false;
  bool _isDynamic = false;

  bool get isAmoled => _isAmoled;
  bool get isDynamic => _isDynamic;

  void initTheme() {
    SharedPrefService().init();
    _isAmoled = SharedPrefService().getBool(PREF_AMOLED_DARK_MODE, def: false);
    _isDynamic = SharedPrefService().getBool(PREF_MATERIAL_THEMING, def: false);
    notifyListeners();
  }

  void toggleDynamicColors() {
    _isDynamic = !_isDynamic;
    SharedPrefService().saveBool(PREF_MATERIAL_THEMING, _isDynamic);
    notifyListeners();
  }

  void toggleAmoledColors() {
    _isAmoled = !_isAmoled;
    SharedPrefService().saveBool(PREF_AMOLED_DARK_MODE, _isAmoled);
    notifyListeners();
  }
}

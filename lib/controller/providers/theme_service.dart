import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/controller/providers/pref_service.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'theme_service.g.dart';

@riverpod
class ThemeService extends _$ThemeService {
  @override
  ThemeState build() {
    return const ThemeState(isAmoled: false, isDynamic: false);
  }

  Future<void> initTheme() async {
    await SharedPrefService().init();
    final isAmoled =
        SharedPrefService().getBool(PREF_AMOLED_DARK_MODE, def: false);
    final isDynamic =
        SharedPrefService().getBool(PREF_MATERIAL_THEMING, def: false);

    state = state.copyWith(isAmoled: isAmoled, isDynamic: isDynamic);
  }

  void toggleDynamicColors() {
    state = state.copyWith(isDynamic: !state.isDynamic);
    SharedPrefService().saveBool(PREF_MATERIAL_THEMING, state.isDynamic);
  }

  void toggleAmoledColors() {
    state = state.copyWith(isAmoled: !state.isAmoled);
    SharedPrefService().saveBool(PREF_AMOLED_DARK_MODE, state.isAmoled);
  }
}

class ThemeState {
  final bool isAmoled;
  final bool isDynamic;

  const ThemeState({required this.isAmoled, required this.isDynamic});

  ThemeState copyWith({bool? isAmoled, bool? isDynamic}) {
    return ThemeState(
      isAmoled: isAmoled ?? this.isAmoled,
      isDynamic: isDynamic ?? this.isDynamic,
    );
  }
}

ThemeData getTheme(ColorScheme? dynamicCol, ThemeState state, bool isDark) {
  ColorScheme defScheme = ColorScheme.fromSeed(
    seedColor: Colors.blueAccent.shade100,
    brightness: isDark ? Brightness.dark : Brightness.light,
  );

  if (state.isAmoled) return _getAmoledTheme();

  return ThemeData(
    colorScheme: state.isDynamic ? dynamicCol ?? defScheme : defScheme,
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
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.black,
      foregroundColor: Colors.white,
    ),
    colorScheme: const ColorScheme.dark(
      primary: Color(0xFF000000),
      onPrimary: Color(0xFFFFFFFF),
      primaryContainer: Color(0xFF121212),
      secondary: Color(0xFF1C1C1C),
      onSecondary: Color(0xFFD3D3D3),
      secondaryContainer: Color(0xFF2B2B2B),
      onSecondaryContainer: Color(0xFFFFFFFF),
      surface: Color(0xFF121212),
      onSurface: Color(0xFFE0E0E0),
      surfaceContainer: Color(0xFF1A1A1A),
    ),
    textTheme: GoogleFonts.cabinTextTheme(),
  );
}

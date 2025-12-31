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
  final amoledScheme = const ColorScheme.dark(
    brightness: Brightness.dark,
    surface: Colors.black,
    onSurface: Color(0xFFE3E2E6),
    primary: Color(0xFFD0BCFF),
    onPrimary: Color(0xFF381E72),
    primaryContainer: Color(0xFF4F378B),
    onPrimaryContainer: Color(0xFFEADDFF),
    secondaryContainer: Color(0xFF2B2B2B),
    onSecondaryContainer: Color(0xFFFFFFFF),
    surfaceContainer: Color(0xFF121212),
    outlineVariant: Color(0xFF44474F),
  );

  return ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    scaffoldBackgroundColor: Colors.black,
    colorScheme: amoledScheme,
    textTheme: GoogleFonts.outfitTextTheme(ThemeData.dark().textTheme),
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: true,
      titleTextStyle: TextStyle(
        fontSize: 22,
        fontWeight: FontWeight.bold,
        color: Color(0xFFE3E2E6),
      ),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: Colors.black,
      indicatorColor: amoledScheme.primaryContainer.withOpacity(0.5),
      labelTextStyle: WidgetStateProperty.all(
        GoogleFonts.outfit(fontSize: 12, fontWeight: FontWeight.w500),
      ),
    ),
  );
}

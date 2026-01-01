import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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
    appBarTheme: AppBarTheme(
      systemOverlayStyle: SystemUiOverlayStyle(
        statusBarIconBrightness:
            (isDark && state.isAmoled) ? Brightness.light : Brightness.dark,
      ),
    ),
  );
}

ThemeData _getAmoledTheme() {
  final amoledScheme = const ColorScheme.dark(
    brightness: Brightness.dark,
    surface: Colors.black,
    onSurface: Color(0xFFC4C6D0),
    primary: Color(0xFF8E9199),
    onPrimary: Color(0xFF1B1B1F),
    primaryContainer: Color(0xFF33353B),
    onPrimaryContainer: Color(0xFFE2E2E6),
    secondaryContainer: Color(0xFF1E1F22),
    onSecondaryContainer: Color(0xFFE3E2E6),
    surfaceContainer: Color(0xFF0F1013),
    outline: Color(0xFF44474F),
    outlineVariant: Color(0xFF2E3036),
  );

  return ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    scaffoldBackgroundColor: Colors.black,
    colorScheme: amoledScheme,
    textTheme: GoogleFonts.outfitTextTheme(ThemeData.dark().textTheme),
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.black,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: true,
      systemOverlayStyle: SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.light,
        statusBarBrightness: Brightness.dark,
        systemNavigationBarColor: Colors.black,
        systemNavigationBarIconBrightness: Brightness.light,
      ),
      titleTextStyle: TextStyle(
        fontSize: 20,
        fontWeight: FontWeight.w600,
        color: Color(0xFFC4C6D0),
      ),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: Colors.black,
      height: 65,
      indicatorColor: amoledScheme.primaryContainer,
      indicatorShape:
          RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      iconTheme: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.selected)) {
          return IconThemeData(
              color: amoledScheme.onPrimaryContainer, size: 24);
        }
        return IconThemeData(
            color: amoledScheme.onSurface.withOpacity(0.5), size: 24);
      }),
      labelTextStyle: WidgetStateProperty.resolveWith((states) {
        final style =
            GoogleFonts.outfit(fontSize: 12, fontWeight: FontWeight.w500);
        if (states.contains(WidgetState.selected)) {
          return style.copyWith(color: amoledScheme.onSurface);
        }
        return style.copyWith(color: amoledScheme.onSurface.withOpacity(0.5));
      }),
    ),
  );
}

import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/controller/providers/pref_service.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/controller/providers/theme_service.dart';
import 'package:revo/view/screen/contactinfo_view.dart';
import 'package:revo/view/screen/dialpad/dialpad.dart';
import 'package:revo/view/screen/history_view.dart';
import 'package:revo/view/screen/landing/landing.dart';
import 'package:revo/view/screen/search_view.dart';
import 'package:revo/view/screen/settings/settings.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SharedPrefService().init();

  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      systemNavigationBarColor: Colors.transparent));

  runApp(const ProviderScope(child: MyApp()));
}

class MyApp extends ConsumerWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeState = ref.watch(themeServiceProvider);

    return DynamicColorBuilder(
      builder: (ColorScheme? lightDynamic, ColorScheme? darkDynamic) {
        return MaterialApp(
          debugShowCheckedModeBanner: false,
          theme: getTheme(lightDynamic, themeState, false),
          darkTheme: getTheme(darkDynamic, themeState, true),
          themeMode: ThemeMode.system,
          initialRoute: homeRoute,
          routes: {
            homeRoute: (context) => const HomeView(),
            settingsRoute: (context) => const SettingsView(),
            searchRoute: (context) => const SearchView(),
            dialpadRoute: (context) => const DialPadView(),
            contactInfoRoute: (context) => ContactInfoView(
                ModalRoute.of(context)!.settings.arguments as Contact),
            callHistoryRoute: (context) => HistoryView(
                numbers:
                    ModalRoute.of(context)!.settings.arguments as List<String>),
          },
        );
      },
    );
  }
}

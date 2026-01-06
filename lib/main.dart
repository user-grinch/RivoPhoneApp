import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/controller/services/pref_service.dart';
import 'package:revo/controller/services/telephony_service.dart';
import 'package:revo/controller/services/theme_service.dart';
import 'package:revo/router/router.dart';

final ProviderContainer gProvider = ProviderContainer();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  SharedPrefService().init();

  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      systemNavigationBarColor: Colors.transparent));

  gProvider.read(telephonyServiceProvider);
  runApp(UncontrolledProviderScope(
    container: gProvider,
    child: MyApp(),
  ));
}

class MyApp extends ConsumerWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeState = ref.watch(themeServiceProvider);

    return DynamicColorBuilder(
      builder: (ColorScheme? lightDynamic, ColorScheme? darkDynamic) {
        return MaterialApp.router(
          debugShowCheckedModeBanner: false,
          theme: withM3ETheme(getTheme(lightDynamic, themeState, false)),
          darkTheme: withM3ETheme(getTheme(darkDynamic, themeState, true)),
          themeMode: ThemeMode.system,
          routerConfig: router,
        );
      },
    );
  }
}

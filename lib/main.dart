import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/constants/app_routes.dart';
import 'package:revo/controller/services/pref_service.dart';
import 'package:revo/controller/services/telephony_service.dart';
import 'package:revo/controller/services/theme_service.dart';
import 'package:revo/model/call_state.dart';
import 'package:revo/router/router.dart';

final ProviderContainer gProvider = ProviderContainer();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SharedPrefService().init();

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

class MyApp extends ConsumerStatefulWidget {
  const MyApp({super.key});

  @override
  ConsumerState<MyApp> createState() => _MyAppState();
}

class _MyAppState extends ConsumerState<MyApp> {
  @override
  void initState() {
    super.initState();
  }

  Widget build(BuildContext context) {
    final themeState = ref.watch(themeServiceProvider);

    ref.listen<CallState>(telephonyServiceProvider, (previous, next) {
      final String? routeName =
          gRouter.routerDelegate.currentConfiguration.last.matchedLocation;
      if (previous == next || routeName == AppRoutes.callScreenRoute) {
        return;
      }
      if (next == CallState.disconnected) {
        gRouter.pushNamed(AppRoutes.homeRoute);
      } else {
        gRouter.pushNamed(AppRoutes.callScreenRoute);
      }
    });

    return DynamicColorBuilder(
      builder: (ColorScheme? lightDynamic, ColorScheme? darkDynamic) {
        return MaterialApp.router(
          debugShowCheckedModeBanner: false,
          theme: withM3ETheme(getTheme(lightDynamic, themeState, false)),
          darkTheme: withM3ETheme(getTheme(darkDynamic, themeState, true)),
          themeMode: ThemeMode.system,
          routerConfig: gRouter,
        );
      },
    );
  }
}

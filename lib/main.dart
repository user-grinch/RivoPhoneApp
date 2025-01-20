import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/services/activity_service.dart';
import 'package:revo/services/cubit/call_log_service.dart';
import 'package:revo/services/cubit/contact_service.dart';
import 'package:revo/services/cubit/mobile_service.dart';
import 'package:revo/ui/views/call_screen.dart';
import 'package:revo/ui/views/contactinfo_view.dart';
import 'package:revo/ui/views/dialpad_view.dart';
import 'package:revo/ui/views/history_view.dart';
import 'package:revo/ui/views/home_view.dart';
import 'package:revo/ui/views/qr_scanner_view.dart';
import 'package:revo/ui/views/qr_view.dart';
import 'package:revo/ui/views/search_view.dart';
import 'package:revo/ui/views/settings_view.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      systemNavigationBarColor: Colors.transparent));

  runApp(DynamicColorBuilder(builder: (
    ColorScheme? lightDynamic,
    ColorScheme? darkDynamic,
  ) {
    final lightColorScheme =
        lightDynamic ?? ColorScheme.fromSeed(seedColor: Colors.blue);
    final darkColorScheme = darkDynamic ??
        ColorScheme.fromSeed(
            seedColor: Colors.blue, brightness: Brightness.dark);
    return MultiProvider(
      providers: [
        BlocProvider(
          create: (context) => CallLogService(),
          lazy: false,
        ),
        BlocProvider(
          create: (context) => ContactService(),
          lazy: false,
        ),
        BlocProvider(
          create: (context) => MobileService(),
          lazy: false,
        ),
      ],
      child: MaterialApp(
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorScheme: lightColorScheme,
          useMaterial3: true,
          textTheme: GoogleFonts.cabinTextTheme(),
          pageTransitionsTheme: const PageTransitionsTheme(
            builders: <TargetPlatform, PageTransitionsBuilder>{
              TargetPlatform.android: PredictiveBackPageTransitionsBuilder(),
            },
          ),
        ),
        darkTheme: ThemeData(
          colorScheme: darkColorScheme,
          useMaterial3: true,
          textTheme: GoogleFonts.cabinTextTheme(),
          pageTransitionsTheme: const PageTransitionsTheme(
            builders: <TargetPlatform, PageTransitionsBuilder>{
              TargetPlatform.android: PredictiveBackPageTransitionsBuilder(),
            },
          ),
        ),
        themeMode: ThemeMode.system,
        initialRoute: homeRoute,
        routes: {
          homeRoute: (context) => HomeView(),
          settingsRoute: (context) => SettingsView(),
          searchRoute: (context) => SearchView(),
          dialpadRoute: (context) => DialPadView(),
          qrShareRoute: (context) => QRCodePopup(
              data: ModalRoute.of(context)!.settings.arguments as String),
          callScreenRoute: (context) => CallScreenView(),
          contactInfoRoute: (context) => ContactInfoView(
              ModalRoute.of(context)!.settings.arguments as Contact),
          qrScanRoute: (context) => QRScannerView(),
          callHistoryRoute: (context) => HistoryView(
              numbers:
                  ModalRoute.of(context)!.settings.arguments as List<String>),
        },
      ),
    );
  }));
}

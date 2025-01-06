import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_contacts/contact.dart';
import 'package:flutter_contacts/flutter_contacts.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/ui/dialpad_view.dart';
import 'package:revo/ui/history_view.dart';
import 'package:revo/ui/home_view.dart';
import 'package:revo/ui/qr_scanner_view.dart';
import 'package:revo/ui/qr_view.dart';
import 'package:revo/ui/search_view.dart';
import 'package:revo/ui/settings_view.dart';

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
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: lightColorScheme,
        useMaterial3: true,
        textTheme: GoogleFonts.cabinTextTheme(),
      ),
      darkTheme: ThemeData(
        colorScheme: darkColorScheme,
        useMaterial3: true,
        textTheme: GoogleFonts.cabinTextTheme(),
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
        qrScanRoute: (context) => QRScannerView(),
        callHistoryRoute: (context) => HistoryView(
            numbers: ModalRoute.of(context)!.settings.arguments as List<Phone>),
      },
    );
  }));
}

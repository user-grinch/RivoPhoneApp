import 'package:ai_barcode_scanner/ai_barcode_scanner.dart';
import 'package:dynamic_color/dynamic_color.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:phone_numbers_parser/phone_numbers_parser.dart';
import 'package:revo/constants/app_routes.dart';
import 'package:revo/controller/services/contact_service.dart';
import 'package:revo/controller/services/pref_service.dart';
import 'package:revo/controller/services/telephony_service.dart';
import 'package:revo/controller/services/theme_service.dart';
import 'package:revo/model/call_state.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/view/screen/call_screen.dart';
import 'package:revo/view/screen/contactinfo_view.dart';
import 'package:revo/view/screen/def_dialer.dart';
import 'package:revo/view/screen/dialpad/dialpad.dart';
import 'package:revo/view/screen/history_view.dart';
import 'package:revo/view/screen/landing/landing.dart';
import 'package:revo/view/screen/search_view.dart';
import 'package:revo/view/screen/settings/about.dart';
import 'package:revo/view/screen/settings/call.dart';
import 'package:revo/view/screen/settings/contributors.dart';
import 'package:revo/view/screen/settings/settings.dart';
import 'package:revo/view/screen/settings/sound.dart';
import 'package:revo/view/screen/settings/user_interface.dart';

final ProviderContainer gProvider = ProviderContainer();
final GlobalKey<NavigatorState> gNavigatorKey = GlobalKey<NavigatorState>();

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
      if (previous == next) return;

      String? currentRoute;
      gNavigatorKey.currentState?.popUntil((route) {
        currentRoute = route.settings.name;
        return true;
      });

      if (next != CallState.disconnected &&
          currentRoute != AppRoutes.callScreenRoute) {
        gNavigatorKey.currentState?.pushNamed(AppRoutes.callScreenRoute);
      } else if (next == CallState.disconnected &&
          currentRoute == AppRoutes.callScreenRoute) {
        gNavigatorKey.currentState?.pop();
      }
    });

    return DynamicColorBuilder(
      builder: (ColorScheme? lightDynamic, ColorScheme? darkDynamic) {
        return MaterialApp(
          navigatorKey: gNavigatorKey,
          debugShowCheckedModeBanner: false,
          theme: withM3ETheme(getTheme(lightDynamic, themeState, false)),
          darkTheme: withM3ETheme(getTheme(darkDynamic, themeState, true)),
          themeMode: ThemeMode.system,
          initialRoute: AppRoutes.homeRoute,
          routes: {
            AppRoutes.homeRoute: (context) => const LandingScreen(),
            AppRoutes.setDefDialerRoute: (context) => const SetDefDialer(),
            AppRoutes.dialpadRoute: (context) => const DialPadView(),
            AppRoutes.searchRoute: (context) => const SearchView(),

            // Screens with arguments
            AppRoutes.contactInfoRoute: (context) {
              final contact =
                  ModalRoute.of(context)!.settings.arguments as Contact;
              return ContactInfoView(contact: contact);
            },

            AppRoutes.callHistoryRoute: (context) {
              final numbers = ModalRoute.of(context)!.settings.arguments
                  as List<PhoneNumber>;
              return HistoryView(numbers: numbers);
            },

            AppRoutes.callScreenRoute: (context) => const CallScreen(),

            AppRoutes.qrScanRoute: (context) {
              final ref = ProviderScope.containerOf(context)
                  .read(contactServiceProvider.notifier);
              return AiBarcodeScanner(
                cameraSwitchIcon: FluentIcons.camera_switch_24_regular,
                flashOnIcon: FluentIcons.flash_24_regular,
                flashOffIcon: FluentIcons.flash_off_24_regular,
                galleryIcon: FluentIcons.image_24_regular,
                galleryButtonText: "Gallery",
                galleryButtonType: GalleryButtonType.filled,
                controller: MobileScannerController(
                    detectionSpeed: DetectionSpeed.normal),
                validator: (capture) {
                  final raw = capture.barcodes.first.rawValue;
                  return raw != null && raw.contains("BEGIN:VCARD");
                },
                onDetect: (capture) async {
                  final res = capture.barcodes.first.rawValue;
                  if (res != null) {
                    await ref.insertContactFromVCard(res);
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                            content: Text('Contact added successfully!')),
                      );
                      // Standard Navigator pop instead of go_router context.pop()
                      Navigator.pop(context);
                    }
                  }
                },
              );
            },

            // Settings Routes
            AppRoutes.settingsRoute: (context) => const SettingsScreen(),
            AppRoutes.callSettingsRoute: (context) => const CallSettings(),
            AppRoutes.soundSettingsRoute: (context) => const SoundView(),
            AppRoutes.uiSettingsRoute: (context) => const UserInterfaceView(),
            AppRoutes.aboutRoute: (context) => const AboutScreen(),
            AppRoutes.contributorsRoute: (context) => const ContributorsView(),
          },
        );
      },
    );
  }
}

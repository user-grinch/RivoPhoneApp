import 'package:revo/controller/services/telephony_service.dart';
import 'package:revo/model/contact.dart';
import 'package:ai_barcode_scanner/ai_barcode_scanner.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:phone_numbers_parser/phone_numbers_parser.dart';
import 'package:revo/controller/services/contact_service.dart';
import 'package:revo/constants/app_routes.dart';
import 'package:revo/view/screen/call-screen.dart';
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

final GoRouter gRouter = GoRouter(
  routes: [
    GoRoute(
      name: AppRoutes.setDefDialerRoute,
      path: '/set-def-dialer',
      builder: (context, state) {
        return const SetDefDialer();
      },
    ),
    GoRoute(
      name: AppRoutes.homeRoute,
      path: '/',
      builder: (context, state) => const LandingScreen(),
    ),
    GoRoute(
      name: AppRoutes.dialpadRoute,
      path: '/dialpad',
      builder: (context, state) => const DialPadView(),
    ),
    GoRoute(
      name: AppRoutes.searchRoute,
      path: '/search',
      builder: (context, state) => const SearchView(),
    ),
    GoRoute(
      name: AppRoutes.contactInfoRoute,
      path: '/contact-info',
      builder: (context, state) =>
          ContactInfoView(contact: state.extra as Contact),
    ),
    GoRoute(
      name: AppRoutes.callHistoryRoute,
      path: '/call-history',
      builder: (context, state) =>
          HistoryView(numbers: state.extra as List<PhoneNumber>),
    ),
    GoRoute(
      name: AppRoutes.callScreenRoute,
      path: '/call-screen',
      builder: (context, state) => CallScreen(),
    ),
    GoRoute(
      name: AppRoutes.qrScanRoute,
      path: '/qr-scan',
      builder: (context, state) {
        final ref = ProviderScope.containerOf(context)
            .read(contactServiceProvider.notifier);
        return AiBarcodeScanner(
          cameraSwitchIcon: FluentIcons.camera_switch_24_regular,
          flashOnIcon: FluentIcons.flash_24_regular,
          flashOffIcon: FluentIcons.flash_off_24_regular,
          galleryIcon: FluentIcons.image_24_regular,
          galleryButtonText: "Gallery",
          galleryButtonType: GalleryButtonType.filled,
          controller:
              MobileScannerController(detectionSpeed: DetectionSpeed.normal),
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
                  const SnackBar(content: Text('Contact added successfully!')),
                );
              }
            }
          },
        );
      },
    ),
    GoRoute(
      name: AppRoutes.settingsRoute,
      path: '/settings',
      builder: (context, state) => const SettingsScreen(),
    ),
    GoRoute(
      name: AppRoutes.callSettingsRoute,
      path: '/settings-call',
      builder: (context, state) => const CallSettings(),
    ),
    GoRoute(
      name: AppRoutes.soundSettingsRoute,
      path: '/settings-sound',
      builder: (context, state) => const SoundView(),
    ),
    GoRoute(
      name: AppRoutes.uiSettingsRoute,
      path: '/settings-ui',
      builder: (context, state) => const UserInterfaceView(),
    ),
    GoRoute(
      name: AppRoutes.aboutRoute,
      path: '/settings-about',
      builder: (context, state) => const AboutScreen(),
    ),
    GoRoute(
      name: AppRoutes.contributorsRoute,
      path: '/settings-contributors',
      builder: (context, state) => const ContributorsView(),
    ),
  ],
);

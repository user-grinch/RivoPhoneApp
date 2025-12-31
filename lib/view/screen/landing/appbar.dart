import 'package:ai_barcode_scanner/ai_barcode_scanner.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';

class AppBarView extends ConsumerWidget implements PreferredSizeWidget {
  @override
  final Size preferredSize;

  const AppBarView({super.key})
      : preferredSize = const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return AppBar(
      title: InkWell(
        onTap: () {
          Navigator.pushNamed(context, searchRoute);
        },
        borderRadius: BorderRadius.circular(50),
        splashColor: context.colorScheme.secondaryContainer,
        child: Container(
          decoration: BoxDecoration(
            color: context.colorScheme.secondaryContainer.withAlpha(200),
            borderRadius: BorderRadius.circular(50),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              IconButton(
                onPressed: () async {
                  await Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (context) => AiBarcodeScanner(
                        cameraSwitchIcon: FluentIcons.camera_switch_24_regular,
                        flashOnIcon: FluentIcons.flash_24_regular,
                        flashOffIcon: FluentIcons.flash_off_24_regular,
                        galleryIcon: FluentIcons.image_24_regular,
                        galleryButtonText: "Gallery",
                        galleryButtonType: GalleryButtonType.filled,
                        controller: MobileScannerController(
                          detectionSpeed: DetectionSpeed.normal,
                        ),
                        validator: (capture) {
                          String? res = capture.barcodes.first.rawValue;
                          return (res != null &&
                              res.startsWith("BEGIN:VCARD") &&
                              res.endsWith("END:VCARD"));
                        },
                        onDetect: (BarcodeCapture capture) async {
                          String? res = capture.barcodes.first.rawValue;
                          await ref
                              .read(contactServiceProvider.notifier)
                              .insertContactFromVCard(res!);
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                                content: Text('Contact added successfully!')),
                          );
                        },
                      ),
                    ),
                  );
                },
                icon: Icon(
                  FluentIcons.qr_code_24_regular,
                ),
              ),
              Text(
                'Search in Rivo',
                style: GoogleFonts.outfit(
                  fontSize: 20,
                  color: context.colorScheme.onSurface,
                ),
              ),
              IconButton(
                onPressed: () {
                  Navigator.pushNamed(context, settingsRoute);
                },
                icon: Icon(
                  FluentIcons.options_24_regular,
                ),
              ),
            ],
          ),
        ),
      ),
      centerTitle: true,
    );
  }
}

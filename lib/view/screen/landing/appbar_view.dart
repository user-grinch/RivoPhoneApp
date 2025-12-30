import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:simple_barcode_scanner/simple_barcode_scanner.dart';

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
                  // Navigator.pushNamed(context, qrScanRoute);
                  String? res = await SimpleBarcodeScanner.scanBarcode(
                        context,
                        barcodeAppBar: const BarcodeAppBar(
                          appBarTitle: 'Scan QR to add contact',
                          centerTitle: false,
                          enableBackButton: true,
                          backButtonIcon:
                              Icon(FluentIcons.arrow_left_24_regular),
                        ),
                        scanType: ScanType.qr,
                        isShowFlashIcon: true,
                        delayMillis: 1000,
                      ) ??
                      "";

                  if (res.startsWith("BEGIN:VCARD") &&
                      res.endsWith("END:VCARD")) {
                    await ref
                        .read(contactServiceProvider.notifier)
                        .insertContactFromVCard(res);
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                          content: Text('Contact added successfully!')),
                    );
                  } else {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Invalid vCard format!')),
                    );
                  }
                },
                icon: Icon(
                  FluentIcons.qr_code_24_regular,
                ),
              ),
              Text(
                'Search in Rivo',
                style: GoogleFonts.raleway(
                  fontSize: 20,
                  color: context.colorScheme.onSurface,
                ),
              ),
              IconButton(
                onPressed: () {
                  Navigator.pushNamed(context, settingsRoute);
                },
                icon: Icon(
                  FluentIcons.settings_24_regular,
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

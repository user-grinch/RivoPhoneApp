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

  const AppBarView({super.key}) : preferredSize = const Size.fromHeight(60);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final colorScheme = context.colorScheme;

    return AppBar(
      backgroundColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: true,
      toolbarHeight: 50,
      title: GestureDetector(
        onTap: () => Navigator.pushNamed(context, searchRoute),
        child: Container(
          height: 50,
          decoration: BoxDecoration(
            color: colorScheme.secondaryContainer.withOpacity(0.4),
            borderRadius: BorderRadius.circular(28),
          ),
          child: Row(
            children: [
              _buildIconButton(
                icon: FluentIcons.qr_code_24_regular,
                onPressed: () => _openScanner(context, ref),
                color: colorScheme.primary,
              ),
              Expanded(
                child: Text(
                  'Search in Rivo',
                  textAlign: TextAlign.center,
                  style: GoogleFonts.outfit(
                    fontSize: 18,
                    fontWeight: FontWeight.w500,
                    color: colorScheme.onSurfaceVariant.withOpacity(0.7),
                  ),
                ),
              ),
              _buildIconButton(
                icon: FluentIcons.options_24_regular,
                onPressed: () => Navigator.pushNamed(context, settingsRoute),
                color: colorScheme.primary,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildIconButton({
    required IconData icon,
    required VoidCallback onPressed,
    required Color color,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(20),
      ),
      child: IconButton(
        onPressed: onPressed,
        icon: Icon(icon, color: color, size: 24),
      ),
    );
  }

  void _openScanner(BuildContext context, WidgetRef ref) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => AiBarcodeScanner(
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
              await ref
                  .read(contactServiceProvider.notifier)
                  .insertContactFromVCard(res);
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Contact added successfully!')),
                );
              }
            }
          },
        ),
      ),
    );
  }
}

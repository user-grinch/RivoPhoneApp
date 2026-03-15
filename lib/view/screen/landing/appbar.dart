import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';

import 'package:revo/constants/app_routes.dart';

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
        onTap: () => Navigator.of(context).pushNamed(AppRoutes.searchRoute),
        child: Container(
          height: 50,
          decoration: BoxDecoration(
            color: colorScheme.secondaryContainer.withOpacity(0.4),
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            children: [
              _buildIconButton(
                icon: FluentIcons.qr_code_24_regular,
                onPressed: () =>
                    Navigator.of(context).pushNamed(AppRoutes.qrScanRoute),
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
                onPressed: () =>
                    Navigator.of(context).pushNamed(AppRoutes.settingsRoute),
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
}

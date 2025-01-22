import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/extentions/theme.dart';

class AppBarView extends StatelessWidget implements PreferredSizeWidget {
  @override
  final Size preferredSize;

  const AppBarView({super.key})
      : preferredSize = const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      title: InkWell(
        onTap: () {
          Navigator.pushNamed(context, searchRoute);
        },
        borderRadius: BorderRadius.circular(50),
        splashColor: context.colorScheme.surfaceTint.withAlpha(50),
        child: Container(
          decoration: BoxDecoration(
            color: context.colorScheme.surfaceTint.withAlpha(30),
            borderRadius: BorderRadius.circular(50),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              IconButton(
                onPressed: () {
                  Navigator.pushNamed(context, qrScanRoute);
                },
                icon: Icon(
                  HugeIcons.strokeRoundedQrCode,
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
                  HugeIcons.strokeRoundedSettings03,
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

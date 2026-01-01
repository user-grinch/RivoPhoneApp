import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/view/components/rounded_icon_btn.dart';

class AppBarComponent extends StatelessWidget implements PreferredSizeWidget {
  final String title;
  final List<Widget>? actions;

  const AppBarComponent(
    this.title, {
    super.key,
    this.actions,
  });

  @override
  Widget build(BuildContext context) {
    return AppBar(
      centerTitle: true,
      backgroundColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      leadingWidth: 72,
      leading: Center(
        child: RoundedIconButton(
          FluentIcons.arrow_left_24_regular,
          size: 40,
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      title: Text(
        title,
        style: GoogleFonts.outfit(
          fontSize: 20,
          fontWeight: FontWeight.w600,
          color: context.colorScheme.onSurface,
        ),
      ),
      actions: actions,
    );
  }

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);
}

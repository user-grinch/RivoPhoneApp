import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/extensions/theme.dart';
import 'package:revo/services/prefservice.dart';
import 'package:revo/ui/theme/handler.dart';
import 'package:revo/utils/switch_tile.dart';

class UserInterfaceView extends StatefulWidget {
  const UserInterfaceView({super.key});

  @override
  State<UserInterfaceView> createState() => _UserInterfaceViewState();
}

class _UserInterfaceViewState extends State<UserInterfaceView> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: Icon(HugeIcons.strokeRoundedArrowLeft01),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text(
          'User Interface',
          style: GoogleFonts.raleway(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: context.colorScheme.onSurface,
          ),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0),
        children: [
          SwitchTileWidget(
              title: "Material You theming",
              subtitle:
                  "Wallpaper based app color theming. Restart is required.",
              value: context.read<ThemeProvider>().isDynamic,
              onChanged: (value) {
                setState(() {
                  context.read<ThemeProvider>().toggleDynamicColors();
                  setState(() {});
                });
              },
              isFirst: true),
          SwitchTileWidget(
              title: "Amoled dark mode",
              subtitle:
                  "Uses pitch black for UI elements. This may save some battery life on OLED screens.",
              value: context.read<ThemeProvider>().isAmoled,
              onChanged: (value) {
                context.read<ThemeProvider>().toggleAmoledColors();
                setState(() {});
              },
              isLast: true),
          const SizedBox(
            height: 10,
          ),
          SwitchTileWidget(
              title: "Show first letter in avartar",
              subtitle:
                  "Displays the first letter of the contact name when a profile picture isn't available",
              value: SharedPrefService().getBool(PREF_SHOW_FIRST_LETTER),
              onChanged: (value) {
                SharedPrefService().saveBool(PREF_SHOW_FIRST_LETTER, value);
                setState(() {});
              },
              isFirst: true),
          SwitchTileWidget(
              title: "Show picture in avatar",
              subtitle: "Shows the contact picture if available",
              value: SharedPrefService().getBool(PREF_SHOW_PICTURE_IN_AVARTAR),
              onChanged: (value) {
                SharedPrefService()
                    .saveBool(PREF_SHOW_PICTURE_IN_AVARTAR, value);
                setState(() {});
              },
              isLast: true),
          const SizedBox(
            height: 10,
          ),
          SwitchTileWidget(
            title: "Icon-only bottom sheet",
            subtitle:
                "Only shows navigation icons in the bottom navigation bar",
            value: SharedPrefService().getBool(PREF_ICON_ONLY_BOTTOMSHEET),
            onChanged: (value) {
              SharedPrefService().saveBool(PREF_ICON_ONLY_BOTTOMSHEET, value);
              setState(() {});
            },
            isFirst: true,
          ),
          SwitchTileWidget(
            title: "Selected icon in bottom sheet",
            subtitle: "Always shows the icon for the selected tab",
            value: SharedPrefService()
                .getBool(PREF_ALWAYS_SHOW_SELECTED_IN_BOTTOMSHEET),
            onChanged: (value) {
              SharedPrefService()
                  .saveBool(PREF_ALWAYS_SHOW_SELECTED_IN_BOTTOMSHEET, value);
              setState(() {});
            },
            isLast: true,
          ),
          const SizedBox(
            height: 10,
          ),
          SwitchTileWidget(
            title: "Dialpad letters",
            subtitle: "Show letters on the dialpad buttons",
            value: SharedPrefService().getBool(PREF_DIALPAD_LETTERS),
            onChanged: (value) {
              SharedPrefService().saveBool(PREF_DIALPAD_LETTERS, value);
              setState(() {});
            },
            isFirst: true,
            isLast: true,
          ),
        ],
      ),
    );
  }
}

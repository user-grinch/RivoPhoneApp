import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/pref_service.dart';
import 'package:revo/controller/providers/theme_service.dart';
import 'package:revo/view/utils/switch_tile.dart';

class UserInterfaceView extends ConsumerWidget {
  const UserInterfaceView({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeState = ref.watch(themeServiceProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(FluentIcons.arrow_left_24_regular),
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
            subtitle: "Wallpaper based app color theming. Restart is required.",
            value: themeState.isDynamic,
            onChanged: (value) =>
                ref.read(themeServiceProvider.notifier).toggleDynamicColors(),
            isFirst: true,
          ),
          SwitchTileWidget(
            title: "Amoled dark mode",
            subtitle:
                "Uses pitch black for UI elements. This may save some battery life on OLED screens.",
            value: themeState.isAmoled,
            onChanged: (value) =>
                ref.read(themeServiceProvider.notifier).toggleAmoledColors(),
            isLast: true,
          ),
          const SizedBox(height: 10),
          SwitchTileWidget(
            title: "Show first letter in avatar",
            subtitle:
                "Displays the first letter of the contact name when a profile picture isn't available",
            value: SharedPrefService().getBool(PREF_SHOW_FIRST_LETTER),
            onChanged: (value) {
              SharedPrefService().saveBool(PREF_SHOW_FIRST_LETTER, value);
            },
            isFirst: true,
          ),
          SwitchTileWidget(
            title: "Show picture in avatar",
            subtitle: "Shows the contact picture if available",
            value: SharedPrefService().getBool(PREF_SHOW_PICTURE_IN_AVATAR),
            onChanged: (value) {
              SharedPrefService().saveBool(PREF_SHOW_PICTURE_IN_AVATAR, value);
            },
            isLast: true,
          ),
          const SizedBox(height: 10),
          SwitchTileWidget(
            title: "Icon-only bottom sheet",
            subtitle:
                "Only shows navigation icons in the bottom navigation bar",
            value: SharedPrefService().getBool(PREF_ICON_ONLY_BOTTOMSHEET),
            onChanged: (value) {
              SharedPrefService().saveBool(PREF_ICON_ONLY_BOTTOMSHEET, value);
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
            },
            isLast: true,
          ),
          const SizedBox(height: 10),
          SwitchTileWidget(
            title: "Dialpad letters",
            subtitle: "Show letters on the dialpad buttons",
            value: SharedPrefService().getBool(PREF_DIALPAD_LETTERS),
            onChanged: (value) {
              SharedPrefService().saveBool(PREF_DIALPAD_LETTERS, value);
            },
            isFirst: true,
            isLast: true,
          ),
        ],
      ),
    );
  }
}

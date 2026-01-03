import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/controller/providers/pref_service.dart';
import 'package:revo/controller/providers/theme_service.dart';
import 'package:revo/view/components/switch_tile.dart';
import 'package:revo/view/screen/settings/appbarcomponent.dart';

class UserInterfaceView extends ConsumerStatefulWidget {
  const UserInterfaceView({super.key});

  @override
  ConsumerState<UserInterfaceView> createState() => _UserInterfaceView();
}

class _UserInterfaceView extends ConsumerState<UserInterfaceView> {
  @override
  Widget build(BuildContext context) {
    final themeState = ref.watch(themeServiceProvider);

    return Scaffold(
      appBar: AppBarComponent("User Interface"),
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
              setState(() {});
            },
            isFirst: true,
          ),
          SwitchTileWidget(
            title: "Use colorful avatars",
            subtitle:
                "Displays a colorful avatar when the contact photo is unavailable",
            value: SharedPrefService()
                .getBool(PREF_SHOW_COLORFUL_PROFILE_PLACEHOLDER),
            onChanged: (value) {
              SharedPrefService()
                  .saveBool(PREF_SHOW_COLORFUL_PROFILE_PLACEHOLDER, value);
              setState(() {});
            },
          ),
          SwitchTileWidget(
            title: "Show picture in avatar",
            subtitle: "Shows the contact picture if available",
            value: SharedPrefService().getBool(PREF_SHOW_PICTURE_IN_AVATAR),
            onChanged: (value) {
              SharedPrefService().saveBool(PREF_SHOW_PICTURE_IN_AVATAR, value);
              setState(() {});
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
          const SizedBox(height: 10),
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

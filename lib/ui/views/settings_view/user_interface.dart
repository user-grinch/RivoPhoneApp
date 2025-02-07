import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/utils/switch_tile.dart';

class UserInterfaceView extends StatefulWidget {
  const UserInterfaceView({super.key});

  @override
  State<UserInterfaceView> createState() => _UserInterfaceViewState();
}

class _UserInterfaceViewState extends State<UserInterfaceView> {
  bool disableMaterialYou = false;
  bool hideAvatarInitials = false;
  bool showAvatarPictures = true;
  bool iconOnlyBottomNav = false;
  bool enableCustomCallScreen = false;

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
              value: disableMaterialYou,
              onChanged: (value) {
                setState(() {
                  disableMaterialYou = value;
                });
              },
              isFirst: true),
          SwitchTileWidget(
              title: "Dimmed Colors",
              subtitle:
                  "Uses a less colorful version of Material You. Restart is required.",
              value: enableCustomCallScreen,
              onChanged: (value) {
                setState(() {
                  enableCustomCallScreen = value;
                });
              },
              isLast: true),
          const SizedBox(
            height: 20,
          ),
          SwitchTileWidget(
              title: "Show first letter in avartar",
              subtitle:
                  "Displays the first letter of the contact name when a profile picture isn't available",
              value: hideAvatarInitials,
              onChanged: (value) {
                setState(() {
                  hideAvatarInitials = value;
                });
              },
              isFirst: true),
          SwitchTileWidget(
              title: "Show picture in avatar",
              subtitle: "Shows the contact picture if available",
              value: showAvatarPictures,
              onChanged: (value) {
                setState(() {
                  showAvatarPictures = value;
                });
              },
              isLast: true),
          const SizedBox(
            height: 20,
          ),
          SwitchTileWidget(
            title: "Full screen dialer",
            subtitle:
                "Removes the suggestions & makes the dialer use the entire screen",
            value: iconOnlyBottomNav,
            onChanged: (value) {
              setState(() {
                iconOnlyBottomNav = value;
              });
            },
            isFirst: true,
          ),
          SwitchTileWidget(
            title: "Icon-only bottom sheet",
            subtitle:
                "Only shows navigation icons in the bottom navigation bar",
            value: iconOnlyBottomNav,
            onChanged: (value) {
              setState(() {
                iconOnlyBottomNav = value;
              });
            },
            isLast: true,
          ),
        ],
      ),
    );
  }
}

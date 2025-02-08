import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/utils/menu_tile.dart';
import 'package:revo/utils/switch_tile.dart';

class SoundView extends StatefulWidget {
  const SoundView({super.key});

  @override
  State<SoundView> createState() => _SoundViewState();
}

class _SoundViewState extends State<SoundView> {
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
          'Sound & Vibration',
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
              title: "Dialpad tone",
              subtitle: "Sound that plays on dialpad press",
              value: disableMaterialYou,
              onChanged: (value) {
                setState(() {
                  disableMaterialYou = value;
                });
              },
              isFirst: true),
          SwitchTileWidget(
            title: "Dialpad vibration",
            subtitle: "Vibration on dialpad press",
            value: enableCustomCallScreen,
            onChanged: (value) {
              setState(() {
                enableCustomCallScreen = value;
              });
            },
            isLast: true,
          ),
          const SizedBox(
            height: 20,
          ),
          MenuTile(
            title: 'Ringtone Settings',
            subtitle: '',
            icon: HugeIcons.strokeRoundedMusicNote02,
            onTap: () {},
            isFirst: true,
            isLast: true,
          ),
        ],
      ),
    );
  }
}

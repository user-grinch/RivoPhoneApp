import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/view/utils/menu_tile.dart';
import 'package:revo/view/utils/switch_tile.dart';

class CallView extends StatefulWidget {
  const CallView({super.key});

  @override
  State<CallView> createState() => _CallViewState();
}

class _CallViewState extends State<CallView> {
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
          'Call Settings',
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
              title: "Speed dial",
              subtitle: "Directly call someone by holding a dialpad key",
              value: disableMaterialYou,
              onChanged: (value) {
                setState(() {
                  disableMaterialYou = value;
                });
              },
              isFirst: true),
          MenuTile(
            title: 'Speed dial Settings',
            subtitle: '',
            icon: HugeIcons.strokeRoundedDialpadSquare02,
            onTap: () {},
            isLast: true,
          ),
          const SizedBox(
            height: 10,
          ),
          SwitchTileWidget(
              title: "T9 Dialing",
              subtitle: "Predicts words from numeric keypad inputs",
              value: enableCustomCallScreen,
              onChanged: (value) {
                setState(() {
                  enableCustomCallScreen = value;
                });
              },
              isFirst: true,
              isLast: true),
        ],
      ),
    );
  }
}

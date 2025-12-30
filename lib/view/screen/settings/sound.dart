import 'package:android_intent_plus/android_intent.dart';
import 'package:android_intent_plus/flag.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/pref_service.dart';
import 'package:revo/view/utils/menu_tile.dart';
import 'package:revo/view/utils/switch_tile.dart';

class SoundView extends StatefulWidget {
  const SoundView({super.key});

  @override
  State<SoundView> createState() => _SoundViewState();
}

class _SoundViewState extends State<SoundView> {
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
              title: "DTMF tone",
              subtitle: "Dialpad tone that plays during keypress",
              value: SharedPrefService().getBool(PREF_DTMF_TONE, def: true),
              onChanged: (value) {
                SharedPrefService().saveBool(PREF_DTMF_TONE, value);
                setState(() {});
              },
              isFirst: true),
          SwitchTileWidget(
            title: "Dialpad vibration",
            subtitle: "Dialpad vibration that plays during keypress",
            value:
                SharedPrefService().getBool(PREF_DIALPAD_VIBRATION, def: true),
            onChanged: (value) {
              SharedPrefService().saveBool(PREF_DIALPAD_VIBRATION, value);
              setState(() {});
            },
            isLast: true,
          ),
          const SizedBox(
            height: 10,
          ),
          MenuTile(
            title: 'Ringtone Settings',
            subtitle: '',
            icon: HugeIcons.strokeRoundedMusicNote02,
            onTap: () {
              final intent = AndroidIntent(
                action: 'android.settings.SOUND_SETTINGS',
                flags: [Flag.FLAG_ACTIVITY_NEW_TASK],
              );
              intent.launch();
            },
            isFirst: true,
            isLast: true,
          ),
        ],
      ),
    );
  }
}

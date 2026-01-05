import 'package:android_intent_plus/android_intent.dart';
import 'package:android_intent_plus/flag.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/pref_service.dart';
import 'package:revo/view/components/menu_tile.dart';
import 'package:revo/view/components/action_icon_btn.dart';
import 'package:revo/view/components/switch_tile.dart';
import 'package:revo/view/screen/settings/appbarcomponent.dart';

class SoundView extends StatefulWidget {
  const SoundView({super.key});

  @override
  State<SoundView> createState() => _SoundViewState();
}

class _SoundViewState extends State<SoundView> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBarComponent("Sound & Vibration"),
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
            icon: FluentIcons.music_note_2_24_regular,
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

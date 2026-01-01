import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:revo/view/screen/settings/about.dart';
import 'package:revo/view/screen/settings/appbarcomponent.dart';
import 'package:revo/view/screen/settings/call.dart';
import 'package:revo/view/screen/settings/sound.dart';
import 'package:revo/view/screen/settings/user_interface.dart';
import 'package:revo/view/components/center_text.dart';
import 'package:revo/view/components/menu_tile.dart';
import 'package:revo/view/utils/utils.dart';

class SettingsView extends StatelessWidget {
  const SettingsView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBarComponent("Settings"),
      body: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0),
        children: [
          Container(
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.primaryContainer,
              borderRadius: BorderRadius.all(Radius.circular(20)),
            ),
            child: ListTile(
              onTap: () async {
                await launchURL('https://www.patreon.com/grinch_');
              },
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.all(Radius.circular(20)),
              ),
              leading: Icon(
                FluentIcons.heart_24_regular,
                size: 40,
              ),
              title: Column(
                children: [
                  CenterText(
                    text: "Help keep Rivo free for everyone.",
                  ),
                  CenterText(text: "Consider Donating!"),
                ],
              ),
            ),
          ),
          const SizedBox(
            height: 30,
          ),
          CenterText(text: "This section is work in progress"),
          MenuTile(
            title: 'User Interface',
            subtitle: 'Customize looks & behaviors',
            icon: FluentIcons.image_24_regular,
            onTap: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => UserInterfaceView()),
              );
            },
            isFirst: true,
          ),
          MenuTile(
            title: 'Sound & Vibration',
            subtitle: 'Manage ringtones & volume',
            icon: FluentIcons.speaker_2_24_regular,
            onTap: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => SoundView()),
              );
            },
            isLast: true,
          ),
          const SizedBox(height: 10.0),
          MenuTile(
            title: 'Blocklist',
            subtitle: 'Block calls from people',
            icon: FluentIcons.call_prohibited_24_regular,
            onTap: null,
            // () {}
            isFirst: true,
          ),
          MenuTile(
            title: 'Call Settings',
            subtitle: 'Incoming call settings',
            icon: FluentIcons.call_24_regular,
            onTap: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => CallView()),
              );
            },
            isLast: true,
          ),
          const SizedBox(height: 10.0),
          MenuTile(
            title: 'About',
            subtitle: 'Information about the dialer app',
            icon: FluentIcons.info_24_regular,
            onTap: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => AboutView()),
              );
            },
            isFirst: true,
            isLast: true,
          ),
          const SizedBox(height: 12.0),
          Center(
            child: Text(
              '© Copyright 2025-2026',
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

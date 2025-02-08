import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/ui/views/settings_view/about.dart';
import 'package:revo/ui/views/settings_view/call.dart';
import 'package:revo/ui/views/settings_view/sound.dart';
import 'package:revo/ui/views/settings_view/user_interface.dart';
import 'package:revo/utils/center_text.dart';
import 'package:revo/utils/menu_tile.dart';
import 'package:revo/utils/utils.dart';

class SettingsView extends StatelessWidget {
  const SettingsView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: Icon(HugeIcons.strokeRoundedArrowLeft01),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text(
          'Settings',
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
          Container(
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.primaryContainer,
              borderRadius: BorderRadius.all(Radius.circular(15)),
            ),
            child: ListTile(
              onTap: () async {
                await launchURL('https://www.patreon.com/grinch_');
              },
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.all(Radius.circular(15)),
              ),
              leading: Icon(
                HugeIcons.strokeRoundedFavourite,
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
            icon: HugeIcons.strokeRoundedImage02,
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
            icon: HugeIcons.strokeRoundedVolumeHigh,
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
            icon: HugeIcons.strokeRoundedCallBlocked02,
            onTap: () {},
            isFirst: true,
          ),
          MenuTile(
            title: 'Call Settings',
            subtitle: 'Incoming call settings',
            icon: HugeIcons.strokeRoundedCallIncoming03,
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
            icon: HugeIcons.strokeRoundedInformationCircle,
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
              '© Copyright Grinch_ 2025',
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

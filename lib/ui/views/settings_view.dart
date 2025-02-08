import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/ui/views/settings_view/call.dart';
import 'package:revo/ui/views/settings_view/sound.dart';
import 'package:revo/ui/views/settings_view/user_interface.dart';
import 'package:revo/utils/center_text.dart';
import 'package:revo/utils/menu_tile.dart';
import 'package:url_launcher/url_launcher.dart';

class SettingsView extends StatelessWidget {
  const SettingsView({super.key});

  Future<void> _launchURL(String url) async {
    try {
      final Uri uri = Uri.parse(url);
      if (await canLaunchUrl(uri)) {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      } else {
        debugPrint('Cannot launch URL: $url');
        throw Exception('Could not launch $url');
      }
    } catch (e) {
      debugPrint('Error occurred: $e');
    }
  }

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
          CenterText(text: "This page is still work in progress."),
          const SizedBox(
            height: 10,
          ),
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
            title: 'Source Code',
            subtitle: 'View the source code on GitHub',
            icon: HugeIcons.strokeRoundedSourceCodeCircle,
            onTap: () async =>
                await _launchURL('https://github.com/user-grinch/Rivo'),
            isFirst: true,
          ),
          MenuTile(
            title: 'Support Us on Patreon',
            subtitle: 'Contribute to our development',
            icon: HugeIcons.strokeRoundedFavourite,
            onTap: () async =>
                await _launchURL('https://www.patreon.com/grinch_'),
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

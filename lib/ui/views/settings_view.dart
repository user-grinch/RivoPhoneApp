import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/extentions/theme.dart';
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
        title: const Text('Settings'),
      ),
      body: ListView(
        children: [
          Padding(
            padding: EdgeInsets.all(16.0),
            child: Text(
              'General Settings',
              style: GoogleFonts.cabin(
                fontSize: 20,
                fontWeight: FontWeight.normal,
                color: context.colorScheme.onSurface,
              ),
            ),
          ),
          SwitchListTile(
            title: const Text('Material You theming'),
            value: false,
            onChanged: (value) {},
          ),
          SwitchListTile(
            title: const Text('Use system-based dark mode'),
            value: true,
            onChanged: (value) {},
          ),
          SwitchListTile(
            title: const Text('Load profile picture'),
            value: true,
            onChanged: (value) {},
          ),
          SwitchListTile(
            title: const Text('Letters in avatars when available'),
            value: false,
            onChanged: (value) {},
          ),

          // Dialer Settings
          Padding(
            padding: EdgeInsets.all(16.0),
            child: Text(
              'Dialer Settings',
              style: GoogleFonts.cabin(
                fontSize: 20,
                fontWeight: FontWeight.normal,
                color: context.colorScheme.onSurface,
              ),
            ),
          ),
          SwitchListTile(
            title: const Text('Disable dial pad tones'),
            value: true,
            onChanged: (value) {},
          ),
          SwitchListTile(
            title: const Text('Vibrate on key press'),
            value: false,
            onChanged: (value) {},
          ),
          SwitchListTile(
            title: const Text('Show recent calls in suggestions'),
            value: true,
            onChanged: (value) {},
          ),
          SwitchListTile(
            title: const Text('Automatically format phone numbers'),
            value: true,
            onChanged: (value) {},
          ),

          Padding(
            padding: EdgeInsets.all(16.0),
            child: Text(
              'Information',
              style: GoogleFonts.cabin(
                fontSize: 20,
                fontWeight: FontWeight.normal,
                color: context.colorScheme.onSurface,
              ),
            ),
          ),
          ListTile(
            title: const Text('Source Code'),
            subtitle: const Text('View the source code on GitHub'),
            leading: Icon(HugeIcons.strokeRoundedSourceCodeCircle,
                color: Theme.of(context).colorScheme.primary),
            onTap: () async =>
                await _launchURL('https://github.com/user-grinch/Rivo'),
          ),
          ListTile(
            title: const Text('Support Us on Patreon'),
            subtitle: const Text('Contribute to our development'),
            leading: Icon(HugeIcons.strokeRoundedFavourite,
                color: Theme.of(context).colorScheme.primary),
            onTap: () async =>
                await _launchURL('https://www.patreon.com/grinch_'),
          ),

          // Footer
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 20.0),
            child: Center(
              child: Text(
                '© Copyright Grinch_ 2025',
                style: TextStyle(
                  fontSize: 14,
                  color: Colors.grey.shade600,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

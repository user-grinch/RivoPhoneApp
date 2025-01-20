import 'package:flutter/material.dart';
import 'package:revo/extentions/theme.dart';
import 'package:url_launcher/url_launcher_string.dart';

class SettingsView extends StatelessWidget {
  const SettingsView({super.key});

  Future<void> _launchURL(String url) async {
    if (await canLaunchUrlString(url)) {
      await launchUrlString(url);
    } else {
      throw 'Could not launch $url';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: Column(
        children: <Widget>[
          Expanded(
            child: Center(
              child: Text(
                'Settings',
                style: TextStyle(
                  fontSize: 24,
                  color: Colors.grey.shade700,
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    ElevatedButton(
                      onPressed: () async => await _launchURL(
                          'https://github.com/user-grinch/Rivo'),
                      style: ElevatedButton.styleFrom(
                        elevation: 0,
                        backgroundColor: context.colorScheme.primaryContainer,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(30),
                        ),
                        padding: const EdgeInsets.symmetric(
                            horizontal: 16, vertical: 12),
                      ),
                      child: const Text('Source Code'),
                    ),
                    ElevatedButton(
                      onPressed: () async =>
                          await _launchURL('https://www.patreon.com/grinch_'),
                      style: ElevatedButton.styleFrom(
                        elevation: 0,
                        backgroundColor: context.colorScheme.primaryContainer,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(30),
                        ),
                        padding: const EdgeInsets.symmetric(
                            horizontal: 16, vertical: 12),
                      ),
                      child: const Text('Patreon'),
                    ),
                  ],
                ),
                const SizedBox(height: 50),
                const Text(
                  '© Copyright Grinch_ 2025',
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey,
                  ),
                ),
                const SizedBox(height: 50),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

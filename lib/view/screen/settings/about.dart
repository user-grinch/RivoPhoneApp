import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/ui.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/utils/utils.dart';
import 'package:revo/router/router.dart';
import 'package:revo/constants/app_routes.dart';
import 'package:revo/view/components/menu_tile.dart';
import 'package:revo/view/components/action_icon_btn.dart';

class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final colorScheme = context.colorScheme;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const ActionIconButton(
            FluentIcons.arrow_left_24_regular,
            size: 40,
          ),
          onPressed: () => context.pop(),
        ),
        title: Text(
          'About',
          style: GoogleFonts.outfit(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: colorScheme.onSurface,
          ),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            const SizedBox(height: 20),
            Image.asset(
              'assets/icon.png',
              width: 100,
              height: 100,
              fit: BoxFit.cover,
            ),
            const SizedBox(height: 12),
            Text(
              'Rivo',
              style: GoogleFonts.outfit(
                fontSize: 26,
                fontWeight: FontWeight.w700,
                color: colorScheme.onSurface,
              ),
            ),
            const SizedBox(height: 32),
            Container(
              width: double.infinity,
              decoration: BoxDecoration(
                color: colorScheme.secondaryContainer.withOpacity(0.35),
                borderRadius: BorderRadius.circular(28),
              ),
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'About the App',
                      style: GoogleFonts.outfit(
                        fontSize: 18,
                        fontWeight: FontWeight.w600,
                        color: colorScheme.onSurface,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Rivo is a modern dialer app that brings simplicity and elegance to calling. '
                      'Designed with Material You, it adapts seamlessly to your theme.',
                      style: GoogleFonts.outfit(
                        fontSize: 15,
                        color: colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),
            MenuTile(
              title: 'Version',
              subtitle: version,
              icon: FluentIcons.info_24_regular,
              onTap: () {},
              isFirst: true,
            ),
            MenuTile(
              title: 'Build number',
              subtitle: buildNumber,
              icon: FluentIcons.number_symbol_24_regular,
              onTap: () {},
            ),
            MenuTile(
              title: 'Contributors',
              subtitle: 'Meet the team behind the project',
              icon: FluentIcons.people_community_24_regular,
              onTap: () {
                router.goNamed(AppRoutes.contributorsRoute);
              },
              isLast: true,
            ),
            const SizedBox(height: 20),
            MenuTile(
              title: 'Support Us on Patreon',
              subtitle: 'Help keep the project alive',
              icon: FluentIcons.heart_24_regular,
              onTap: () async => await launchURL(patreonUrl),
              isFirst: true,
            ),
            MenuTile(
              title: 'Discord Server',
              subtitle:
                  'Join the discord server and be a part of the community',
              icon: FontAwesomeIcons.discord,
              onTap: () async => await launchURL(discordUrl),
              isLast: true,
            ),
            const SizedBox(height: 20),
            MenuTile(
              title: 'Source Code',
              subtitle: 'View the source code on GitHub',
              icon: FontAwesomeIcons.github,
              onTap: () async => await launchURL(githubUrl),
              isFirst: true,
              isLast: true,
            ),
          ],
        ),
      ),
    );
  }
}

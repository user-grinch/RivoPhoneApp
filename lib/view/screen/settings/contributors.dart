import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/utils/utils.dart';

class Contributor {
  final String name;
  final String role;
  final String? githubUrl;

  const Contributor({
    required this.name,
    required this.role,
    this.githubUrl,
  });
}

const List<Contributor> appContributors = [
  Contributor(
    name: 'Grinch_',
    role: 'Lead Developer',
    githubUrl: 'https://github.com/user-grinch',
  ),
  Contributor(
    name: 'RoBoT_095',
    role: 'Developer',
    githubUrl: 'https://github.com/RoBoT095',
  ),
  Contributor(
    name: 'UniconLabs',
    role: 'App icon',
    githubUrl: 'https://www.flaticon.com/authors/uniconlabs',
  ),
];

class ContributorsView extends StatelessWidget {
  const ContributorsView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Contributors'),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: appContributors.length,
        itemBuilder: (context, index) {
          final contributor = appContributors[index];
          return Card(
            elevation: 0,
            margin: const EdgeInsets.only(bottom: 12),
            color: context.colorScheme.secondaryContainer.withAlpha(80),
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
            child: ListTile(
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              leading: CircleAvatar(
                radius: 30,
                backgroundColor: context.colorScheme.primaryContainer,
                child: Text(
                  contributor.name[0].toUpperCase(),
                  style: TextStyle(
                      color: context.colorScheme.onPrimaryContainer,
                      fontSize: 25),
                ),
              ),
              title: Text(
                contributor.name,
                style: GoogleFonts.outfit(fontWeight: FontWeight.bold),
              ),
              subtitle: Text(
                contributor.role,
                style: GoogleFonts.outfit(fontSize: 13),
              ),
              trailing: contributor.githubUrl != null
                  ? const Icon(FluentIcons.open_24_regular, size: 18)
                  : null,
              onTap: contributor.githubUrl != null
                  ? () => launchURL(contributor.githubUrl!)
                  : null,
            ),
          );
        },
      ),
    );
  }
}

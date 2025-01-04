import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/services/contact_service.dart';
import 'package:revo/ui/contactinfo_view.dart';
import 'package:revo/utils/center_text.dart';
import 'package:revo/utils/circle_profile.dart';

class FavView extends StatelessWidget {
  const FavView({super.key});

  @override
  Widget build(BuildContext context) {
    final length = ContactService().favContacts.length;
    return FutureBuilder(
        future: ContactService().initialize(),
        builder: (context, snapshot) {
          if (length == 0) {
            return CenterText(text: 'No favorite contacts');
          } else {
            return displayFavs(context);
          }
        });
  }

  Widget displayFavs(BuildContext context) {
    var favContacts = ContactService().favContacts;
    return Padding(
      padding: const EdgeInsets.only(top: 30.0),
      child: GridView.builder(
        padding: const EdgeInsets.all(8.0),
        shrinkWrap: true,
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 3,
          crossAxisSpacing: 16.0,
          mainAxisSpacing: 16.0,
          childAspectRatio: 0.8,
        ),
        itemCount: favContacts.length,
        itemBuilder: (context, i) {
          final contact = favContacts[i];
          return InkWell(
            onTap: () {
              Navigator.of(context).push(MaterialPageRoute(
                  builder: (_) => ContactInfoView(favContacts[i])));
            },
            child: Column(
              children: [
                CircleProfile(
                  name: contact.displayName,
                  profile: contact.photo,
                  size: 45,
                ),
                const SizedBox(height: 10),
                Center(
                  child: Text(
                    contact.displayName,
                    textAlign: TextAlign.center,
                    style: GoogleFonts.cabin(
                      color: context.colorScheme.onSurface,
                      fontSize: 14,
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

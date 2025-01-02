import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/services/contact_service.dart';
import 'package:revo/ui/contactinfo_view.dart';

class FavView extends StatelessWidget {
  const FavView({super.key});

  @override
  Widget build(BuildContext context) {
    final length = ContactService().getFavLength();
    return FutureBuilder(
        future: ContactService().initialize(),
        builder: (context, snapshot) {
          if (length == 0) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    'No contacts found',
                    style: GoogleFonts.cabin(
                      fontSize: 16,
                      color: context.colorScheme.onSurface,
                    ),
                  ),
                ],
              ),
            );
          } else {
            return FavContactCard();
          }
        });
  }
}

class FavContactCard extends StatelessWidget {
  const FavContactCard({
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      padding: const EdgeInsets.all(8.0),
      shrinkWrap: true,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        crossAxisSpacing: 16.0,
        mainAxisSpacing: 16.0,
        childAspectRatio: 0.8,
      ),
      itemCount: ContactService().getFavLength(),
      itemBuilder: (context, i) {
        final contact = ContactService().favElementAt(i);
        return InkWell(
          onTap: () {
            Navigator.of(context).push(MaterialPageRoute(
                builder: (_) =>
                    ContactInfoView(ContactService().favElementAt(i))));
          },
          child: Column(
            children: [
              CircleAvatar(
                radius: 40,
                backgroundImage: contact.photoOrThumbnail != null
                    ? MemoryImage(contact.photoOrThumbnail!)
                    : null,
                child: contact.photoOrThumbnail == null
                    ? Icon(
                        Icons.person,
                        size: 40,
                      )
                    : null,
              ),
              const SizedBox(height: 10),
              Center(
                child: Text(
                  contact.displayName,
                  textAlign: TextAlign.center,
                  style: GoogleFonts.cabin(
                    fontSize: 18,
                    color: context.colorScheme.primary,
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

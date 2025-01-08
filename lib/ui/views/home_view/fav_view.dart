import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/services/cubit/contact_service.dart';
import 'package:revo/ui/views/contactinfo_view.dart';
import 'package:revo/utils/center_text.dart';
import 'package:revo/utils/circle_profile.dart';

class FavView extends StatelessWidget {
  const FavView({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<ContactService, List<Contact>>(
      builder: (context, state) {
        var stars = context.read<ContactService>().filterByStars();
        if (stars.isEmpty) {
          return CenterText(text: 'No favorite contacts found');
        }
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
            itemCount: stars.length,
            itemBuilder: (context, i) {
              return _buildFavs(context, stars[i]);
            },
          ),
        );
      },
    );
  }

  Widget _buildFavs(BuildContext context, Contact contact) {
    return InkWell(
      onTap: () {
        Navigator.of(context)
            .push(MaterialPageRoute(builder: (_) => ContactInfoView(contact)));
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
  }
}

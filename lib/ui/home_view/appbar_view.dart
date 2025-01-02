import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/extentions/theme.dart';

class AppBarView extends StatelessWidget implements PreferredSizeWidget {
  @override
  final Size preferredSize;

  const AppBarView({super.key})
      : preferredSize = const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      title: Container(
        decoration: BoxDecoration(
          color: context.colorScheme.surfaceTint.withAlpha(30),
          borderRadius: BorderRadius.circular(50),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            IconButton(
              onPressed: () {},
              icon: Icon(
                Icons.menu,
                color: context.colorScheme.primary,
              ),
            ),
            TextButton(
              style: ButtonStyle(
                overlayColor: MaterialStateProperty.all(Colors.transparent),
              ),
              isSemanticButton: true,
              onPressed: () {
                Navigator.pushNamed(context, searchRoute);
              },
              child: Text(
                'Search in Rivo',
                style: GoogleFonts.cabin(
                  fontSize: 24,
                  color: context.colorScheme.primary,
                ),
              ),
            ),
            IconButton(
              onPressed: () {
                Navigator.pushNamed(context, settingsRoute);
              },
              icon: Icon(
                Icons.more_vert,
                color: context.colorScheme.primary,
              ),
            ),
          ],
        ),
      ),
      centerTitle: true,
    );
  }
}

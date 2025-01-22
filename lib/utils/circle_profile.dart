import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/extentions/theme.dart';

class CircleProfile extends StatelessWidget {
  final Uint8List? profile;
  final String name;
  final double size;
  const CircleProfile(
      {super.key, required this.name, this.profile, required this.size});

  @override
  Widget build(BuildContext context) {
    return CircleAvatar(
      radius: size,
      backgroundColor: context.colorScheme.primaryContainer,
      backgroundImage: profile != null ? MemoryImage(profile!) : null,
      child: profile == null
          ? name.isNotEmpty
              ? Text(
                  name[0].toUpperCase(),
                  style: GoogleFonts.raleway(
                    fontSize: size,
                    fontWeight: FontWeight.w300,
                    color: context.colorScheme.onSurface,
                  ),
                )
              : Icon(
                  HugeIcons.strokeRoundedUser,
                  size: size,
                  color: context.colorScheme.onSurface,
                )
          : null,
    );
  }
}

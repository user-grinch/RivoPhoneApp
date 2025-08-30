import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:hugeicons/hugeicons.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/extensions/theme.dart';
import 'package:revo/services/prefservice.dart';

class CircleProfile extends StatefulWidget {
  final Uint8List? profile;
  final String name;
  final double size;
  const CircleProfile(
      {super.key, required this.name, this.profile, required this.size});

  @override
  State<CircleProfile> createState() => _CircleProfileState();
}

class _CircleProfileState extends State<CircleProfile> {
  @override
  void initState() {
    SharedPrefService().onPreferenceChanged.listen((key) {
      if (key == PREF_SHOW_FIRST_LETTER ||
          key == PREF_SHOW_PICTURE_IN_AVARTAR) {
        setState(() {});
      }
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    bool showPic = widget.profile != null &&
        SharedPrefService().getBool(PREF_SHOW_PICTURE_IN_AVARTAR, def: true);

    bool showFirstLetter = widget.name.isNotEmpty &&
        SharedPrefService().getBool(PREF_SHOW_FIRST_LETTER, def: true);
    return CircleAvatar(
      radius: widget.size,
      backgroundColor: context.colorScheme.secondaryContainer,
      backgroundImage: showPic ? MemoryImage(widget.profile!) : null,
      child: !showPic
          ? showFirstLetter
              ? Text(
                  widget.name[0].toUpperCase(),
                  style: GoogleFonts.raleway(
                    fontSize: widget.size,
                    fontWeight: FontWeight.w300,
                    color: context.colorScheme.onSurface,
                  ),
                )
              : Icon(
                  HugeIcons.strokeRoundedUser,
                  size: widget.size,
                  color: context.colorScheme.onSurface,
                )
          : null,
    );
  }
}

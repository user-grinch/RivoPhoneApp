import 'dart:typed_data';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/pref.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/pref_service.dart';

class CircleProfile extends StatefulWidget {
  final Uint8List? profile;
  final String name;
  final double size;

  const CircleProfile({
    super.key,
    required this.name,
    this.profile,
    required this.size,
  });

  @override
  State<CircleProfile> createState() => _CircleProfileState();
}

class _CircleProfileState extends State<CircleProfile> {
  @override
  void initState() {
    SharedPrefService().onPreferenceChanged.listen((key) {
      if (key == PREF_SHOW_FIRST_LETTER || key == PREF_SHOW_PICTURE_IN_AVATAR) {
        if (mounted) setState(() {});
      }
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = context.colorScheme;

    bool showPic = widget.profile != null &&
        SharedPrefService().getBool(PREF_SHOW_PICTURE_IN_AVATAR, def: true);

    bool showFirstLetter = widget.name.isNotEmpty &&
        SharedPrefService().getBool(PREF_SHOW_FIRST_LETTER, def: true);

    final double borderRadius = widget.size * 0.5;
    final double diameter = widget.size * 2;

    return Container(
      width: diameter,
      height: diameter,
      decoration: BoxDecoration(
        color: colorScheme.secondaryContainer,
        borderRadius: BorderRadius.circular(borderRadius),
        image: showPic
            ? DecorationImage(
                image: MemoryImage(widget.profile!),
                fit: BoxFit.cover,
              )
            : null,
      ),
      child: !showPic
          ? Center(
              child: showFirstLetter
                  ? Text(
                      widget.name[0].toUpperCase(),
                      style: GoogleFonts.outfit(
                        fontSize: widget.size * 0.8,
                        fontWeight: FontWeight.w600,
                        color: colorScheme.onSecondaryContainer,
                      ),
                    )
                  : Icon(
                      FluentIcons.person_24_filled,
                      size: widget.size,
                      color: colorScheme.onSecondaryContainer,
                    ),
            )
          : null,
    );
  }
}

import 'dart:typed_data';

import 'package:flutter/material.dart';
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
      backgroundImage: profile != null ? MemoryImage(profile!) : null,
      child: profile == null
          ? name.isNotEmpty
              ? Text(
                  name[0].toUpperCase(),
                  style: TextStyle(
                      color: context.colorScheme.onSurface, fontSize: size),
                )
              : Icon(
                  Icons.person,
                  size: size,
                  color: context.colorScheme.onPrimaryContainer,
                )
          : null,
    );
  }
}

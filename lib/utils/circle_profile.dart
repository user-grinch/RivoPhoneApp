import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:revo/extentions/theme.dart';

class CircleProfile extends StatelessWidget {
  final Uint8List? profile;
  final double size;
  const CircleProfile({super.key, this.profile, required this.size});

  @override
  Widget build(BuildContext context) {
    return CircleAvatar(
      radius: size,
      backgroundImage: profile != null ? MemoryImage(profile!) : null,
      child: profile == null
          ? Icon(
              Icons.person,
              size: size,
              color: context.colorScheme.onPrimaryContainer,
            )
          : null,
    );
  }
}

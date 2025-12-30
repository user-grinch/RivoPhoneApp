import 'package:flutter/material.dart';

extension ColorSchemeExtension on BuildContext {
  ColorScheme get colorScheme => Theme.of(this).colorScheme;
}

extension ThemeExtension on BuildContext {
  TextTheme get textTheme => Theme.of(this).textTheme;
}

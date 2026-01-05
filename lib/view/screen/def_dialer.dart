import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:android_intent_plus/android_intent.dart';
import 'package:flutter_tele/flutter_tele.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/constants/routes.dart';

class DefaultDialerScreen extends StatefulWidget {
  const DefaultDialerScreen({super.key});

  @override
  State<DefaultDialerScreen> createState() => _DefaultDialerScreenState();
}

class _DefaultDialerScreenState extends State<DefaultDialerScreen>
    with WidgetsBindingObserver {
  bool isDefault = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _checkDialerStatus(navigateOnly: true);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _checkDialerStatus(navigateOnly: true);
    }
  }

  Future<void> _checkDialerStatus({bool navigateOnly = false}) async {
    isDefault = await TeleDialer.isDefaultDialer();

    if (mounted) {
      if (isDefault) {
        Navigator.pushNamedAndRemoveUntil(
          context,
          homeRoute,
          (route) => false,
        );
      } else if (!navigateOnly) {
        const intent = AndroidIntent(
          action: 'android.settings.MANAGE_DEFAULT_APPS_SETTINGS',
        );
        intent.launch();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surface,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(flex: 2),
              Center(
                child: Container(
                  width: 120,
                  height: 120,
                  decoration: BoxDecoration(
                    color: colorScheme.secondaryContainer,
                    borderRadius: BorderRadius.circular(28),
                  ),
                  child: Icon(
                    Icons.phone_rounded,
                    size: 48,
                    color: colorScheme.primary,
                  ),
                ),
              ),
              const SizedBox(height: 32),
              Text(
                'Set Rivo as default',
                style: theme.textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: colorScheme.onSurface,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              Text(
                'To provide caller ID and spam protection, Rivo needs to be your primary phone app.',
                style: theme.textTheme.bodyLarge?.copyWith(
                  color: colorScheme.onSurfaceVariant,
                  height: 1.5,
                ),
                textAlign: TextAlign.center,
              ),
              const Spacer(flex: 3),
              ButtonM3E(
                onPressed: () => _checkDialerStatus(navigateOnly: false),
                label: const Text(
                  'Set as Default',
                  style: TextStyle(fontSize: 18),
                ),
                icon: const Icon(FluentIcons.settings_28_filled),
                size: ButtonM3ESize.md,
                style: ButtonM3EStyle.tonal,
                shape: ButtonM3EShape.round,
              ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }
}

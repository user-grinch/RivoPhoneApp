import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/controller/services/contact_service.dart';
import 'package:revo/controller/services/telephony_service.dart';
import 'package:revo/main.dart';
import 'package:revo/model/call_state.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/router/router.dart';
import 'package:revo/view/components/action_btn.dart';
import 'package:revo/view/components/circle_profile.dart';
import 'package:revo/constants/app_routes.dart';

class CallScreen extends StatefulHookConsumerWidget {
  const CallScreen({super.key});

  @override
  ConsumerState<CallScreen> createState() => _CallScreenState();
}

class _CallScreenState extends ConsumerState<CallScreen> {
  @override
  void initState() {
    super.initState();
  }

  Widget buildBottomActions(BuildContext context, CallState state) {
    final colorScheme = Theme.of(context).colorScheme;
    switch (state) {
      case CallState.incoming:
        return Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            CallActionButton(
                label: "Decline",
                icon: Icons.call_end_rounded,
                backgroundColor: colorScheme.errorContainer,
                foregroundColor: colorScheme.onErrorContainer,
                onTap: () {
                  gProvider
                      .read(telephonyServiceProvider.notifier)
                      .declineCall();
                }),
            const SizedBox(width: 24),
            CallActionButton(
                label: "Answer",
                icon: Icons.call_rounded,
                backgroundColor: const Color(0xFFC3EED0),
                foregroundColor: const Color(0xFF073819),
                onTap: () {
                  TelephonyService().acceptCall();
                }),
          ],
        );
      case CallState.initiating:
      case CallState.ringing:
      case CallState.outgoing:
        return Center(
          child: CallActionButton(
              label: "Cancel",
              icon: Icons.call_end_rounded,
              backgroundColor: colorScheme.errorContainer,
              foregroundColor: colorScheme.onErrorContainer,
              onTap: () {
                TelephonyService().declineCall();
              }),
        );
      case CallState.connected:
        return Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            CallActionButton(
              label: "Mute",
              icon: Icons.mic_off,
              backgroundColor: colorScheme.secondaryContainer,
              foregroundColor: colorScheme.onSecondaryContainer,
              onTap: () {},
            ),
            CallActionButton(
              label: "End",
              icon: Icons.call_end,
              backgroundColor: colorScheme.errorContainer,
              foregroundColor: colorScheme.onErrorContainer,
              onTap: () => TelephonyService().declineCall(),
            ),
            CallActionButton(
              label: "Speaker",
              icon: Icons.volume_up,
              backgroundColor: colorScheme.secondaryContainer,
              foregroundColor: colorScheme.onSecondaryContainer,
              onTap: () {},
            ),
          ],
        );
      default:
        return const SizedBox();
    }
  }

  @override
  Widget build(BuildContext context) {
    final telService = ref.watch(telephonyServiceProvider.notifier);
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final textTheme = theme.textTheme;
    return Placeholder();

    ref.listen(telephonyServiceProvider, (e, k) {
      setState(() {});
    });

    final call = telService.getCall();

    final contacts = ref.watch(contactServiceProvider.notifier);
    Contact contact = contacts.findByNumber(call?.remoteNumber ?? "");

    final String displayName = contact.name;
    final String displayNumber = call?.remoteNumber ?? "Unknown Number";
    final bool isVideo = (call?.remoteVideoCount ?? 0) > 0;

    final pulseController = useAnimationController(
      duration: const Duration(seconds: 2),
    )..repeat(reverse: true);

    final scaleAnimation = Tween<double>(begin: 1.0, end: 1.15).animate(
      CurvedAnimation(parent: pulseController, curve: Curves.easeInOutSine),
    );

    final rippleController = useAnimationController(
      duration: const Duration(seconds: 2),
    )..repeat();

    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
          child: Column(
            children: [
              const SizedBox(height: 24),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                decoration: BoxDecoration(
                  color: colorScheme.secondaryContainer,
                  borderRadius: BorderRadius.circular(30),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      isVideo ? Icons.videocam : Icons.call,
                      size: 16,
                      color: colorScheme.onSurfaceVariant,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      "Sim ${call?.simSlot ?? 0} - ${telService.getCallState().name.capitalize()} ${isVideo ? 'Video ' : ''}Call",
                      style: textTheme.labelMedium?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                        letterSpacing: 0.5,
                      ),
                    ),
                  ],
                ),
              ),
              const Spacer(flex: 2),
              SizedBox(
                width: 250,
                height: 250,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    AnimatedBuilder(
                      animation: rippleController,
                      builder: (context, child) {
                        return Container(
                          width: 200 + (rippleController.value * 50),
                          height: 200 + (rippleController.value * 50),
                          decoration: BoxDecoration(
                            shape: BoxShape.rectangle,
                            borderRadius: BorderRadius.circular(40),
                            border: Border.all(
                              color: colorScheme.primary
                                  .withOpacity(1.0 - rippleController.value),
                              width: 1,
                            ),
                          ),
                        );
                      },
                    ),
                    ScaleTransition(
                      scale: scaleAnimation,
                      child: Center(
                        child: CircleProfile(
                          name: contact.name,
                          profile: contact.photo,
                          size: 80,
                          col: colorScheme.primaryContainer,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 40),
              Text(
                displayName,
                textAlign: TextAlign.center,
                style: textTheme.displaySmall?.copyWith(
                  fontWeight: FontWeight.w600,
                  fontSize: 40,
                  color: colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                displayNumber,
                textAlign: TextAlign.center,
                style: textTheme.titleMedium?.copyWith(
                  color: colorScheme.onSurfaceVariant,
                  fontSize: 18,
                  fontWeight: FontWeight.w400,
                ),
              ),
              const Spacer(flex: 3),
              ButtonM3E(
                onPressed: () {},
                style: ButtonM3EStyle.tonal,
                size: ButtonM3ESize.sm,
                label: const Text("Message"),
              ),
              const SizedBox(height: 32),
              buildBottomActions(context, telService.getCallState()),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }
}

extension StringCapitalize on String {
  String capitalize() =>
      length > 0 ? '${this[0].toUpperCase()}${substring(1)}' : '';
}

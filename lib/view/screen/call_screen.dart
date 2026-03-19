import 'dart:async';

import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_tele/flutter_tele.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/controller/extensions/string.dart';
import 'package:revo/controller/services/contact_service.dart';
import 'package:revo/controller/services/telephony_service.dart';
import 'package:revo/model/contact.dart';

import 'package:revo/view/components/action_btn.dart';
import 'package:revo/view/components/call_status_badge.dart';
import 'package:revo/view/components/circle_profile.dart';
import 'package:url_launcher/url_launcher.dart';

class CallScreen extends StatefulHookConsumerWidget {
  const CallScreen({super.key});

  @override
  ConsumerState<CallScreen> createState() => _CallScreenState();
}

class _CallScreenState extends ConsumerState<CallScreen> {
  Widget buildBottomActions(BuildContext context, CallState state) {
    final colorScheme = Theme.of(context).colorScheme;
    final telService = ref.watch(telephonyServiceProvider.notifier);
    final call = telService.getCall();

    switch (state) {
      case CallState.incoming:
      case CallState.ringing:
        return Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            Flexible(
              child: CallActionButton(
                  label: "Decline",
                  icon: Icons.call_end_rounded,
                  isLarge: true,
                  toggleable: false,
                  backgroundColor: colorScheme.errorContainer,
                  foregroundColor: colorScheme.onErrorContainer,
                  onTap: () => telService.declineCall()),
            ),
            const SizedBox(width: 16),
            Flexible(
              child: CallActionButton(
                  label: "Answer",
                  icon: Icons.call_rounded,
                  isLarge: true,
                  toggleable: false,
                  backgroundColor: const Color(0xFFC3EED0),
                  foregroundColor: const Color(0xFF073819),
                  onTap: () => telService.acceptCall()),
            ),
          ],
        );

      case CallState.initiating:
      case CallState.outgoing:
        return Center(
          child: CallActionButton(
              label: "End",
              icon: Icons.call_end_rounded,
              isSuperLarge: true,
              toggleable: false,
              backgroundColor: colorScheme.errorContainer,
              foregroundColor: colorScheme.onErrorContainer,
              onTap: () => telService.declineCall()),
        );

      case CallState.connected:
        final isMuted = call?.muted ?? false;
        final isSpeaker = call?.speaker ?? false;

        return Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            Flexible(
              child: CallActionButton(
                label: isMuted ? "Unmute" : "Mute",
                icon: isMuted ? Icons.mic_off : Icons.mic,
                backgroundColor: isMuted
                    ? colorScheme.primaryContainer
                    : colorScheme.secondaryContainer,
                foregroundColor: isMuted
                    ? colorScheme.onPrimaryContainer
                    : colorScheme.onSecondaryContainer,
                onTap: () => isMuted ? telService.unmute() : telService.mute(),
              ),
            ),
            Flexible(
              child: CallActionButton(
                label: "End",
                icon: Icons.call_end,
                isLarge: true,
                toggleable: false,
                backgroundColor: colorScheme.errorContainer,
                foregroundColor: colorScheme.onErrorContainer,
                onTap: () => telService.declineCall(),
              ),
            ),
            Flexible(
              child: CallActionButton(
                label: "Speaker",
                icon: isSpeaker ? Icons.volume_up : Icons.volume_down,
                backgroundColor: isSpeaker
                    ? colorScheme.primaryContainer
                    : colorScheme.secondaryContainer,
                foregroundColor: isSpeaker
                    ? colorScheme.onPrimaryContainer
                    : colorScheme.onSecondaryContainer,
                onTap: () => isSpeaker
                    ? telService.useEarpiece()
                    : telService.useSpeaker(),
              ),
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
    final callState = telService.getCallState();
    final ticker = useState(0);

    final call = telService.getCall();
    final contacts = ref.watch(contactServiceProvider.notifier);
    Contact contact = contacts.findByNumber(call?.remoteNumber ?? "");

    final String displayName = contact.name;
    final String displayNumber = call?.remoteNumber ?? "Unknown Number";

    final pulseController =
        useAnimationController(duration: const Duration(seconds: 2))
          ..repeat(reverse: true);
    final scaleAnimation = Tween<double>(begin: 1.0, end: 1.15).animate(
      CurvedAnimation(parent: pulseController, curve: Curves.easeInOutSine),
    );
    final rippleController =
        useAnimationController(duration: const Duration(seconds: 2))..repeat();

    ref.listen(telephonyServiceProvider, (e, k) {
      if (mounted) setState(() {});
    });

    useEffect(() {
      Timer? timer;
      if (callState == CallState.connected) {
        timer =
            Timer.periodic(const Duration(seconds: 1), (t) => ticker.value++);
      }
      return () => timer?.cancel();
    }, [callState]);

    return Scaffold(
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            return SingleChildScrollView(
              physics: const ClampingScrollPhysics(),
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight: constraints.maxHeight,
                ),
                child: IntrinsicHeight(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 24.0, vertical: 16.0),
                    child: Column(
                      children: [
                        const SizedBox(height: 16),
                        CallStatusBadge(
                          icon: Icons.call,
                          label:
                              "Sim ${(call?.simSlot ?? 0) + 1} - ${callState.name.capitalize()} Call",
                        ),
                        const Spacer(),
                        SizedBox(
                          width: 200,
                          height: 200,
                          child: Stack(
                            alignment: Alignment.center,
                            children: [
                              AnimatedBuilder(
                                animation: rippleController,
                                builder: (context, child) {
                                  return Container(
                                    width: 150 + (rippleController.value * 50),
                                    height: 150 + (rippleController.value * 50),
                                    decoration: BoxDecoration(
                                      shape: BoxShape.rectangle,
                                      borderRadius: BorderRadius.circular(40),
                                      border: Border.all(
                                        color: colorScheme.primary.withOpacity(
                                            1.0 - rippleController.value),
                                        width: 1,
                                      ),
                                    ),
                                  );
                                },
                              ),
                              ScaleTransition(
                                scale: scaleAnimation,
                                child: CircleProfile(
                                  name: contact.name,
                                  profile: contact.photo,
                                  size: 80,
                                  col: colorScheme.primaryContainer,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 24),
                        Text(
                          displayName,
                          textAlign: TextAlign.center,
                          style: textTheme.displaySmall?.copyWith(
                            fontWeight: FontWeight.w600,
                            fontSize: 32,
                            color: colorScheme.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          displayNumber,
                          textAlign: TextAlign.center,
                          style: textTheme.titleMedium?.copyWith(
                            color: colorScheme.onSurfaceVariant,
                            fontSize: 16,
                            fontWeight: FontWeight.w400,
                          ),
                        ),
                        if (callState == CallState.connected) ...[
                          const SizedBox(height: 16),
                          CallStatusBadge(
                            icon: FluentIcons.clock_24_filled,
                            label: "Duration: ${telService.getDuration()}",
                          ),
                        ],
                        const Spacer(flex: 2),
                        ButtonM3E(
                          onPressed: () async {
                            final Uri smsLaunchUri =
                                Uri(scheme: 'sms', path: displayNumber);
                            if (await canLaunchUrl(smsLaunchUri))
                              await launchUrl(smsLaunchUri);
                          },
                          style: ButtonM3EStyle.tonal,
                          size: ButtonM3ESize.md,
                          label: const Text("Message"),
                        ),
                        const SizedBox(height: 24),
                        buildBottomActions(context, telService.getCallState()),
                        const SizedBox(height: 8),
                      ],
                    ),
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

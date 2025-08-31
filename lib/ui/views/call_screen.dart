import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extensions/theme.dart';

class CallScreenView extends StatelessWidget {
  const CallScreenView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: context.colorScheme.surface,
      body: SafeArea(
        child: Stack(
          children: [
            Align(
              alignment: Alignment.topCenter,
              child: Padding(
                padding:
                    const EdgeInsets.symmetric(vertical: 40, horizontal: 16),
                child: Column(
                  children: [
                    CircleAvatar(
                      radius: 90,
                      backgroundColor: context.colorScheme.secondaryContainer,
                      child: Icon(
                        Icons.person,
                        size: 100,
                        color: context.colorScheme.onSecondaryContainer,
                      ),
                    ),
                    const SizedBox(height: 20),
                    Text(
                      "John Doe",
                      style: GoogleFonts.raleway(
                        fontSize: 30,
                        fontWeight: FontWeight.bold,
                        color: context.colorScheme.onSurface,
                      ),
                    ),
                    Text(
                      "+1 (123) 456-7890",
                      style: GoogleFonts.raleway(
                        fontSize: 15,
                        color: context.colorScheme.onSurface.withAlpha(150),
                      ),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      "01:25",
                      style: GoogleFonts.raleway(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                        color: context.colorScheme.primary.withAlpha(150),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Align(
              alignment: Alignment.bottomCenter,
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 20, vertical: 30),
                decoration: BoxDecoration(
                  color: context.colorScheme.surface,
                  borderRadius:
                      const BorderRadius.vertical(top: Radius.circular(30)),
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        _CallActionButton(
                          icon: Icons.record_voice_over,
                          label: "Record",
                          color: context.colorScheme.secondaryContainer,
                          textColor: context.colorScheme.onSecondaryContainer,
                          size: 65,
                        ),
                        _CallActionButton(
                          icon: Icons.mic_off,
                          label: "Mute",
                          color: context.colorScheme.secondaryContainer,
                          textColor: context.colorScheme.onSecondaryContainer,
                          size: 65,
                        ),
                        _CallActionButton(
                          icon: Icons.pause,
                          label: "Hold",
                          color: context.colorScheme.secondaryContainer,
                          textColor: context.colorScheme.onSecondaryContainer,
                          size: 65,
                        ),
                      ],
                    ),
                    const SizedBox(height: 20),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        _CallActionButton(
                          icon: Icons.add_call,
                          label: "Add Call",
                          color: context.colorScheme.secondaryContainer,
                          textColor: context.colorScheme.onSecondaryContainer,
                          size: 65,
                        ),
                        _CallActionButton(
                          icon: Icons.volume_up,
                          label: "Speaker",
                          color: context.colorScheme.secondaryContainer,
                          textColor: context.colorScheme.onSecondaryContainer,
                          size: 65,
                        ),
                        _CallActionButton(
                          icon: Icons.dialpad,
                          label: "Dialpad",
                          color: context.colorScheme.secondaryContainer,
                          textColor: context.colorScheme.onSecondaryContainer,
                          size: 65,
                        ),
                      ],
                    ),
                    const SizedBox(height: 50),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        _CallActionButton(
                          icon: Icons.call_end,
                          label: "End",
                          color: Colors.redAccent,
                          textColor: context.colorScheme.onError,
                          size: 70,
                          onPressed: () {
                            // Add your end call logic here
                          },
                          showLabel: false,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _CallActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final Color textColor;
  final double size;
  final VoidCallback? onPressed;
  final bool showLabel;

  const _CallActionButton({
    required this.icon,
    required this.label,
    required this.color,
    required this.textColor,
    this.size = 60,
    this.onPressed,
    this.showLabel = true,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        GestureDetector(
          onTap: onPressed,
          child: Container(
            width: size,
            height: size,
            decoration: BoxDecoration(
              color: color,
              shape: BoxShape.circle,
            ),
            child: Icon(
              icon,
              size: size * 0.35,
              color: Colors.white,
            ),
          ),
        ),
        const SizedBox(height: 10),
        if (showLabel)
          Text(
            label,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w500,
              color: textColor,
            ),
          ),
      ],
    );
  }
}

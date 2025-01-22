import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/theme.dart';
import 'package:just_audio/just_audio.dart';

class DialPadButton extends StatefulWidget {
  final String mainText;
  final String? subText;
  final Function(String) onUpdate;

  const DialPadButton({
    required this.mainText,
    this.subText,
    required this.onUpdate,
    super.key,
  });

  @override
  _DialPadButtonState createState() => _DialPadButtonState();
}

class _DialPadButtonState extends State<DialPadButton> {
  late final AudioPlayer _audioPlayer;
  late final String soundPath;

  @override
  void initState() {
    super.initState();
    String text = widget.mainText;
    if (widget.mainText == '#') {
      text = 'hash';
    } else if (widget.mainText == '*') {
      text = 'star';
    }
    soundPath =
        'assets/dialpad/${text}.mp3'; // Ensure your sound files are stored in assets
    _audioPlayer = AudioPlayer();
    _preloadSound();
  }

  Future<void> _preloadSound() async {
    try {
      await _audioPlayer.setAsset(soundPath);
    } catch (e) {
      debugPrint("Error preloading sound from $soundPath: $e");
    }
  }

  Future<void> _playSound() async {
    try {
      await _audioPlayer.stop();
      await _audioPlayer.seek(Duration.zero);
      await _audioPlayer.play();
    } catch (e) {
      debugPrint("Error playing sound from $soundPath: $e");
    }
  }

  @override
  void dispose() {
    _audioPlayer.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return TextButton(
      style: TextButton.styleFrom(
        elevation: 0,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(50)),
        ),
        backgroundColor: context.colorScheme.surface.withAlpha(150),
        overlayColor: context.colorScheme.onSurface,
      ),
      onPressed: () async {
        widget.onUpdate(widget.mainText);
        await _playSound();
      },
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            widget.mainText,
            style: GoogleFonts.raleway(
              fontSize: 28,
              fontWeight: FontWeight.normal,
              color: context.colorScheme.onSurface,
            ),
          ),
          if (widget.subText != null)
            Text(
              widget.subText!,
              style: GoogleFonts.raleway(
                fontSize: 12,
                color: context.colorScheme.onSurface,
              ),
            ),
        ],
      ),
    );
  }
}

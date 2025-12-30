import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/model/sim_card.dart';
import 'package:revo/controller/providers/activity_service.dart';
import 'package:revo/view/utils/center_text.dart';

class SimPicker {
  final BuildContext context;
  final List<SimCard> simCards;
  final String number;

  SimPicker({
    required this.context,
    required this.simCards,
    required this.number,
  });

  Future<void> show() async {
    if (simCards.isEmpty) return;

    if (simCards.length == 1) {
      await ActivityService()
          .makePhoneCall(number, simCards.first.simSlotIndex);
      return;
    }

    await showDialog(
      context: context,
      builder: (_) => _buildDialog(),
    );
  }

  Widget _buildDialog() {
    return Dialog(
      backgroundColor: context.colorScheme.surfaceContainer,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(24),
      ),
      alignment: Alignment.bottomCenter,
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const CenterText(
              text: "Choose SIM for call",
              size: 24,
            ),
            const SizedBox(height: 8),
            ...simCards.map((sim) => _buildSimCard(sim)),
          ],
        ),
      ),
    );
  }

  Widget _buildSimCard(SimCard sim) {
    return Card(
      elevation: 0,
      margin: const EdgeInsets.symmetric(vertical: 4),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(24),
      ),
      color: context.colorScheme.primaryContainer,
      child: InkWell(
        onTap: () async {
          await ActivityService().makePhoneCall(number, sim.simSlotIndex);
          Navigator.of(context).pop();
        },
        borderRadius: BorderRadius.circular(24),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: context.colorScheme.primary,
                child: Text(
                  "${sim.simSlotIndex + 1}",
                  style: GoogleFonts.raleway(
                    color: context.colorScheme.onSecondary,
                    fontSize: 22,
                  ),
                ),
              ),
              const SizedBox(width: 16),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  CenterText(
                    text:
                        "${sim.carrierName} (${sim.countryCode.toUpperCase()})",
                    size: 18,
                  ),
                  const SizedBox(height: 2),
                  CenterText(
                    text: sim.phoneNumber,
                    size: 12,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

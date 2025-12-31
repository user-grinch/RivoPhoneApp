import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/model/sim_card.dart';
import 'package:revo/controller/providers/activity_service.dart';

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
      barrierDismissible: true,
      builder: (_) => _buildDialog(),
    );
  }

  Widget _buildDialog() {
    return Dialog(
      backgroundColor: context.colorScheme.surfaceContainerHigh,
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(32)),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.sim_card_outlined,
              color: context.colorScheme.primary,
              size: 32,
            ),
            const SizedBox(height: 16),
            Text(
              "Select SIM Card",
              style: GoogleFonts.outfit(
                fontSize: 22,
                fontWeight: FontWeight.w600,
                color: context.colorScheme.onSurface,
              ),
            ),
            const SizedBox(height: 24),
            ...simCards.map((sim) => _buildSimItem(sim)),
          ],
        ),
      ),
    );
  }

  Widget _buildSimItem(SimCard sim) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Material(
        color: context.colorScheme.secondaryContainer,
        borderRadius: BorderRadius.circular(20),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: () async {
            await ActivityService().makePhoneCall(number, sim.simSlotIndex);
            if (context.mounted) Navigator.of(context).pop();
          },
          splashFactory: InkSparkle.splashFactory,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: context.colorScheme.primary,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Center(
                    child: Text(
                      "${sim.simSlotIndex + 1}",
                      style: GoogleFonts.outfit(
                        color: context.colorScheme.onPrimary,
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        sim.carrierName,
                        style: GoogleFonts.outfit(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: context.colorScheme.onSecondaryContainer,
                        ),
                      ),
                      Text(
                        sim.phoneNumber,
                        style: GoogleFonts.outfit(
                          fontSize: 13,
                          color: context.colorScheme.onSecondaryContainer
                              .withOpacity(0.7),
                        ),
                      ),
                    ],
                  ),
                ),
                Icon(
                  Icons.chevron_right_rounded,
                  color:
                      context.colorScheme.onSecondaryContainer.withOpacity(0.4),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

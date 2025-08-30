import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extensions/theme.dart';
import 'package:revo/model/sim_card.dart';
import 'package:revo/services/activity_service.dart';
import 'package:revo/services/cubit/mobile_service.dart';
import 'package:revo/utils/center_text.dart';

Future<void> simChooserDialog(BuildContext context, String number) {
  // If only one sim card, skip dialog
  if (context.read<MobileService>().getSimInfo.length == 1) {
    return ActivityService().makePhoneCall(number, 1);
  }
  return showDialog(
      context: context,
      builder: (context) => BlocBuilder<MobileService, List<SimCard>>(
            builder: (context, state) {
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
                      CenterText(
                        text: "Choose SIM for call",
                        size: 24,
                      ),
                      const SizedBox(height: 8),
                      Column(
                        children:
                            context.read<MobileService>().getSimInfo.map((sim) {
                          return _buildSimCard(context, sim, number);
                        }).toList(),
                      ),
                    ],
                  ),
                ),
              );
            },
          ));
}

Widget _buildSimCard(BuildContext context, SimCard sim, String number) {
  return Card(
    elevation: 0,
    margin: const EdgeInsets.symmetric(vertical: 4),
    shape: RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(24),
    ),
    color: context.colorScheme.primaryContainer,
    child: InkWell(
      onTap: () async {
        ActivityService().makePhoneCall(number, sim.simSlotIndex);
        Navigator.of(context).pop();
      },
      borderRadius: BorderRadius.circular(20),
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Row(
          children: [
            CircleAvatar(
              backgroundColor: Theme.of(context).colorScheme.primary,
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
                  text: "${sim.carrierName} (${sim.countryCode.toUpperCase()})",
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

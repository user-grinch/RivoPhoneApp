import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/model/sim_card.dart';
import 'package:revo/services/activity_service.dart';
import 'package:revo/services/cubit/mobile_service.dart';
import 'package:revo/utils/center_text.dart';

Widget simChooserDialog(BuildContext context, String number) {
  return BlocBuilder<MobileService, List<SimCard>>(
    builder: (context, state) {
      return Dialog(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(28),
        ),
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CenterText(
                text: "Choose SIM for call",
                size: 24,
              ),
              const SizedBox(height: 16),
              Column(
                children: context.read<MobileService>().getSimInfo.map((sim) {
                  return _buildSimCard(context, sim, number);
                }).toList(),
              ),
            ],
          ),
        ),
      );
    },
  );
}

Widget _buildSimCard(BuildContext context, SimCard sim, String number) {
  return Card(
    elevation: 0,
    margin: const EdgeInsets.symmetric(vertical: 8),
    shape: RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(16),
    ),
    color: context.colorScheme.primaryContainer,
    child: InkWell(
      onTap: () {
        ActivityService().makePhoneCall(number, sim.simSlotIndex);
        Navigator.of(context).pop(sim);
      },
      borderRadius: BorderRadius.circular(16),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          children: [
            CircleAvatar(
              backgroundColor: Theme.of(context).colorScheme.primary,
              child: Text(
                "${sim.cardId + 1}",
                style: GoogleFonts.cabin(
                  color: context.colorScheme.onPrimary,
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
                const SizedBox(height: 4),
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

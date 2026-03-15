import 'package:flutter_sim_data/sim_data_model.dart' as lib;

class SimCard {
  final String carrierName;
  final bool isESIM;
  final int subscriptionId;
  final int simSlotIndex;
  final int cardId;
  final String phoneNumber;
  final String displayName;
  final String countryCode;

  SimCard({
    required this.carrierName,
    required this.isESIM,
    required this.subscriptionId,
    required this.simSlotIndex,
    required this.cardId,
    required this.phoneNumber,
    required this.displayName,
    required this.countryCode,
  });

  factory SimCard.fromInternal(lib.SimDataModel data) {
    return SimCard(
      carrierName: data.carrierName,
      isESIM: data.isESIM,
      subscriptionId: data.subscriptionId,
      simSlotIndex: data.simSlotIndex,
      cardId: data.cardId,
      phoneNumber: data.phoneNumber,
      displayName: data.displayName,
      countryCode: data.countryCode,
    );
  }
}

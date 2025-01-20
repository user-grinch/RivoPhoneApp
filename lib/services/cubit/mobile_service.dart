import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_sim_data/sim_data.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/model/sim_card.dart';
import 'package:revo/services/activity_service.dart';

class MobileService extends Cubit<List<SimCard>> {
  List<SimCard>? _simCards;
  MobileService() : super([]) {
    _initialize();
  }

  Future<void> _initialize() async {
    await ActivityService().requestPermissions();
    if (await Permission.phone.status.isGranted) {
      try {
        var data = await SimData().getSimData();
        _simCards = data.map((e) => SimCard.fromInternal(e)).toList();
      } catch (e) {
        // TODO: Show error dialog
      }
    }
  }

  List<SimCard> get getSimInfo {
    return _simCards ?? [];
  }
}

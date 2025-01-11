import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_sim_data/sim_data.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/model/sim_card.dart';

class MobileService extends Cubit<List<SimCard>> {
  List<SimCard>? _simCards;
  MobileService() : super([]) {
    _initialize();
  }

  Future<void> _initialize() async {
    var status = await Permission.phone.status;
    if (status.isGranted) {
      try {
        var data = await SimData().getSimData();
        _simCards = data.map((e) => SimCard.fromInternal(e)).toList();
      } catch (e) {
        // TODO: Show error dialog
      }
    } else {
      status = await Permission.phone.request();
    }
  }

  List<SimCard> get getSimInfo {
    return _simCards ?? [];
  }
}

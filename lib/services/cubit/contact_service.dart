import 'package:bloc/bloc.dart';
import 'package:flutter_contacts/flutter_contacts.dart' as fc;
import 'package:permission_handler/permission_handler.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/utils/utils.dart';

class ContactService extends Cubit<List<Contact>> {
  ContactService() : super([]) {
    _initialize();
  }

  Future<void> _initialize() async {
    if (state.isEmpty) {
      if (await Permission.contacts.request().isGranted) {
        var f_contact = (await fc.FlutterContacts.getContacts(
          withProperties: true,
          withAccounts: true,
          withGroups: true,
          withPhoto: true,
          withThumbnail: true,
        ))
            .toList();
        emit(f_contact.map((e) => Contact.fromInternal(e)).toList());
      }
    }
  }

  List<Contact> filterByStars() {
    return state.where((e) => e.isStarred).toList();
  }

  ContactService getAll() {
    _initialize();
    return this;
  }

  Contact findByNumber(String number) {
    return state.firstWhere(
      (element) => element.phones.any(
        (e) {
          return normalizePhoneNumber(e) == normalizePhoneNumber(number);
        },
      ),
    );
  }
}

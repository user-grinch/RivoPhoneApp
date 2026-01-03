import 'dart:typed_data';
import 'package:flutter_contacts/flutter_contacts.dart' as lib;
import 'package:phone_numbers_parser/phone_numbers_parser.dart' as pnp;
import 'package:sticky_az_list/sticky_az_list.dart';

class Contact extends TaggedItem {
  String id;
  String name;
  Uint8List? photo;
  bool isStarred;
  List<pnp.PhoneNumber> numbers;
  List<lib.Email> emails;
  List<lib.Address> addresses;
  List<lib.Organization> organizations;
  List<lib.Website> websites;
  List<lib.SocialMedia> socialMedias;
  List<lib.Event> events;
  List<lib.Note> notes;
  List<lib.Account> accounts;
  List<lib.Group> groups;

  Contact({
    required this.id,
    required this.name,
    this.photo,
    this.isStarred = false,
    this.numbers = const [],
    this.emails = const [],
    this.addresses = const [],
    this.organizations = const [],
    this.websites = const [],
    this.socialMedias = const [],
    this.events = const [],
    this.notes = const [],
    this.accounts = const [],
    this.groups = const [],
  });

  // TODO: Needs more work
  factory Contact.fromInternal(lib.Contact contact, {String? countryCode}) {
    String displayName = contact.displayName;

    if (displayName.isEmpty) {
      displayName = "Unknown";
    }

    return Contact(
      id: contact.id,
      name: displayName,
      photo: contact.photo ?? contact.thumbnail,
      isStarred: contact.isStarred,
      numbers: contact.phones.map((phone) {
        try {
          return pnp.PhoneNumber.parse(phone.number,
              callerCountry: countryCode != null
                  ? pnp.IsoCode.values.byName(countryCode.toUpperCase())
                  : null);
        } catch (e) {
          // return a dummy phone number
          return pnp.PhoneNumber.parse('0');
        }
      }).toList(),
      emails: contact.emails,
      addresses: contact.addresses,
      organizations: contact.organizations,
      websites: contact.websites,
      socialMedias: contact.socialMedias,
      events: contact.events,
      notes: contact.notes,
      accounts: contact.accounts,
      groups: contact.groups,
    );
  }

  lib.Contact toInternal() {
    return lib.Contact(
      id: id,
      displayName: name,
      thumbnail: photo,
      photo: photo,
      name: lib.Name(
        first: name.split(' ')[0],
        middle: name.split(' ').length > 2 ? name.split(' ')[1] : '',
        last: name.split(' ').last,
      ),
      phones: numbers.map((p) => lib.Phone(p.international)).toList(),
      emails: emails,
      addresses: addresses,
      organizations: organizations,
      websites: websites,
      socialMedias: socialMedias,
      events: events,
      notes: notes,
      accounts: accounts,
      groups: groups,
      isStarred: isStarred,
    );
  }

  @override
  String sortName() => name;
}

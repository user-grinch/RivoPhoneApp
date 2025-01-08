import 'dart:typed_data';
import 'package:flutter_contacts/flutter_contacts.dart' as lib;

class Contact {
  String id;
  String displayName;
  Uint8List? thumbnail;
  Uint8List? photo;
  bool isStarred;
  String fullName;
  List<String> phones;
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
    required this.displayName,
    this.thumbnail,
    this.photo,
    this.isStarred = false,
    required this.fullName,
    this.phones = const [],
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
  factory Contact.fromInternal(lib.Contact contact) {
    return Contact(
      id: contact.id,
      displayName: contact.displayName,
      thumbnail: contact.thumbnail,
      photo: contact.photo,
      isStarred: contact.isStarred,
      fullName:
          '${contact.name.first} ${contact.name.middle} ${contact.name.last}',
      phones: contact.phones.map((phone) => phone.normalizedNumber).toList(),
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
      displayName: displayName,
      thumbnail: thumbnail,
      photo: photo,
      name: lib.Name(
        first: fullName.split(' ')[0],
        middle: fullName.split(' ').length > 2 ? fullName.split(' ')[1] : '',
        last: fullName.split(' ').last,
      ),
      phones: phones.map((p) => lib.Phone(p)).toList(),
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
}

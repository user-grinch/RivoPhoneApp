import 'dart:convert';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:m3e_collection/m3e_collection.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/controller/extensions/theme.dart';
import 'package:revo/controller/providers/contact_service.dart';
import 'package:revo/controller/providers/mobile_service.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/controller/providers/activity_service.dart';
import 'package:revo/view/components/num_picker.dart';
import 'package:revo/view/components/qr_popup.dart';
import 'package:revo/view/components/sim_picker.dart';
import 'package:revo/view/components/rounded_icon_btn.dart';
import 'package:revo/view/utils/utils.dart';
import 'package:share_plus/share_plus.dart';

class ContactInfoView extends ConsumerStatefulWidget {
  final Contact contact;
  const ContactInfoView(this.contact, {super.key});

  @override
  ConsumerState<ContactInfoView> createState() => _ContactInfoViewState();
}

class _ContactInfoViewState extends ConsumerState<ContactInfoView> {
  @override
  Widget build(BuildContext context) {
    final c = widget.contact;

    return Scaffold(
      appBar: AppBarM3E(
        leading: IconButton(
          icon: RoundedIconButton(
            FluentIcons.arrow_left_24_regular,
            size: 40,
          ),
          onPressed: () => Navigator.of(context).pop(),
        ),
        centerTitle: false,
        shapeFamily: AppBarM3EShapeFamily.round,
        density: AppBarM3EDensity.regular,
        actions: [
          RoundedIconButton(
            FluentIcons.edit_24_regular,
            size: 40,
            onPressed: () =>
                ref.read(contactServiceProvider.notifier).editContact(c),
          ),
          const SizedBox(width: 8),
        ],
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: Column(
          children: [
            const SizedBox(height: 20),
            _buildProfilePicture(context),
            const SizedBox(height: 20),
            Text(
              c.name,
              style: GoogleFonts.outfit(
                fontSize: 32,
                fontWeight: FontWeight.bold,
                color: context.colorScheme.onSurface,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            _buildQuickActions(),
            const SizedBox(height: 32),
            _buildPhoneSection(context),
            if (c.emails.isNotEmpty) ...[
              const SizedBox(height: 24),
              _buildEmailSection(context)
            ],
            if (c.addresses.isNotEmpty) ...[
              const SizedBox(height: 24),
              _buildAddressSection(context)
            ],
            if (c.organizations.isNotEmpty) ...[
              const SizedBox(height: 24),
              _buildOrgSection(context)
            ],
            if (c.events.isNotEmpty) ...[
              const SizedBox(height: 24),
              _buildEventsSection(context)
            ],
            if (c.websites.isNotEmpty || c.socialMedias.isNotEmpty) ...[
              const SizedBox(height: 24),
              _buildLinksSection(context)
            ],
            if (c.notes.isNotEmpty) ...[
              const SizedBox(height: 24),
              _buildNotesSection(context)
            ],
            const SizedBox(height: 24),
            _buildExternalAppsSection(context),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  Widget _buildPhoneSection(BuildContext context) {
    final phones = widget.contact.phones;
    return _buildCard(
      title: "Phone Numbers",
      child: phones.isEmpty
          ? const Padding(
              padding: EdgeInsets.all(24), child: Text("No phone numbers"))
          : Column(
              children: List.generate(phones.length, (index) {
                return Column(
                  children: [
                    _buildPhoneItem(context, phones[index]),
                    if (index < phones.length - 1) _buildDivider(),
                  ],
                );
              }),
            ),
    );
  }

  Widget _buildEmailSection(BuildContext context) {
    return _buildCard(
      title: "Email Addresses",
      child: Column(
        children: widget.contact.emails.asMap().entries.map((e) {
          return Column(
            children: [
              _buildInfoTile(
                context,
                icon: FluentIcons.mail_24_regular,
                title: e.value.address,
                subtitle: e.value.label.name.toUpperCase(),
                // onTap: () => ActivityService().sendEmail(e.value.address),
              ),
              if (e.key < widget.contact.emails.length - 1) _buildDivider(),
            ],
          );
        }).toList(),
      ),
    );
  }

  Widget _buildAddressSection(BuildContext context) {
    return _buildCard(
      title: "Addresses",
      child: Column(
        children: widget.contact.addresses.asMap().entries.map((e) {
          return Column(
            children: [
              _buildInfoTile(
                context,
                icon: FluentIcons.location_24_regular,
                title: e.value.address,
                subtitle: e.value.label.name.toUpperCase(),
                // onTap: () => ActivityService().openMap(e.value.address),
              ),
              if (e.key < widget.contact.addresses.length - 1) _buildDivider(),
            ],
          );
        }).toList(),
      ),
    );
  }

  Widget _buildOrgSection(BuildContext context) {
    return _buildCard(
      title: "Organization",
      child: Column(
        children: widget.contact.organizations.asMap().entries.map((e) {
          final org = e.value;
          return Column(
            children: [
              _buildInfoTile(
                context,
                icon: FluentIcons.building_24_regular,
                title: org.company,
                subtitle: org.title.isNotEmpty ? org.title : "Work",
              ),
              if (e.key < widget.contact.organizations.length - 1)
                _buildDivider(),
            ],
          );
        }).toList(),
      ),
    );
  }

  Widget _buildEventsSection(BuildContext context) {
    return _buildCard(
      title: "Important Dates",
      child: Column(
        children: widget.contact.events.asMap().entries.map((e) {
          final event = e.value;
          final dateStr =
              "${event.month}/${event.day}${event.year != null ? "/${event.year}" : ""}";
          return Column(
            children: [
              _buildInfoTile(
                context,
                icon: FluentIcons.calendar_ltr_24_regular,
                title: dateStr,
                subtitle: event.label.name.toUpperCase(),
              ),
              if (e.key < widget.contact.events.length - 1) _buildDivider(),
            ],
          );
        }).toList(),
      ),
    );
  }

  Widget _buildLinksSection(BuildContext context) {
    final hasWeb = widget.contact.websites.isNotEmpty;

    return _buildCard(
      title: "Web & Social",
      child: Column(
        children: [
          ...widget.contact.socialMedias
              .asMap()
              .entries
              .map((e) => Column(children: [
                    _buildInfoTile(context,
                        icon: FontAwesomeIcons.shareNodes,
                        title: e.value.userName,
                        subtitle: e.value.label.name.toUpperCase()),
                    if (e.key < widget.contact.socialMedias.length - 1 ||
                        hasWeb)
                      _buildDivider(),
                  ])),
          ...widget.contact.websites
              .asMap()
              .entries
              .map((e) => Column(children: [
                    _buildInfoTile(
                      context,
                      icon: FluentIcons.globe_24_regular,
                      title: e.value.url,
                      subtitle: "WEBSITE",
                      // onTap: () => ActivityService().launchUrl(e.value.url)
                    ),
                    if (e.key < widget.contact.websites.length - 1)
                      _buildDivider(),
                  ])),
        ],
      ),
    );
  }

  Widget _buildNotesSection(BuildContext context) {
    return _buildCard(
      title: "Notes",
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Text(
          widget.contact.notes.map((n) => n.note).join("\n"),
          style: GoogleFonts.outfit(
              fontSize: 15, color: context.colorScheme.onSurfaceVariant),
        ),
      ),
    );
  }

  Widget _buildInfoTile(BuildContext context,
      {required IconData icon,
      required String title,
      required String subtitle,
      VoidCallback? onTap}) {
    return ListTile(
      leading: Icon(icon, color: context.colorScheme.primary, size: 22),
      title: Text(title,
          style: GoogleFonts.outfit(fontWeight: FontWeight.w600, fontSize: 16)),
      subtitle: Text(subtitle, style: GoogleFonts.outfit(fontSize: 12)),
      onTap: onTap,
      trailing: onTap != null
          ? const Icon(FluentIcons.open_24_regular, size: 14)
          : null,
    );
  }

  Widget _buildQuickActions() {
    return Wrap(
      alignment: WrapAlignment.center,
      spacing: 16,
      runSpacing: 16,
      children: [
        RoundedIconButton(
          FluentIcons.qr_code_24_regular,
          onPressed: () {
            QrCodePopup(
                    context: context, data: generateVCardString(widget.contact))
                .show();
          },
        ),
        RoundedIconButton(
          FluentIcons.share_24_regular,
          onPressed: () {
            SharePlus.instance.share(ShareParams(
              files: [
                XFile.fromData(utf8.encode(generateVCardString(widget.contact)),
                    mimeType: 'text/plain')
              ],
              fileNameOverrides: ['contact.vcf'],
            ));
          },
        ),
        RoundedIconButton(
          FluentIcons.history_24_regular,
          onPressed: () {
            Navigator.of(context)
                .pushNamed(callHistoryRoute, arguments: widget.contact.phones);
          },
        ),
        RoundedIconButton(
          widget.contact.isStarred
              ? FluentIcons.star_24_filled
              : FluentIcons.star_24_regular,
          onPressed: () {
            setState(
                () => widget.contact.isStarred = !widget.contact.isStarred);
            ref
                .read(contactServiceProvider.notifier)
                .updateContact(contact: widget.contact);
          },
          isActive: widget.contact.isStarred,
        ),
      ],
    );
  }

  Widget _buildProfilePicture(BuildContext context) {
    return Container(
      width: 140,
      height: 140,
      decoration: BoxDecoration(
        color: context.colorScheme.secondaryContainer,
        borderRadius: BorderRadius.circular(28),
        image: widget.contact.photo != null
            ? DecorationImage(
                image: MemoryImage(widget.contact.photo!), fit: BoxFit.cover)
            : null,
      ),
      child: widget.contact.photo == null
          ? Icon(FluentIcons.person_24_filled,
              size: 80, color: context.colorScheme.onSecondaryContainer)
          : null,
    );
  }

  Widget _buildExternalAppsSection(BuildContext context) {
    return _buildCard(
      title: "External Apps",
      child: Column(
        children: [
          _buildAppTile(context, FontAwesomeIcons.telegramPlane, 'Telegram',
              () {
            NumberPicker(
                context: context,
                numbers: widget.contact.phones,
                onTap: (num) => ActivityService().openTelegram(num)).show();
          }),
          _buildDivider(),
          _buildAppTile(context, FluentIcons.video_24_regular, 'Video Call',
              () {
            NumberPicker(
                context: context,
                numbers: widget.contact.phones,
                onTap: (num) => ActivityService().makeVideoCall(num)).show();
          }),
          _buildDivider(),
          _buildAppTile(context, FontAwesomeIcons.whatsapp, 'WhatsApp', () {
            NumberPicker(
                context: context,
                numbers: widget.contact.phones,
                onTap: (num) => ActivityService().openWhatsApp(num)).show();
          }),
        ],
      ),
    );
  }

  Widget _buildCard({required String title, required Widget child}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 8, bottom: 12),
          child: Text(title,
              style: GoogleFonts.outfit(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: context.colorScheme.primary)),
        ),
        Container(
          clipBehavior: Clip.antiAlias,
          width: double.infinity,
          decoration: BoxDecoration(
            color: context.colorScheme.secondaryContainer.withOpacity(0.3),
            borderRadius: BorderRadius.circular(28),
          ),
          child: child,
        ),
      ],
    );
  }

  Widget _buildDivider() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Divider(
          height: 1,
          thickness: 1,
          color: context.colorScheme.onSecondaryContainer.withOpacity(0.05)),
    );
  }

  Widget _buildPhoneItem(BuildContext context, String phone) {
    final simCards = ref.watch(getSimInfoProvider);
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      title: Text(phone,
          style: GoogleFonts.outfit(fontWeight: FontWeight.w600, fontSize: 17)),
      subtitle: const Text("Mobile"),
      trailing: Wrap(
        spacing: 12,
        children: [
          RoundedIconButton(
            FluentIcons.call_24_filled,
            size: 40,
            onPressed: () {
              simCards.whenData((value) =>
                  SimPicker(context: context, simCards: value, number: phone)
                      .show());
            },
          ),
          RoundedIconButton(
            FluentIcons.chat_24_filled,
            size: 40,
            onPressed: () => ActivityService().sendSMS(phone),
          ),
        ],
      ),
    );
  }

  Widget _buildAppTile(
      BuildContext context, IconData icon, String label, VoidCallback onTap) {
    return ListTile(
      leading: Icon(icon, size: 22, color: context.colorScheme.primary),
      title:
          Text(label, style: GoogleFonts.outfit(fontWeight: FontWeight.w500)),
      trailing: const Icon(FluentIcons.chevron_right_24_regular, size: 16),
      onTap: onTap,
    );
  }
}

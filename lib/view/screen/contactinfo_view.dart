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
    return Scaffold(
      appBar: AppBarM3E(
        leading: IconButton(
          icon: RoundedIconButton(FluentIcons.arrow_left_24_regular),
          onPressed: () => Navigator.of(context).pop(),
        ),
        centerTitle: false,
        shapeFamily: AppBarM3EShapeFamily.round,
        density: AppBarM3EDensity.regular,
        actions: [
          RoundedIconButton(
            FluentIcons.edit_24_regular,
            onTap: () => ref
                .read(contactServiceProvider.notifier)
                .editContact(widget.contact),
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
              widget.contact.fullName,
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
            _buildContactInfoSection(context),
            const SizedBox(height: 24),
            _buildExternalAppsSection(context),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  Widget _buildQuickActions() {
    return Wrap(
      alignment: WrapAlignment.center,
      spacing: 16,
      runSpacing: 16,
      children: [
        _buildCircularAction(FluentIcons.qr_code_24_regular, 'QR Code', () {
          QrCodePopup(
                  context: context, data: generateVCardString(widget.contact))
              .show();
        }),
        _buildCircularAction(FluentIcons.share_24_regular, 'Share', () {
          SharePlus.instance.share(ShareParams(
            files: [
              XFile.fromData(utf8.encode(generateVCardString(widget.contact)),
                  mimeType: 'text/plain')
            ],
            fileNameOverrides: ['contact.vcf'],
          ));
        }),
        _buildCircularAction(FluentIcons.history_24_regular, 'History', () {
          Navigator.of(context)
              .pushNamed(callHistoryRoute, arguments: widget.contact.phones);
        }),
        _buildCircularAction(
          widget.contact.isStarred
              ? FluentIcons.star_24_filled
              : FluentIcons.star_24_regular,
          'Favorite',
          () {
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

  Widget _buildCircularAction(IconData icon, String label, VoidCallback onTap,
      {bool isActive = false}) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        GestureDetector(
          onTap: onTap,
          child: Container(
            width: 60,
            height: 60,
            decoration: BoxDecoration(
              color: isActive
                  ? context.colorScheme.primaryContainer
                  : context.colorScheme.secondaryContainer.withAlpha(150),
              shape: BoxShape.circle,
            ),
            child: Icon(icon,
                color: isActive
                    ? context.colorScheme.onPrimaryContainer
                    : context.colorScheme.onSurfaceVariant),
          ),
        ),
        const SizedBox(height: 8),
        Text(label,
            style:
                GoogleFonts.outfit(fontSize: 12, fontWeight: FontWeight.w500)),
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

  Widget _buildContactInfoSection(BuildContext context) {
    final phones = widget.contact.phones;
    return _buildExpressiveCard(
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

  Widget _buildExternalAppsSection(BuildContext context) {
    return _buildExpressiveCard(
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

  Widget _buildExpressiveCard({required String title, required Widget child}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 8, bottom: 12),
          child: Text(title,
              style: GoogleFonts.outfit(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: context.colorScheme.primary)),
        ),
        Container(
          clipBehavior: Clip.antiAlias,
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
          _buildActionIcon(FluentIcons.call_24_filled, () {
            simCards.whenData((value) =>
                SimPicker(context: context, simCards: value, number: phone)
                    .show());
          }),
          _buildActionIcon(FluentIcons.chat_24_filled,
              () => ActivityService().sendSMS(phone)),
        ],
      ),
    );
  }

  Widget _buildAppTile(
      BuildContext context, IconData icon, String label, VoidCallback onTap) {
    return ListTile(
      leading: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
            color: context.colorScheme.surface,
            borderRadius: BorderRadius.circular(12)),
        child: Icon(icon, size: 20, color: context.colorScheme.primary),
      ),
      title:
          Text(label, style: GoogleFonts.outfit(fontWeight: FontWeight.w500)),
      trailing: const Icon(FluentIcons.chevron_right_24_regular, size: 16),
      onTap: onTap,
    );
  }

  Widget _buildActionIcon(IconData icon, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
            color: context.colorScheme.surface,
            borderRadius: BorderRadius.circular(14)),
        child: Icon(icon, size: 20, color: context.colorScheme.primary),
      ),
    );
  }
}

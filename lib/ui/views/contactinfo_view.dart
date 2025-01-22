import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/constants/routes.dart';
import 'package:revo/extentions/theme.dart';
import 'package:revo/model/contact.dart';
import 'package:revo/services/activity_service.dart';
import 'package:revo/services/cubit/contact_service.dart';
import 'package:revo/ui/sim_choose_popup.dart';
import 'package:revo/utils/rounded_icon_btn.dart';
import 'package:revo/utils/share.dart';
import 'package:share_plus/share_plus.dart';

class ContactInfoView extends StatefulWidget {
  final Contact contact;
  const ContactInfoView(this.contact, {super.key});

  @override
  State<ContactInfoView> createState() => _ContactInfoViewState();
}

class _ContactInfoViewState extends State<ContactInfoView> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SingleChildScrollView(
        padding: EdgeInsets.only(
            top: MediaQuery.of(context).padding.top + 100, left: 16, right: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            _buildProfilePicture(context),
            const SizedBox(height: 16),
            Text(
              widget.contact.fullName,
              style: context.textTheme.headlineSmall?.copyWith(
                color: context.colorScheme.onSurface,
                fontWeight: FontWeight.bold,
              ),
              textAlign: TextAlign.center,
            ),

            // Action buttons
            const SizedBox(height: 16),
            Wrap(
              alignment: WrapAlignment.center,
              spacing: 16,
              runSpacing: 16,
              children: [
                RoundedIconButton(
                  context,
                  size: 40,
                  icon: Icons.qr_code,
                  text: 'QR Code',
                  onTap: () {
                    Navigator.of(context).pushNamed(
                      qrShareRoute,
                      arguments: generateVCardString(widget.contact),
                    );
                  },
                ),
                RoundedIconButton(
                  context,
                  icon: Icons.share,
                  size: 40,
                  text: 'Share',
                  onTap: () {
                    Share.shareXFiles([
                      XFile.fromData(
                          utf8.encode(generateVCardString(widget.contact)),
                          mimeType: 'text/plain')
                    ], fileNameOverrides: [
                      'contact.vcf'
                    ]);
                  },
                ),
                RoundedIconButton(
                  context,
                  icon:
                      widget.contact.isStarred ? Icons.star : Icons.star_border,
                  size: 40,
                  text: 'Favourite',
                  onTap: () {
                    setState(
                      () {
                        widget.contact.isStarred = !widget.contact.isStarred;
                      },
                    );
                    context
                        .read<ContactService>()
                        .updateContact(contact: widget.contact);
                  },
                ),
                RoundedIconButton(
                  context,
                  icon: Icons.edit,
                  size: 40,
                  text: 'Edit',
                  onTap: () async {
                    await context
                        .read<ContactService>()
                        .editContact(widget.contact);
                  },
                ),
              ],
            ),

            const SizedBox(height: 16),
            _buildContactInfoSection(context),
            const SizedBox(height: 16),
            _buildAdditionalDetailsSection(context),
            const SizedBox(height: 24),
            _buildFlatOption(context, Icons.history, 'Call History', () {
              Navigator.of(context).pushNamed(callHistoryRoute,
                  arguments: widget.contact.phones.map(
                    (e) {
                      return e;
                    },
                  ).toList());
            }),
            const SizedBox(height: 50),
          ],
        ),
      ),
    );
  }

  Widget _buildContactInfoSection(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (widget.contact.phones.isNotEmpty)
          ...widget.contact.phones
              .map((phone) => _buildPhoneWithActionIcons(context, phone)),
      ],
    );
  }

  Widget _buildPhoneWithActionIcons(BuildContext context, var phone) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Row(
        children: [
          Expanded(
            child: Text(
              phone,
              style: GoogleFonts.cabin(
                textStyle: context.textTheme.bodyLarge,
                color: context.colorScheme.onSurface,
              ),
            ),
          ),
          Wrap(
            spacing: 8,
            children: [
              RoundedIconButton(
                context,
                icon: Icons.phone,
                onTap: () {
                  showDialog(
                    context: context,
                    builder: (context) => simChooserDialog(context, phone),
                  );
                },
                size: 35,
              ),
              RoundedIconButton(
                context,
                icon: Icons.sms_outlined,
                onTap: () {
                  ActivityService().sendSMS(phone);
                },
                size: 35,
              ),
              RoundedIconButton(
                context,
                icon: Icons.video_call,
                onTap: () {
                  ActivityService().makeVideoCall(phone);
                },
                size: 35,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildProfilePicture(BuildContext context) {
    return Positioned(
      top: 150,
      child: CircleAvatar(
        backgroundColor: context.colorScheme.primaryContainer,
        radius: 120,
        backgroundImage: widget.contact.photo != null
            ? MemoryImage(widget.contact.photo!)
            : null,
        child: widget.contact.photo == null
            ? Icon(
                Icons.person,
                size: 100,
                color: context.colorScheme.onSurface,
              )
            : null,
      ),
    );
  }

  Widget _buildFlatOption(
    BuildContext context,
    IconData icon,
    String label,
    Function()? onClick,
  ) {
    return ListTile(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(15),
      ),
      contentPadding: EdgeInsets.zero,
      leading: Container(
        width: 40,
        height: 40,
        decoration: BoxDecoration(
          color: context.colorScheme.primaryContainer.withAlpha(25),
          shape: BoxShape.circle,
        ),
        child: Icon(icon, color: context.colorScheme.onSurface, size: 20),
      ),
      title: Text(
        label,
        style: GoogleFonts.cabin(
          textStyle: context.textTheme.bodyLarge,
          color: context.colorScheme.onSurface,
        ),
      ),
      onTap: onClick,
    );
  }

  Widget _buildAdditionalDetailsSection(BuildContext context) {
    // return Column(
    //   crossAxisAlignment: CrossAxisAlignment.start,
    //   children: [
    //     if (widget.contact.notes.isNotEmpty)
    //       _buildDetail(context, 'Notes', widget.contact.notes.first.note),
    //     if (widget.contact.groups.isNotEmpty)
    //       _buildDetail(context, 'Groups', widget.contact.groups.first.name),
    //     if (widget.contact.events.isNotEmpty)
    //       _buildDetail(
    //           context, 'Birthday', widget.contact.events.first),
    //   ],
    // );
    return Container();
  }

  Widget _buildDetail(BuildContext context, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Text(
            '$label:',
            style: context.textTheme.bodyLarge?.copyWith(
                fontWeight: FontWeight.bold,
                color: context.colorScheme.onSurface),
          ),
          const SizedBox(width: 8),
          Text(
            value,
            style: GoogleFonts.cabin(
              textStyle: context.textTheme.bodyLarge,
              color: context.colorScheme.onSurface,
            ),
          ),
        ],
      ),
    );
  }
}

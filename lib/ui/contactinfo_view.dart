import 'package:flutter/material.dart';
import 'package:flutter_contacts/contact.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revo/extentions/theme.dart';

class ContactInfoView extends StatelessWidget {
  final Contact contact;
  const ContactInfoView(this.contact, {super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          // Background image
          Container(
            height: 250,
            decoration: BoxDecoration(
              color: context.colorScheme.secondaryContainer,
              image: const DecorationImage(
                image: AssetImage('assets/contact_background.jpg'),
                fit: BoxFit.cover,
              ),
            ),
          ),
          SingleChildScrollView(
            padding: EdgeInsets.only(
                top: MediaQuery.of(context).padding.top + 100,
                left: 16,
                right: 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                _buildProfilePicture(context),
                const SizedBox(height: 16),
                Text(
                  '${contact.name.first} ${contact.name.last}',
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
                    _buildActionIcon(context, Icons.star, 'Favourite'),
                    _buildActionIcon(context, Icons.qr_code, 'QR Code'),
                    _buildActionIcon(context, Icons.share, 'Share'),
                    _buildActionIcon(context, Icons.edit, 'Edit'),
                  ],
                ),

                const SizedBox(height: 16),
                _buildContactInfoSection(context),
                const SizedBox(height: 16),
                _buildAdditionalDetailsSection(context),
                const SizedBox(height: 24),
                _buildFlatOption(context, Icons.history, 'Call History'),
                const SizedBox(height: 16),
                _buildFlatOption(context, Icons.music_note, 'Change Ringtone'),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildContactInfoSection(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Display all phone numbers
        if (contact.phones.isNotEmpty)
          ...contact.phones
              .map((phone) => _buildPhoneWithActionIcons(context, phone)),
      ],
    );
  }

  Widget _buildPhoneWithActionIcons(BuildContext context, var phone) {
    return Padding(
      padding:
          const EdgeInsets.only(bottom: 16), // Padding between phone numbers
      child: Row(
        children: [
          // Phone number text
          Expanded(
            child: Text(
              phone.number,
              style: GoogleFonts.cabin(
                textStyle: context.textTheme.bodyLarge,
                color: context.colorScheme.primary,
              ),
            ),
          ),
          // Action icons (Call, Message, Video)
          Wrap(
            spacing: 8,
            children: [
              _buildLargeActionIcon(context, Icons.phone, 'Call'),
              _buildLargeActionIcon(context, Icons.message, 'Message'),
              _buildLargeActionIcon(context, Icons.video_call, 'Video'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildLargeActionIcon(
      BuildContext context, IconData icon, String label) {
    return Container(
      width: 40,
      height: 40,
      decoration: BoxDecoration(
        color: context.colorScheme.primary.withOpacity(0.1),
        shape: BoxShape.circle,
      ),
      child: Icon(icon,
          color: context.colorScheme.primary, size: 24), // Increased size
    );
  }

  Widget _buildProfilePicture(BuildContext context) {
    return Positioned(
      top: 150,
      child: CircleAvatar(
        radius: 120,
        backgroundImage: contact.photoOrThumbnail != null
            ? MemoryImage(contact.photoOrThumbnail!)
            : null,
        child: contact.photoOrThumbnail == null
            ? Icon(
                Icons.person,
                size: 100,
                color: context.colorScheme.onPrimaryContainer,
              )
            : null,
      ),
    );
  }

  Widget _buildActionIcon(BuildContext context, IconData icon, String label) {
    return Column(
      children: [
        Container(
          width: 45,
          height: 45,
          decoration: BoxDecoration(
            color: context.colorScheme.primary.withOpacity(0.1),
            shape: BoxShape.circle,
          ),
          child: Icon(icon, color: context.colorScheme.primary, size: 25),
        ),
        const SizedBox(height: 8),
        Text(
          label,
          style: GoogleFonts.cabin(
            textStyle: context.textTheme.bodyLarge,
            color: context.colorScheme.primary,
            fontSize: 12,
          ),
        ),
        const SizedBox(height: 16),
      ],
    );
  }

  Widget _buildFlatOption(BuildContext context, IconData icon, String label) {
    return ListTile(
      contentPadding: EdgeInsets.zero, // Remove default padding for consistency
      leading: Container(
        width: 40,
        height: 40,
        decoration: BoxDecoration(
          color: context.colorScheme.primary.withOpacity(0.1),
          shape: BoxShape.circle,
        ),
        child: Icon(icon, color: context.colorScheme.primary, size: 20),
      ),
      title: Text(
        label,
        style: GoogleFonts.cabin(
          textStyle: context.textTheme.bodyLarge,
          color: context.colorScheme.primary,
        ),
      ),
      onTap: () {},
    );
  }

  Widget _buildAdditionalDetailsSection(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (contact.notes.isNotEmpty)
          _buildDetail(context, 'Notes', contact.notes.first.note),
        if (contact.groups.isNotEmpty)
          _buildDetail(context, 'Groups', contact.groups.first.name),
        if (contact.events.isNotEmpty)
          _buildDetail(context, 'Birthday', contact.events.first.customLabel),
      ],
    );
  }

  Widget _buildDetail(BuildContext context, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Text(
            '$label:',
            style: context.textTheme.bodyLarge
                ?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(width: 8),
          Text(
            value,
            style: GoogleFonts.cabin(
              textStyle: context.textTheme.bodyLarge,
              color: context.colorScheme.primary,
            ),
          ),
        ],
      ),
    );
  }
}

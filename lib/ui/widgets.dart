import 'package:flutter/material.dart';

import '../core/highlights.dart';
import 'colors.dart';

/// Page padding and scrolling shared by every screen.
class ArgusPage extends StatelessWidget {
  const ArgusPage({super.key, required this.children, this.bottom});

  final List<Widget> children;

  /// The main action, pinned under the content.
  final Widget? bottom;

  @override
  Widget build(BuildContext context) => SafeArea(
    child: Column(
      children: [
        Expanded(
          child: ListView(padding: const EdgeInsets.fromLTRB(20, 12, 20, 20), children: children),
        ),
        if (bottom != null) Padding(padding: const EdgeInsets.fromLTRB(20, 0, 20, 16), child: bottom),
      ],
    ),
  );
}

class Gap extends StatelessWidget {
  const Gap([this.size = 16, Key? key]) : super(key: key);
  final double size;

  @override
  Widget build(BuildContext context) => SizedBox(height: size);
}

/// A big selectable row with a radio mark, for the title and language choices.
class ChoiceRow extends StatelessWidget {
  const ChoiceRow({super.key, required this.label, required this.selected, required this.onTap});

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 10),
    child: Material(
      color: selected ? ArgusColors.pineTint : ArgusColors.paper,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(color: selected ? ArgusColors.pine : ArgusColors.line, width: 2),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: onTap,
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: 60),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              children: [
                Icon(selected ? Icons.radio_button_checked : Icons.radio_button_unchecked, color: ArgusColors.pine, size: 28),
                const SizedBox(width: 14),
                Expanded(child: Text(label, style: Theme.of(context).textTheme.bodyLarge)),
              ],
            ),
          ),
        ),
      ),
    ),
  );
}

/// A status line on the home screen: what it is, and whether it works.
class StatusRow extends StatelessWidget {
  const StatusRow({super.key, required this.label, required this.ok, required this.stateText, this.onFix, this.fixLabel});

  final String label;
  final bool ok;
  final String stateText;
  final VoidCallback? onFix;
  final String? fixLabel;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 8),
    child: Row(
      children: [
        Icon(ok ? Icons.check_circle : Icons.error, color: ok ? ArgusColors.pine : ArgusColors.alarm, size: 30),
        const SizedBox(width: 12),
        Expanded(child: Text(label, style: Theme.of(context).textTheme.bodyLarge)),
        if (!ok && onFix != null)
          TextButton(onPressed: onFix, style: TextButton.styleFrom(minimumSize: const Size(64, 48)), child: Text(fixLabel ?? stateText, style: const TextStyle(fontSize: 18)))
        else
          Text(stateText, style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: ok ? ArgusColors.pine : ArgusColors.alarm)),
      ],
    ),
  );
}

/// The message as received, with the dangerous parts marked. Links are shown, never clickable.
class MarkedMessage extends StatelessWidget {
  const MarkedMessage({super.key, required this.text, required this.marks});

  final String text;
  final List<Mark> marks;

  @override
  Widget build(BuildContext context) {
    final spans = <TextSpan>[];
    var at = 0;
    for (final m in marks) {
      if (m.start < at || m.end > text.length) continue;
      if (m.start > at) spans.add(TextSpan(text: text.substring(at, m.start)));
      spans.add(TextSpan(
        text: text.substring(m.start, m.end),
        style: const TextStyle(backgroundColor: ArgusColors.marker, fontWeight: FontWeight.w700, decoration: TextDecoration.underline),
      ));
      at = m.end;
    }
    if (at < text.length) spans.add(TextSpan(text: text.substring(at)));
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(color: ArgusColors.paper, borderRadius: BorderRadius.circular(16), border: Border.all(color: ArgusColors.line)),
      child: Text.rich(TextSpan(children: spans), style: const TextStyle(fontSize: 20, color: ArgusColors.ink, height: 1.45)),
    );
  }
}

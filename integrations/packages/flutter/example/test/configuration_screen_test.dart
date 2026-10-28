import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:cmp_consent_example/consent_example.dart';

void main() {
  testWidgets('registration cannot open before CMP is configured', (
    tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: ConsentExample()));
    final button = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, 'Đăng ký với consent nhúng'),
    );
    expect(button.onPressed, isNull);
  });
}

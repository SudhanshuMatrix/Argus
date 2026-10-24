# Test samples

**All 20 messages here are synthetic.** They were written for testing on Oct 3, 2026.
Every name, phone number, account number, bank and link is made up (for example
0801 234 5678, bit.ly/xyz123, "Zentra Bank"). None of them is a real message.

- `scam/`: 10 scams (should become SCAM or CAREFUL)
- `normal/`: 10 normal messages, including hard cases (should stay NONE; the real OTP should become OTP)

One message per `.txt` file. Add real samples later with the same naming (`NN_short_name.txt`),
and remove anything that identifies a real person before committing.

The dev flavor packages this folder as assets so "Run all samples" on the debug screen can run them on a phone.

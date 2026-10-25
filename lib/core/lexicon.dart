/// Shared word lists for the rules: English (Nigerian and Indian usage), Hinglish and Hindi.
/// Each entry is a regex fragment matched as a whole word (see patterns.dart).
library;

/// Secrets nobody should ask for.
const secretWords = [
  'otp', 'pin', 'upi pin', 'mpin', 'bvn', 'nin', 'password', 'code', 'token', 'cvv', 'card number', 'card details',
  'ओटीपी', 'पिन', 'पासवर्ड', 'कोड',
];

/// "Hand it over" verbs, in English, Hinglish and Hindi.
const handOverVerbs = [
  'send', 'share', 'give', 'forward', 'tell', 'confirm', 'provide', 'enter', 'type', 'reply', 'drop',
  'bhejo', 'bhej do', 'bhejiye', 'bhejein', 'batao', 'bataiye', 'bata do', 'share karo', 'share karein', 'share kijiye',
  'भेजें', 'भेजो', 'भेज दो', 'भेजिए', 'भेजिये', 'बताएं', 'बताएँ', 'बताइए', 'बताइये', 'बताओ', 'बता दो', 'शेयर करें', 'शेयर करो', 'दीजिए', 'दें',
];

/// Negations that come before an English verb ("do not share") or before a Hindi or
/// Hinglish verb ("साझा न करें", "mat batana").
const englishNegations = ['do not', "don't", 'dont', 'never', 'not to', 'no'];
const hindiNegations = ['न', 'ना', 'मत', 'नहीं', 'mat', 'na', 'nahi', 'nahin'];

/// Verbs a Hindi or Hinglish negation can come before.
const hindiVerbs = [
  'करें', 'करना', 'करो', 'कीजिए', 'बताएं', 'बताएँ', 'बताइए', 'बताओ', 'बताना', 'भेजें', 'भेजो', 'भेजना', 'दें', 'दीजिए', 'देना',
  'karein', 'karna', 'karo', 'batana', 'batao', 'bhejna', 'bhejo', 'dena', 'share',
];

/// "Send money" verbs.
const payVerbs = [
  'send', 'pay', 'transfer', 'deposit', 'remit', 'credit', 'invest',
  'bhejo', 'bhej do', 'bhejiye', 'bhejein', 'pay karo', 'pay karein', 'jama karo', 'jama karein', 'transfer karo', 'transfer karein',
  'भेजें', 'भेजो', 'भेज दो', 'भेजिए', 'जमा करें', 'जमा करो', 'ट्रांसफर करें', 'भुगतान करें', 'पे करें',
];

/// Money amounts and money words: naira and rupees, in digits or words.
const amountPattern =
    r'₦|₹|\bN\s?\d|\bNGN\s?\d|\bRs\.?\s?\d|\bINR\s?\d|\bnaira\b|\d+\s?k\b|\bmoney\b|\bfee\b|\bcharges?\b|\bcash\b'
    r'|\brupees?\b|\brupaye\b|\brupaiye\b|\blakh\b|\bcrore\b|\bpaise\b|\bpaisa\b';
const hindiAmountWords = ['रुपये', 'रुपए', 'रुपया', 'पैसे', 'पैसा', 'लाख', 'करोड़', 'शुल्क', 'फीस'];

/// Hurry words.
const urgencyWords = [
  'urgent', 'urgently', 'now', 'immediately', 'today', 'quick', 'quickly', 'fast', 'within 24', 'sharp sharp', 'asap',
  'turant', 'jaldi', 'abhi', 'तुरंत', 'जल्दी', 'अभी', 'आज ही',
];

/// Someone claiming to be a bank, a company or an official.
const officialWords = [
  'dear customer', 'dear valued customer', 'valued customer', 'customer care', 'customer service',
  'customer support', 'cbn', 'efcc', 'central bank', 'government', 'ministry', 'our agent',
  'account officer', 'management', 'hr', 'fraud desk', 'fraud department', 'fraud unit', 'security team',
  // India
  'dear consumer', 'dear user', 'rbi', 'reserve bank', 'sbi', 'hdfc', 'icici', 'axis bank', 'trai', 'cbi', 'police',
  'cyber cell', 'cyber crime', 'income tax', 'customs', 'electricity officer', 'electricity department', 'electricity board',
  'bijli vibhag', 'court', 'narcotics',
  'प्रिय ग्राहक', 'प्रिय उपभोक्ता', 'पुलिस', 'आरबीआई', 'सीबीआई', 'बिजली विभाग', 'बैंक से', 'कस्टम',
];

/// "This is my new number" and its Indian and Hindi versions.
const newNumberPattern =
    r"new number|new line|friend'?s? (?:line|phone|number)|phone (?:fell|spoil|spoilt|got lost|was stolen)|lost my phone|don'?t call (?:the|my) old"
    r'|naya number|nayi sim|purana number|dost ka phone|dost ke phone|phone kho gaya|phone gir gaya|phone kharab'
    r'|नया नंबर|नए नंबर|नई सिम|पुराना नंबर|दोस्त का फोन|दोस्त के फोन|फोन खो गया|फोन गिर गया|फोन खराब';

/// Call verbs for "call this number about your account".
const callVerbs = [
  'call', 'phone', 'contact', 'reach', 'dial', 'whatsapp', 'text', 'call karo', 'call karein', 'sampark karein',
  'कॉल करें', 'कॉल करो', 'संपर्क करें', 'फोन करें',
];

/// Account, card or legal trouble (not just the word "account": real credit alerts stay quiet).
const accountTroubleWords = [
  'fraud', 'fraudulent', 'unusual', 'suspicious', 'unauthorised', 'unauthorized', 'compromised',
  'blocked', 'block', 'restricted', 'suspended', 'closure', 'closed', 'deactivated', 'frozen', 'hacked',
  'disconnected', 'disconnect', 'disconnection', 'expired', 'illegal', 'seized', 'arrest', 'warrant',
  'ब्लॉक', 'बंद हो', 'काट', 'निलंबित', 'गिरफ्तार', 'अवैध',
];

/// Prizes and offers that are too good to be true.
const prizePattern =
    r'prize|promo|you have won|you won|congratulations|lottery|lucky draw|kbc|jackpot'
    r'|बधाई हो|इनाम|लॉटरी|जीती|जीते';

/// Threats used by fake police and "digital arrest" scams.
const threatWords = [
  'arrest', 'arrested', 'warrant', 'money laundering', 'digital arrest', 'illegal', 'drugs', 'case filed', 'fir',
  'गिरफ्तार', 'गिरफ्तारी', 'वारंट', 'केस दर्ज', 'मनी लॉन्ड्रिंग',
];

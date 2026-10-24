# Holdout samples

Messages here are **never** used to tune rules, keywords or prompts. They are only run
("Run holdout samples" on the dev debug screen) and reported, to estimate how Argus does
on messages it was not tuned for.

- `scam/` should become SCAM or CAREFUL; `normal/` should stay NONE (or OTP for a real code).
- One message per `.txt` file, named `NN_short_name.txt`.
- Remove anything that identifies a real person before committing.
- Unit tests don't read this folder on purpose.

## Current set (Oct 4)

10 scam + 10 normal synthetic messages, written by Claude before the first holdout run and
committed before any result was seen. They are not real messages, so they are a weaker test
than real ones; replace or add real messages when you have them. Nothing was tuned on them.

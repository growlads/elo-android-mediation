# Contributing

Thanks for your interest in the Elo Android mediation adapters. This repository hosts first-party adapters that bridge third-party ad networks into the Elo Android SDK.

## Reporting bugs

Please open an issue using the **Bug report** template. Include the adapter and version, the underlying ad network SDK version, the Elo SDK version, Android API level, device, and a minimal repro.

For integration help, use the **Integration question** template instead.

## Security issues

Do **not** open a public issue. See [SECURITY.md](./SECURITY.md).

## Pull requests

PRs are welcome for:

- Adapter source under `adapter-*/`.
- Test utilities under `testkit/`.
- `README.md`, `CHANGELOG.md`, this file, and `.github/`.

The adapter contract is documented in [`ADAPTER_AUTHOR_GUIDE.md`](./ADAPTER_AUTHOR_GUIDE.md). Please read it before proposing a new adapter.

Third-party adapters should ship as separate Gradle artifacts so this repo's release cadence isn't coupled to vendor SDK releases.

## Code of conduct

By participating, you agree to abide by the [Code of Conduct](./CODE_OF_CONDUCT.md).

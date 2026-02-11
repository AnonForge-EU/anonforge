# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in AnonForge, please report it responsibly.

**Do NOT open a public GitHub issue for security vulnerabilities.**

### How to Report

1. **Email**: Send details to the maintainers via GitHub private vulnerability reporting:
   - Go to the [Security tab](https://github.com/AnonForge-EU/anonforge/security) of this repository
   - Click "Report a vulnerability"
   - Provide a detailed description

2. **Include**:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

### Response Timeline

- **Acknowledgment**: Within 48 hours
- **Assessment**: Within 7 days
- **Fix**: As soon as practically possible, targeting 30 days for critical issues

### Scope

The following are in scope:
- Encryption weaknesses (SQLCipher, Keystore usage)
- Data leakage (logs, memory, screenshots)
- Authentication bypass (biometric, PIN)
- Insecure data storage
- API key exposure
- Any OWASP Mobile Top 10 vulnerability

### Out of Scope

- Social engineering attacks
- Vulnerabilities in third-party services (SimpleLogin, etc.)
- Physical access attacks requiring an unlocked device
- Denial of service on the local device

## Security Measures

For details on AnonForge's security architecture, see the [Security section](README.md#%EF%B8%8F-security--privacy) in the README.

## Supported Versions

| Version | Supported |
|---------|-----------|
| Latest release | ✅ |
| Previous releases | ❌ (upgrade recommended) |

Thank you for helping keep AnonForge secure. 🛡️

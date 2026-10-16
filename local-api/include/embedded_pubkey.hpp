// SPDX-License-Identifier: GPL-3.0-or-later
//
// embedded_pubkey.hpp — Manager handshake public key (EC P-256, SPKI PEM).
//
// The matching private key lives in the manager app (SHA256withECDSA).
// Handshake: server sends 401 {challenge} -> client signs SHA256(challenge)
// with its private key -> server verifies with this public key.
// Root (uid 0) connections skip the handshake for curl/adb debugging.

#pragma once

namespace local_api {

inline constexpr char kManagerPubkeyPem[] =
    "-----BEGIN PUBLIC KEY-----\n"
    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAENnhPTdYq4RKw3lfRtIn3m6gblzR2\n"
    "kExfCrwZ4ro++UC5dEGEekc6GGtCPMSL2D1Kdtxkc99rCzB0eFfurTJ0hA==\n"
    "-----END PUBLIC KEY-----\n";

} // namespace local_api

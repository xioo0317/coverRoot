// SPDX-License-Identifier: GPL-3.0-or-later
//
// router.cpp — Request dispatcher implementation (Unix domain socket).
//
// Transport (v1.16.0, TCP removed):
//   default:  abstract "@local_api"                  -> manager main channel
//             /data/local/tmp/local_api.sock (0666)  -> curl / adb debug
//   arg:      "@name" = abstract only; "path" = file socket only (test)
//
// Security, two layers:
//   1. SO_PEERCRED (kernel, cannot be spoofed):
//        uid 0 (root/shell debug)            -> full access, no handshake
//        uid in /data/adb/coverroot/allowed_uids -> handshake required
//        everyone else                       -> 403, closed
//   2. ECDSA P-256 challenge-response (SHA256withECDSA):
//        server sends 401 {"challenge": hex} on accept,
//        client signs SHA256(challenge) with its private key and posts
//        POST /auth {"sig": hex}; verified against the embedded public key.
//
// Protocol: HTTP/1.1 semantics, keep-alive per connection.
//   POST /     {"action":"name"}  -> tools::execute("name")
//   POST /auth {"sig":"hex"}      -> handshake (see above)
//   GET  /debug                  -> tools::debug_info()

#include "router.hpp"
#include "tools.hpp"
#include "version.hpp"
#include "embedded_pubkey.hpp"

#include <nlohmann/json.hpp>

#include <mbedtls/pk.h>
#include <mbedtls/md.h>

#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/un.h>
#include <poll.h>
#include <sys/wait.h>
#include <unistd.h>
#include <signal.h>
#include <cerrno>
#include <cstddef>
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

using json = nlohmann::json;

namespace router {
namespace {

#ifndef SO_PEERCRED
#define SO_PEERCRED 21
#endif

constexpr const char* kAbstractName = "@local_api";
constexpr const char* kDefaultSock  = "/data/local/tmp/local_api.sock";
constexpr const char* kAllowFile    = "/data/adb/coverroot/allowed_uids";
constexpr size_t kMaxHead = 32 * 1024;
constexpr size_t kMaxBody = 1024 * 1024;
constexpr size_t kChallengeBytes = 32;

// Kernel's struct ucred layout (declared locally to avoid _GNU_SOURCE /
// bionic header differences between host and NDK builds).
struct PeerCred { pid_t pid; uid_t uid; gid_t gid; };

bool send_all(int fd, const char* data, size_t len) {
    while (len > 0) {
        const ssize_t n = ::send(fd, data, len, MSG_NOSIGNAL);
        if (n <= 0) return false;
        data += n;
        len -= static_cast<size_t>(n);
    }
    return true;
}

void respond(int fd, int code, const std::string& body) {
    const char* reason = (code == 200) ? "OK"
                       : (code == 400) ? "Bad Request"
                       : (code == 401) ? "Unauthorized"
                       : (code == 403) ? "Forbidden"
                       : (code == 404) ? "Not Found" : "Error";
    std::ostringstream out;
    out << "HTTP/1.1 " << code << ' ' << reason << "\r\n"
        << "Content-Type: application/json\r\n"
        << "Content-Length: " << body.size() << "\r\n"
        << "Connection: keep-alive\r\n\r\n"
        << body;
    const std::string s = out.str();
    send_all(fd, s.data(), s.size());
}

// Kernel-reported peer uid check: root always, others only via whitelist file.
// LOCAL_API_ALLOW_ALL=1 opens the socket to any local peer (test hosts only;
// production Android never sets it).
bool peer_allowed(int fd, uid_t& out_uid) {
    PeerCred cred{};
    socklen_t len = sizeof(cred);
    if (::getsockopt(fd, SOL_SOCKET, SO_PEERCRED, &cred, &len) != 0) return false;
    out_uid = cred.uid;
    if (cred.uid == 0) return true;
    if (const char* open = ::getenv("LOCAL_API_ALLOW_ALL"); open && open[0] == '1') return true;

    std::ifstream in(kAllowFile);
    if (!in) return false;
    long id = 0;
    while (in >> id) {
        if (id >= 0 && static_cast<uid_t>(id) == cred.uid) return true;
    }
    return false;
}

std::string random_hex(size_t nbytes) {
    static const char* digits = "0123456789abcdef";
    std::string hex;
    hex.reserve(nbytes * 2);
    std::ifstream urandom("/dev/urandom", std::ios::binary);
    char byte = 0;
    for (size_t i = 0; i < nbytes && urandom.get(byte); ++i) {
        hex.push_back(digits[(static_cast<unsigned char>(byte) >> 4) & 0xF]);
        hex.push_back(digits[static_cast<unsigned char>(byte) & 0xF]);
    }
    return hex;
}

bool hex_decode(const std::string& in, std::vector<unsigned char>& out) {
    if (in.size() % 2 != 0) return false;
    out.clear();
    out.reserve(in.size() / 2);
    auto nib = [](char c) -> int {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    };
    for (size_t i = 0; i < in.size(); i += 2) {
        const int hi = nib(in[i]), lo = nib(in[i + 1]);
        if (hi < 0 || lo < 0) return false;
        out.push_back(static_cast<unsigned char>((hi << 4) | lo));
    }
    return true;
}

// Verify SHA256withECDSA over the challenge.
// Both the challenge and the signature arrive hex-encoded; the client signs
// SHA256 of the raw 32 challenge bytes (Java SHA256withECDSA semantics), so
// decode both before hashing / verifying.
bool verify_signature(const std::string& challenge_hex, const std::string& sig_hex) {
    std::vector<unsigned char> sig;
    std::vector<unsigned char> challenge;
    if (!hex_decode(sig_hex, sig) || sig.empty()) return false;
    if (!hex_decode(challenge_hex, challenge) || challenge.empty()) return false;

    unsigned char hash[32];
    const mbedtls_md_info_t* md = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
    if (md == nullptr ||
        mbedtls_md(md, challenge.data(), challenge.size(), hash) != 0) return false;

    mbedtls_pk_context pk;
    mbedtls_pk_init(&pk);
    const bool ok =
        mbedtls_pk_parse_public_key(&pk,
                                    reinterpret_cast<const unsigned char*>(local_api::kManagerPubkeyPem),
                                    sizeof(local_api::kManagerPubkeyPem)) == 0 &&
        mbedtls_pk_verify(&pk, MBEDTLS_MD_NONE, hash, sizeof(hash),
                          sig.data(), sig.size()) == 0;
    mbedtls_pk_free(&pk);
    return ok;
}

// Read the request head, then exactly Content-Length body bytes.
bool read_request(int fd, std::string& method, std::string& path, std::string& body) {
    std::string head;
    char buf[4096];
    size_t hdr_end = std::string::npos;
    while (hdr_end == std::string::npos) {
        if (head.size() > kMaxHead) return false;
        const ssize_t n = ::recv(fd, buf, sizeof(buf), 0);
        if (n <= 0) return false;
        head.append(buf, static_cast<size_t>(n));
        hdr_end = head.find("\r\n\r\n");
    }

    std::istringstream first(head.substr(0, head.find("\r\n")));
    first >> method >> path;
    if (method.empty() || path.empty()) return false;

    size_t content_len = 0;
    size_t pos = head.find("\r\n");
    while (pos != std::string::npos && pos + 2 < hdr_end) {
        const size_t eol = head.find("\r\n", pos + 2);
        const size_t stop = (eol == std::string::npos) ? hdr_end : eol;
        std::string line = head.substr(pos + 2, stop - (pos + 2));
        for (auto& c : line) c = static_cast<char>(::tolower(c));
        if (line.rfind("content-length:", 0) == 0)
            content_len = static_cast<size_t>(::strtoul(line.c_str() + 15, nullptr, 10));
        pos = eol;
    }
    if (content_len > kMaxBody) return false;

    body = head.substr(hdr_end + 4);
    while (body.size() < content_len) {
        const ssize_t n = ::recv(fd, buf, sizeof(buf), 0);
        if (n <= 0) return false;
        body.append(buf, static_cast<size_t>(n));
    }
    body.resize(content_len);
    return true;
}

void dispatch(int fd, const std::string& method, const std::string& path,
              const std::string& body) {
    if (method == "POST" && (path == "/" || path.empty())) {
        json req;
        try {
            req = json::parse(body);
        } catch (...) {
            respond(fd, 400, json{{"status","error"},{"error","invalid JSON body"}}.dump());
            return;
        }
        const std::string action = req.value("action", "");
        if (action.empty()) {
            respond(fd, 400, json{{"status","error"},
                                  {"error","missing 'action' field"},
                                  {"available_actions", tools::list_actions()}}.dump());
            return;
        }
        respond(fd, 200, tools::execute(action));
        return;
    }
    if (method == "GET" && path == "/debug") {
        respond(fd, 200, tools::debug_info());
        return;
    }
    respond(fd, 404, json{{"error","not found"},
                          {"hint","POST / with {\"action\":\"...\"}  |  GET /debug"}}.dump());
}

#ifdef __ANDROID__
// Best-effort SELinux allowance so a non-root app (untrusted_app domain) can
// connect to the abstract socket we bind in the magisk/root domain. Live rules
// vanish on reboot; the daemon re-applies them on every start, so persistence
// is equivalent. Never fatal: ROMs that already allow this just log a warning.
// Runs one magiskpolicy rule. Returns false and fills `detail` with the
// failure mode (exec-not-found / exit code) on failure.
static bool run_policy_bin(const char* bin, const char* rule, std::string& detail) {
    const pid_t pid = ::fork();
    if (pid == 0) {
        // execlp so a bare "magiskpolicy" on PATH works (execl never searches).
        ::execlp(bin, bin, "--live", rule, (char*)nullptr);
        _exit(errno == ENOENT ? 127 : 126);
    }
    if (pid < 0) { detail = "fork failed"; return false; }
    int st = 0;
    if (::waitpid(pid, &st, 0) < 0) { detail = "waitpid failed"; return false; }
    if (WIFEXITED(st)) {
        const int code = WEXITSTATUS(st);
        if (code == 0) return true;
        detail = code == 127 ? "binary not found" : "exit " + std::to_string(code);
        return false;
    }
    detail = "killed by signal " + std::to_string(WTERMSIG(st));
    return false;
}

static void apply_sepolicy_allowance() {
    static const char* kBins[] = {
        "magiskpolicy",                       // Magisk / KSU / APatch on PATH
        "/data/adb/magisk/magiskpolicy",      // Magisk
        "/data/adb/ksu/bin/magiskpolicy",     // KernelSU
        "/data/adb/ap/bin/magiskpolicy",      // APatch
    };
    // Client side: appdomain attribute covers every app domain (untrusted_app,
    // untrusted_app_25..32, platform_app, ...). Actual access control happens
    // in the daemon via SO_PEERCRED uid + ECDSA handshake, so a wide connectto
    // allowance does not weaken security. Server side: the domain the su
    // binary runs in (magisk / ksu / apatch). Rules are applied per-bin and
    // failures are tolerated individually: a ROM without the "apatch" type
    // must not make us report total failure when magisk/ksu rules went through.
    static const char* kRules[] = {
        "allow appdomain magisk unix_stream_socket connectto",
        "allow appdomain ksu unix_stream_socket connectto",
        "allow appdomain apatch unix_stream_socket connectto",
    };
    int applied = 0;
    std::string failures;
    for (const char* bin : kBins) {
        for (const char* rule : kRules) {
            std::string detail;
            if (run_policy_bin(bin, rule, detail)) {
                ++applied;
            } else if (applied == 0) {
                // Only the first binary's failures matter for diagnosis; the
                // later fallback bins are expected to be missing on most ROMs.
                failures += "\n  [" + std::string(bin) + "] " + rule + " -> " + detail;
            }
        }
        if (applied >= static_cast<int>(sizeof(kRules) / sizeof(kRules[0]))) break;  // fully applied
    }
    if (applied > 0) {
        std::cout << local_api::APP_NAME << ": SELinux connectto allowance applied ("
                  << applied << " rule instances)" << std::endl;
    } else {
        std::cerr << "local_api: SELinux connectto allowance NOT applied;"
                     " app connections may be denied. Details:" << failures << std::endl;
    }
}
#endif

// One connection = one client lifecycle (auth once, then keep-alive requests).
void serve_client(int fd) {
    uid_t uid = 0;
    if (!peer_allowed(fd, uid)) {
        respond(fd, 403, json{{"status","error"},
                              {"error","peer not allowed"},
                              {"peer_uid", uid}}.dump());
        ::close(fd);
        return;
    }

    // Layer 2: non-root peers must complete the ECDSA challenge handshake.
    // (LOCAL_API_ALLOW_ALL=1 test mode skips it along with the uid check.)
    const bool trust_all = [] {
        const char* o = ::getenv("LOCAL_API_ALLOW_ALL");
        return o != nullptr && o[0] == '1';
    }();
    const bool need_auth = (uid != 0) && !trust_all;
    bool authenticated = !need_auth;
    std::string challenge;
    if (need_auth) {
        challenge = random_hex(kChallengeBytes);
        respond(fd, 401, json{{"status","auth_required"},
                              {"challenge", challenge}}.dump());
    }

    for (;;) {
        std::string method, path, body;
        if (!read_request(fd, method, path, body)) break;

        if (!authenticated) {
            if (method == "POST" && path == "/auth") {
                std::string sig;
                try {
                    sig = json::parse(body).value("sig", "");
                } catch (...) {}
                if (!sig.empty() && verify_signature(challenge, sig)) {
                    authenticated = true;
                    respond(fd, 200, json{{"status","authenticated"},
                                          {"version", local_api::SERVER_VERSION}}.dump());
                    continue;
                }
                respond(fd, 403, json{{"status","error"},{"error","auth failed"}}.dump());
                break;
            }
            respond(fd, 401, json{{"status","auth_required"},
                                  {"challenge", challenge}}.dump());
            continue;
        }

        dispatch(fd, method, path, body);
    }
    ::close(fd);
}

// Returns "pid N" / "live listener" if something is already serving on this
// endpoint, or "" when the endpoint is free (or holds only a stale socket).
std::string probe_live_instance(const std::string& spec) {
    if (spec.empty()) return "";
    const bool abstract = spec[0] == '@';
    const int fd = ::socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) return "";
    sockaddr_un sa{};
    sa.sun_family = AF_UNIX;
    const size_t name_len = abstract ? spec.size() - 1 : spec.size();
    if (abstract) {
        sa.sun_path[0] = '\0';
        ::memcpy(sa.sun_path + 1, spec.data() + 1, name_len);
    } else {
        ::memcpy(sa.sun_path, spec.c_str(), name_len);
    }
    const socklen_t len = static_cast<socklen_t>(
        offsetof(sockaddr_un, sun_path) + name_len + (abstract ? 1 : 0));
    std::string out;
    if (::connect(fd, reinterpret_cast<sockaddr*>(&sa), len) == 0) {
        PeerCred pc{};
        socklen_t pl = sizeof(pc);
        if (::getsockopt(fd, SOL_SOCKET, SO_PEERCRED, &pc, &pl) == 0 && pc.pid > 0) {
            out = "pid " + std::to_string(pc.pid);
        } else {
            out = "live listener";
        }
    }
    ::close(fd);
    return out;
}

int make_listener(const std::string& spec) {
    const int fd = ::socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) return -1;

    sockaddr_un addr{};
    addr.sun_family = AF_UNIX;
    const bool abstract = !spec.empty() && spec[0] == '@';
    const size_t name_len = abstract ? spec.size() - 1 : spec.size();
    if (name_len >= sizeof(addr.sun_path)) {
        ::close(fd);
        return -1;
    }
    if (abstract) {
        // Android's LocalSocket (ABSTRACT) puts the name at sun_path[1] with a
        // leading NUL at sun_path[0]; match that byte-for-byte or the names
        // never compare equal inside the kernel.
        addr.sun_path[0] = '\0';
        ::memcpy(addr.sun_path + 1, spec.data() + 1, name_len);
    } else {
        // Guarded by probe_live_instance() in run(): reaching here means no
        // live daemon holds this path, so unlink only cleans a stale socket.
        ::unlink(spec.c_str());
        ::memcpy(addr.sun_path, spec.c_str(), name_len);
    }
    const socklen_t addrlen =
        static_cast<socklen_t>(offsetof(sockaddr_un, sun_path) + name_len + (abstract ? 1 : 0));

    if (::bind(fd, reinterpret_cast<sockaddr*>(&addr), addrlen) != 0 ||
        ::listen(fd, 8) != 0) {
        std::cerr << "bind/listen failed: " << spec << "\n";
        ::close(fd);
        return -1;
    }
    if (!abstract)
        ::chmod(spec.c_str(), 0666);
    return fd;
}

} // namespace

int run(const std::string& sock_arg) {
    ::signal(SIGPIPE, SIG_IGN);
#ifdef __ANDROID__
    apply_sepolicy_allowance();
#endif

    std::vector<std::string> endpoints;
    if (sock_arg.empty()) {
        endpoints = {kAbstractName, kDefaultSock};
    } else {
        endpoints = {sock_arg};
    }

    // Single-instance guard. Previously a filesystem endpoint unlinked the old
    // socket before binding, which silently orphaned the running daemon and let
    // duplicate local_api processes pile up across restarts. Now any live
    // instance on a requested endpoint stops us from starting another copy.
    for (const auto& ep : endpoints) {
        const std::string live = probe_live_instance(ep);
        if (live.empty()) continue;
        std::cerr << local_api::APP_NAME << ": already running (" << live << ") on "
                  << ep << " — not starting a second copy. Kill it first with:"
                     " pkill -x local_api" << std::endl;
        return 0;
    }

    std::vector<pollfd> pfds;
    for (const auto& ep : endpoints) {
        const int fd = make_listener(ep);
        if (fd < 0) continue;
        pfds.push_back({fd, POLLIN, 0});
        std::cout << local_api::APP_NAME << " v" << local_api::SERVER_VERSION
                  << " listening on " << ep << std::endl;
    }
    if (pfds.empty()) {
        std::cerr << "no listen socket available\n";
        return 1;
    }

    // Self-check: connect to each abstract listener the way an Android
    // LocalSocket does (leading NUL at sun_path[0], name at [1]). A byte-level
    // name mismatch shows up here on the device instead of in the field.
    for (const auto& ep : endpoints) {
        if (ep.empty() || ep[0] != '@') continue;
        const int cfd = ::socket(AF_UNIX, SOCK_STREAM, 0);
        if (cfd < 0) continue;
        sockaddr_un sa{};
        sa.sun_family = AF_UNIX;
        const size_t n = ep.size() - 1;
        sa.sun_path[0] = '\0';
        ::memcpy(sa.sun_path + 1, ep.data() + 1, n);
        const socklen_t salen =
            static_cast<socklen_t>(offsetof(sockaddr_un, sun_path) + 1 + n);
        if (::connect(cfd, reinterpret_cast<sockaddr*>(&sa), salen) == 0) {
            std::cout << local_api::APP_NAME << ": abstract self-check ok (" << ep << ")"
                      << std::endl;
        } else {
            std::cerr << local_api::APP_NAME << ": abstract self-check FAILED (" << ep
                      << "): errno=" << errno
                      << " — Android clients will not reach this listener" << std::endl;
        }
        ::close(cfd);
    }

    for (;;) {
        if (::poll(pfds.data(), static_cast<nfds_t>(pfds.size()), -1) < 0) {
            if (errno == EINTR) continue;
            return 1;
        }
        for (const auto& p : pfds) {
            if (p.revents & POLLIN) {
                const int c = ::accept(p.fd, nullptr, nullptr);
                if (c >= 0) serve_client(c);
            }
        }
    }
}

} // namespace router

// SPDX-License-Identifier: GPL-3.0-or-later
//
// router.hpp — Request dispatcher / transport layer.
//
// Owns the Unix-domain-socket server: endpoint binding, peer-credential
// authentication, HTTP/1.1-ish parsing and dispatch to tools.cpp.

#pragma once

#include <cstdint>
#include <string>

namespace router {

// Bind listening sockets and block serving.
//   sock_arg empty  -> production endpoints (abstract @local_api + file sock)
//   sock_arg "@x"   -> abstract socket only   (custom / test)
//   sock_arg "path" -> file socket only      (test / adb debug)
// Called once from main().
int run(const std::string& sock_arg = "", uint16_t tcp_port = 0);

} // namespace router

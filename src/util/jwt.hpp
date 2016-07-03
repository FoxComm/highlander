#ifndef ISAAC_UTIL_JWT_H
#define ISAAC_UTIL_JWT_H

#include <memory>
#include <string>

#include <folly/io/IOBuf.h>
#include <folly/Memory.h>
#include <folly/Portability.h>
#include <folly/json.h>
#include <folly/io/async/EventBaseManager.h>

//for checking sig
#include <botan/botan.h>
#include <botan/rsa.h>
#include <botan/pubkey.h>
#include <botan/x509_key.h>

#include <boost/algorithm/string/find_iterator.hpp>

namespace isaac
{
    namespace util
    {
        using parts_iterator = boost::algorithm::split_iterator<const uint8_t*>;
        struct jwt_parts
        {
            parts_iterator header;
            parts_iterator payload;
            parts_iterator signature;
        };

        using public_key = Botan::Public_Key;
        using sig_verifier = Botan::PK_Verifier;
        using sig_verifier_ptr = std::unique_ptr<sig_verifier>;

        std::string base64url_decode(parts_iterator p);
        bool get_jwt_parts(jwt_parts& parts, const unsigned char* data, size_t length);

        bool check_jwt_signature(const jwt_parts& parts, sig_verifier& verifier);
        bool jwt_expired(folly::dynamic& payload);
    }
}

#endif

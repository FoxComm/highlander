#ifndef ISAAC_UTIL_USER_CACHE_H
#define ISAAC_UTIL_USER_CACHE_H

#include <string>
#include <memory>
#include "db/user.hpp"
#include <folly/AtomicHashMap.h>

namespace isaac
{
    namespace db
    {
        using key = std::size_t;
        struct user_entry 
        {
            int ratchet;
        };

        using user_hash_map = folly::AtomicHashMap<key, user_entry>;
        
        class user_cache
        {
            public:
                user_cache(std::size_t customer_size_est, std::size_t admin_size_est) : 
                    _c{customer_size_est}, _a{admin_size_est} {}

            public:
                bool valid_customer(std::size_t id, int ratchet, user& db);
                bool valid_admin(std::size_t id, int ratchet, user& db);

            public:
                bool invalidate_customer(std::size_t id);
                bool invalidate_admin(std::size_t id);

            private:
                user_hash_map _c;
                user_hash_map _a;
        };
    }
}

#endif

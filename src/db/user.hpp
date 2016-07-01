#ifndef ISAAC_UTIL_USER_H
#define ISAAC_UTIL_USER_H

#include <string>
#include <memory>
#include <pqxx/pqxx>

namespace isaac
{
    namespace db
    {
        using connection_ptr = std::unique_ptr<pqxx::connection>;
        
        class user
        {
            public:
                user(pqxx::connection& c) : _c{c} {}

            public:
                bool valid_customer(std::size_t id, int ratchet);
                bool valid_admin(std::size_t id, int ratchet);

            private:
                pqxx::connection& _c;
        };

        using user_ptr = std::unique_ptr<user>;
    }
}

#endif

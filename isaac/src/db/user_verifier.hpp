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
        
        class user_verifier
        {
            public:
                user_verifier(pqxx::connection& c);

            public:
                bool valid_user(std::size_t id, int ratchet);

            private:
                bool same_ratchet(
                        const char* table, 
                        const std::size_t id, 
                        const int ratchet);
            private:
                pqxx::connection& _c;
        };

        using verifier_ptr = std::unique_ptr<user_verifier>;
    }
}

#endif

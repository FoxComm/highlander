
#include "db/user.hpp"
#include "util/dbc.hpp"

#include <sstream>

namespace isaac
{
    namespace db
    {
        //This is temporary, the ratchet will come from query
        const int FAKE_RATCHET = 0;

        std::string query(const char* table, std::size_t id)
        {
            std::stringstream q;
            q << "select id from " << table << " where id=" << id;
            return q.str();
        }

        bool user::valid_customer(std::size_t id, int ratchet)
        {
            REQUIRE_GREATER_EQUAL(ratchet, 0);

            pqxx::read_transaction w{_c};
            auto r = w.exec(query("customers", id));
            return !r.empty() && ratchet == FAKE_RATCHET;
        }

        bool user::valid_admin(std::size_t id, int ratchet)
        {
            REQUIRE_GREATER_EQUAL(ratchet, 0);

            pqxx::read_transaction w{_c};
            auto r = w.exec(query("store_admins", id));
            return !r.empty() && ratchet == FAKE_RATCHET;
        }
    }
}

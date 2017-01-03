#include "cluster/cluster.hpp"
#undef CHECK
#include "util/dbc.hpp"

#include <stdexcept>
#include <sstream>

namespace bernardo::cluster
{

    feature_vec::value_type map_number(const folly::dynamic& v)
    {
        return v.asDouble();
    }

    feature_vec::value_type map_enumeration(const folly::dynamic& v, const trait::definition& t)
    {
        auto val = std::find(std::begin(t.values), std::end(t.values), v.getString());
        return val != std::end(t.values) ? std::distance(std::begin(t.values), val) + 1.0 : 0.0;
    }

    feature_vec::value_type map_value(const folly::dynamic& v, const trait::definition& t)
    {
        feature_vec::value_type r = 0.0;
        switch(t.type)
        {
            case trait::kind::number: r = map_number(v); break;
            case trait::kind::enumeration: r = map_enumeration(v, t); break;
            default: CHECK(false && "forgot case");
        }
        return r;
    }

    feature_vec::value_type compile_trait(const trait::definition& t, const query& q)
    {
        auto trait = q.traits.find(t.name);
        if(trait == q.traits.items().end())
        {
            std::stringstream s;
            s << "query is missing trait \"" << t.name << "\"";
            throw std::invalid_argument{s.str()};
        }
        return map_value(trait->second, t);
    }

    group_map::const_iterator group_for_query(const all_groups& all, const query& q)
    {
        return all.groups.find(q.type);
    }

    feature_vec compile_query(const query& q, const group& g)
    {
        const auto& d = g.def;
        REQUIRE_FALSE(d.traits.empty());

        feature_vec r;
        r.reserve(d.traits.size());
        std::transform(std::begin(d.traits), std::end(d.traits),
                std::back_inserter(r),
                [&q](const auto& trait) { return compile_trait(trait, q); });

        ENSURE_EQUAL(r.size(), d.traits.size());
        return r;
    }

}

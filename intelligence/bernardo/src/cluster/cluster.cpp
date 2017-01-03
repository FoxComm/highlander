#include "cluster/cluster.hpp"
#undef CHECK
#include "util/dbc.hpp"

#include <stdexcept>
#include <sstream>
#include <numeric>

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

    feature_vec::value_type compile_trait(const trait::definition& t, const folly::dynamic& traits)
    {
        auto trait = traits.find(t.name);
        if(trait == traits.items().end())
        {
            std::stringstream s;
            s << "query is missing trait \"" << t.name << "\"";
            throw std::invalid_argument{s.str()};
        }
        return map_value(trait->second, t);
    }

    const group* group_for_query(const all_groups& all, const query& q)
    {
        auto groups = all.groups.find(q.scope);
        if(groups == all.groups.end()) return nullptr;

        auto group = groups->second.find(q.group_name);
        if(group == groups->second.end()) return nullptr;
        return &(group->second);
    }

    feature_vec compile_traits(const folly::dynamic& traits, const definition& d)
    {
        REQUIRE_FALSE(d.traits.empty());

        feature_vec r;
        r.reserve(d.traits.size());

        std::transform(std::begin(d.traits), std::end(d.traits),
                std::back_inserter(r),
                [&traits](const auto& trait_def) { return compile_trait(trait_def, traits); });

        ENSURE_EQUAL(r.size(), d.traits.size());
        return r;
    }

    feature_vec compile_query(const query& q, const group& g)
    {
        const auto& d = g.def;
        REQUIRE_FALSE(d.traits.empty());
        return compile_traits(q.traits, g.def);
    }

    void group::add_cluster(const std::string& reference, folly::dynamic traits)
    {
        clusters.emplace_back( cluster { reference, traits, compile_traits(traits, def)});
    }

    double euclidean_dist(const feature_vec& a, const feature_vec& b)
    {
        REQUIRE_EQUAL(a.size(), b.size());
        return std::inner_product(std::begin(a), std::end(a), std::begin(b), 0.0,
                std::plus<double>(), 
                [](auto a, auto b) 
                { 
                    auto d = a - b;
                    return d*d;
                });
    }

    double hamming_dist(const feature_vec& a, const feature_vec& b)
    {
        REQUIRE_EQUAL(a.size(), b.size());

        return std::inner_product(std::begin(a), std::end(a), std::begin(b), 0.0,
                std::plus<double>(), 
                [](auto a, auto b) 
                { 
                    return a == b ? 0.0 : 1.0;
                });
    }

    double cluster_dist(const feature_vec& a, const feature_vec& b, distance_function dist_type)
    {
        double d = std::numeric_limits<double>::max();
        switch(dist_type)
        {
            case distance_function::euclidean: d = euclidean_dist(a, b); break;
            case distance_function::hamming: d = hamming_dist(a, b); break;
            default: CHECK(false && "missed case");
        }

        REQUIRE_GREATER_EQUAL(d, 0.0);
        return d;
    }

    /**
     * TODO: USE FLANN HERE
     */
    find_result find_cluster(const feature_vec& features, const group& g)
    {
        REQUIRE_GREATER(g.clusters.size(), 0);

        double smallest_dist = cluster_dist(features, g.clusters.front().features, g.def.distance_func);
        auto best_cluster =  std::min_element(std::begin(g.clusters), std::end(g.clusters),
                [&](const auto& c, const auto&) -> bool
                {
                    auto old_smallest = smallest_dist;
                    auto dist = cluster_dist(features, c.features, g.def.distance_func);
                    if(dist <= smallest_dist) smallest_dist = dist;
                    return dist < old_smallest;
                });
        return { best_cluster, smallest_dist};
    }

    void load_groups_from_db(pqxx::connection& c, all_groups& all)
    {

    }
}

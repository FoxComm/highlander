#include "cluster/cluster.hpp"
#undef CHECK
#include "util/dbc.hpp"

#include <stdexcept>
#include <sstream>
#include <numeric>

namespace bernardo::cluster
{

    namespace
    {
        const int KD_TREES = 4;
        const int TREE_SEARCH_PARAMS = 128;
        const int NN = 1;
        const int QUERY_SIZE = 1;

    }

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

    group::group() : def{}, clusters{}, _features{},  _index{} {}
    group::group(const definition& d) : def{d}, clusters{}, _features{},  _index{} {}
    group& group::operator=(const group& o)
    {
        if(&o == this) return *this;
        def = o.def;
        clusters = o.clusters;
        _features = o._features;
        if(o._index) _index = std::make_unique<index>(*o._index);
    }

    void group::add_cluster(const std::string& reference, folly::dynamic traits)
    {
        clusters.emplace_back( cluster { reference, traits, compile_traits(traits, def)});
    }

    void group::build_index()
    {
        REQUIRE_FALSE(clusters.empty());

        const auto& first = clusters.front();
        auto feature_count = first.features.size();

        REQUIRE_GREATER(feature_count, 0);

        //construct feature matrix
        auto data_size = clusters.size() * feature_count;
        _features = feature_mat{new double[data_size], clusters.size(), feature_count};

        for(int c = 0; c < clusters.size(); c++)
        {
            const auto& cl = clusters[c];
            CHECK_EQUAL(cl.features.size(), feature_count);
            std::copy(std::begin(cl.features), std::end(cl.features), _features[c]);
        }

        //index features
        _index = std::make_unique<index>(_features, flann::KDTreeIndexParams(KD_TREES));
        _index->buildIndex();

        ENSURE(_index);
    }

    /**
     * TODO: USE FLANN HERE
     */
    find_result group::find_cluster(const feature_vec& features) const
    {
        REQUIRE_GREATER(clusters.size(), 0);

        if(_index)
        {
            REQUIRE_EQUAL(clusters.size(), _features.rows);
            INVARIANT_EQUAL(QUERY_SIZE, 1);

            //hack here, just use feature vector memory since we have a query size of 1
            feature_mat q{const_cast<double*>(features.data()), QUERY_SIZE, features.size()};

            CHECK_EQUAL(QUERY_SIZE * NN, 1); //make sure we have only closest result for single query.
                                              //no need to use heap
            int index;
            double dist;
            indices_mat indices{&index, QUERY_SIZE, NN};
            dist_mat dists{&dist, QUERY_SIZE, NN};

            _index->knnSearch(q, indices, dists, NN, flann::SearchParams(TREE_SEARCH_PARAMS));

            CHECK_RANGE(index, 0, clusters.size());

            auto best_cluster = clusters.begin();
            std::advance(best_cluster, index);
            return {best_cluster, dist};
        }
        else
        {
            double smallest_dist = cluster_dist(features, clusters.front().features, def.distance_func);
            auto best_cluster =  std::min_element(std::begin(clusters), std::end(clusters),
                    [&](const auto& c, const auto&) -> bool
                    {
                    auto old_smallest = smallest_dist;
                    auto dist = cluster_dist(features, c.features, def.distance_func);
                    if(dist <= smallest_dist) smallest_dist = dist;
                    return dist < old_smallest;
                    });

            return { best_cluster, smallest_dist};
        }
    }

    void load_groups_from_db(pqxx::connection& c, all_groups& all)
    {
        pqxx::read_transaction w{c};

    }
}

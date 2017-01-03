#include "cluster/cluster.hpp"
#undef CHECK
#include "util/dbc.hpp"

#include <stdexcept>
#include <sstream>
#include <numeric>

#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/split.hpp>

#include <folly/json.h>
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

    distance_function to_distance_func(const std::string& name)
    {
        distance_function f = distance_function::euclidean;
        if(name == "euclidean") f = distance_function::euclidean;
        else if(name == "hamming") f = distance_function::hamming;
        else 
        {
            std::stringstream e;
            e << "the distance function " << name << " is not valid" << std::endl;
            throw std::invalid_argument{e.str()};
        }

        return f;
    }

    trait::kind to_trait_kind(const std::string& name)
    {
        trait::kind f = trait::kind::number;
        if(name == "number") f = trait::kind::number;
        else if(name == "enumeration") f = trait::kind::enumeration;
        else 
        {
            std::stringstream e;
            e << "the trait kind " << name << " is not valid" << std::endl;
            throw std::invalid_argument{e.str()};
        }

        return f;
    }
    trait::enum_values to_enum_values(const std::string& encoded)
    {
        if(encoded.size() <= 2) return {};

        auto just_values = encoded.substr(1, encoded.size() - 2);
        trait::enum_values r;

        using split_iter = boost::algorithm::split_iterator<std::string::const_iterator>;
        split_iter it(just_values.begin(), just_values.end(), 
                boost::algorithm::first_finder(",", boost::algorithm::is_equal()));
        split_iter end;

        for(;it != end; it++) r.emplace_back(std::string{it->begin(), it->end()});

        ENSURE_GREATER_EQUAL(r.size(), 1);
        return r;
    }

    definition load_definition(pqxx::read_transaction& w, size_t cluster_definition_id)
    {
        std::stringstream def_query;
        def_query << "select distance_func from cluster_definitions where id=" << cluster_definition_id << " limit 1";
        auto cluster_def = w.exec(def_query.str());
        if(cluster_def.size() == 0) 
        {
            std::stringstream e;
            e << "unable to find a cluster definition with id " << cluster_definition_id;
            throw std::invalid_argument{e.str()};
        }

        std::string distance_function_name;
        cluster_def[0][0].to(distance_function_name);

        std::cout << "\tdistance function: " << distance_function_name << std::endl;
        definition def;
        def.distance_func = to_distance_func(distance_function_name);

        std::stringstream trait_definition_query;
        trait_definition_query << "select name, kind, enum_values from trait_definitions where cluster_definition_id = " << cluster_definition_id;
        auto trait_definitions = w.exec(trait_definition_query.str());
        for(auto t : trait_definitions)
        {
            CHECK_EQUAL(t.size(), 3);
            std::string name;
            std::string kind;
            std::string enum_values;

            t[0].to(name);
            t[1].to(kind);
            t[2].to(enum_values);

            std::cout << "\t\ttrait: " << name << " kind: " << kind << " vals: " << enum_values << std::endl;

            trait::definition d;
            d.name = name;
            d.type = to_trait_kind(kind);
            d.values = to_enum_values(enum_values);
            
            def.traits.emplace_back(d);
        }

        if(def.traits.empty())
        {
            std::cerr << "WARNING: Cluster definition " << cluster_definition_id << " does not have any traits";
        }

        return def;
    }

    void load_clusters(pqxx::read_transaction& w, size_t group_id, group& g)
    {
        std::stringstream cluster_query;
        cluster_query << "select ref, traits from clusters where group_id = " << group_id;
        auto clusters = w.exec(cluster_query.str());
        for(auto cluster: clusters)
        {
            CHECK_EQUAL(cluster.size(), 2);
            std::string ref;
            std::string encoded_traits;
            cluster[0].to(ref);
            cluster[1].to(encoded_traits);

            std::cout << "\t\tcluster: " << ref << " traits: " << encoded_traits << std::endl;
            g.add_cluster(ref, folly::parseJson(encoded_traits));
        }
        g.build_index();
    }

    void load_groups_from_db(pqxx::connection& c, all_groups& all)
    {
        pqxx::read_transaction w{c};
        auto groups = w.exec("select id, scope, name, cluster_definition_id from groups");
        for(auto row: groups)
        {
            CHECK_EQUAL(row.size(), 4);
            
            size_t group_id;
            std::string scope;
            std::string name;
            size_t cluster_definition_id;
            row[0].to(group_id);
            row[1].to(scope);
            row[2].to(name);
            row[3].to(cluster_definition_id);

            std::cout << "group: " << group_id << " scope: " << scope << " name: " << name << " def_id: " << cluster_definition_id << std::endl;

            group g;
            g.def = load_definition(w, cluster_definition_id);
            load_clusters(w, group_id, g);

            all.groups[scope][name] = g;
        }

        if(all.groups.empty()) 
        {
            std::cerr << "WARNING: NO GROUPS IN DB";
        }
    }
}

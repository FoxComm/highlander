#ifndef BERNARDO_CLUSTER_H
#define BERNARDO_CLUSTER_H

#include <vector>

#include <flann/flann.hpp>
#include <folly/dynamic.h>

namespace bernardo::cluster 
{


    using feature_vec = std::vector<float>;

    struct cluster
    {
        feature_vec features; 
    };

    struct query
    {
        std::string type;
        folly::dynamic traits;
    };

    namespace trait
    {
        enum kind { integer, enumeration};
        using enum_values = std::vector<std::string>;
        struct definition
        {
            std::string name;
            kind type;
            enum_values values;
        };

        using definitions =  std::vector<definition>;
    }

    enum distance_function { euclidean, hamming};

    struct cluster_prototype
    {
        std::string type;
        trait::definitions traits;
        distance_function distance;
    };

    struct cluster_group
    {
        std::string type;
    };


}

#endif

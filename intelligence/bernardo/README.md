#Bernardo The Architect

Bernardo is an architect who designed the wedding of Marie de Medici to King 
Henry the IV of France in 1600.

Bernardo is a service that maps an object with a set of attributes and find the best
cluster matching that object. The attributes are mapped to a feature vector which is then
compared to clusters find the best one using kNN search.

#High Level Design

Bernardo will have an endpoint that will take a json object as a payload

  {
     "scope" : "1.1",
     "group": "client-request",
     "traits" : {
        "foo": "bar",
        "baz": 6
     }
  } 

And Map that to a feature vector

  [3, 4]

Which will search all clusters of the type specified, in this case "customer-request"
and find the closest match.

Each cluster type will have a definition describing the mapping.

    namespace trait
    {
        enum kind { number, enumeration};
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

    struct definition
    {
        trait::definitions traits;
        distance_function distance_func;
    };

Where the definition has a list of traits and info in how they are mapped

For now we will support just enumerations and number, but you can imagine more complex
mapping functions later, including using neural nets.

Given the above mapping and arrangement we get a feature vector.

The "distance_function" describes a function to use to compare feature vectors, in
this case we can have "euclidean" and "hamming" for example.

#Performance

The service will use a kNN library called [FLANN](http://www.cs.ubc.ca/research/flann/) do index
and compare feature vectors.

Eventually we may want to run this on a GPU machine given FLANN's support for CUDA and the eventual
situation where we have many clusters to match against.

#Directories
 
| Directories                            | Description                                                                                                  |
|:---------------------------------------|:-------------------------------------------------------------------------------------------------------------|
| [sql](sql)                             | flyway migration scripts|
| [src](src)                             | Source Code |



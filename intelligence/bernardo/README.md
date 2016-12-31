#Bernardo The Architect

Bernardo is an architect who designed the wedding of Marie de Medici to King 
Henry the IV of France in 1600.

Bernardo is a service that maps an object with a set of attributes and find the best
cluster matching that object. The attributes are mapped to a feature vector which is then
compared to clusters find the best one using kNN search.

#High Level Design

Bernardo will have an endpoint that will take a json object as a payload

  {
     "type": "customer-request",
     "foo": "bar",
     "baz": 6
  } 

And Map that to a feature vector


  [3, 4]

Which will search all clusters of the type specified, in this case "customer-request"
and find the closest match.

Each cluster type will have a definition describing the mapping.

  create table cluster_mapping(
    id serial primary key,
    type text,
    mapping jsonb,
    arangement array,
    distance_function text
 );

Where the mapping is a json object describing the attributes and the kind
of mapping function to use


  {
      "foo": ["bar", "boop"]
      "baz": "int"
  }

The example above, "foo" is mapped to an enumeration where "bar" is 0 and "boop" is 1.
Also, "baz" is already an int so it gets mapped as an "int"


For now we will support just enumerations and int, but you can imagine more complex
mapping functions later.


The "arrangement" parameter is an array of the attributes in which order they
are mapped, so in this case

  ["foo", "baz"] 

means foo is first and baz is next.

Given the above mapping and arrangement we get the following feature vector.

  [0, 6]

The "distance_function" describes a function to use to compare feature vectors, in
this case we can have "euclidean" and "hamming" for example.


 



import _ from 'lodash';
import SchemaResolver from 'jsen/lib/resolver';

function expand(schema, resolver) {
  schema = resolver.resolve(schema);

  if (schema.properties) {
    _.forOwn(schema.properties, (value, key) => {
      schema.properties[key] = expand(value, resolver);
    });
  } else if (schema.items) {
    schema.items = expand(schema.items, resolver);
  }

  return schema;
}

export function expandRefs(schema, options = {}) {
  const resolver = new SchemaResolver(schema, options.schemas, false);

  return expand(_.cloneDeep(schema), resolver);
}

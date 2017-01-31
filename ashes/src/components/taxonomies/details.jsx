// @flow

// lib
import React, { Element } from 'react';

// components
import ObjectDetailsDeux from 'components/object-page/object-details-deux';

// types
import type { ObjectSchema, ObjectView } from 'paragons/object';
import type { Taxonomy } from 'paragons/taxonomy';

const layout = require('./layout.json');

type Props = {
  schema: ?schema,
  taxonomy: ?Taxonomy,
  onUpdateObject: (object: ObjectView) => void,
};

const TaxonomyDetails = (props: Props): Element => {
  const { schema, taxonomy, onUpdateObject } = props;
  if (!taxonomy) {
    return <div></div>;
  }

  return (
    <ObjectDetailsDeux
      layout={layout}
      title="taxonomy"
      plural="taxonomies"
      object={taxonomy}
      schema={schema}
      onUpdateObject={onUpdateObject}
    />
  );
};

export default TaxonomyDetails;

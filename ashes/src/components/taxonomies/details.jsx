// @flow

// lib
import React, { Element } from 'react';

// components
import ObjectDetailsDeux from 'components/object-page/object-details-deux';

const layout = require('./layout.json');

type Props = {
  schema: ObjectSchema,
  taxonomy: ?Taxonomy,
  onUpdateObject: (object: ObjectView) => void,
};

const TaxonomyDetails = (props: Props) => {
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

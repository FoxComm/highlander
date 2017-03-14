// @flow

// lib
import React, { Element } from 'react';

// components
import ObjectDetailsDeux from 'components/object-page/object-details-deux';

const layout = require('./layout.json');

export default class TaxonDetails extends React.Component {
  render () {
    const { schema, taxon, onUpdateObject } = this.props;
    if (!taxon) {
      return <div></div>;
    }
    return (
      <ObjectDetailsDeux
        layout={layout}
        title="taxon"
        plural="taxons"
        object={taxon}
        schema={schema}
        onUpdateObject={onUpdateObject}
      />
    );
  }
};

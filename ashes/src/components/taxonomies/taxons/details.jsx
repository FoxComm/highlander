// @flow

// lib
import React, { Element } from 'react';

// components
import ObjectDetailsDeux from 'components/object-page/object-details-deux';

export default class TaxonDetails extends React.Component {
  props: ObjectPageChildProps<Taxon>;

  render() {
    return (
      <ObjectDetailsDeux
        {...this.props}
      />
    );
  }
}

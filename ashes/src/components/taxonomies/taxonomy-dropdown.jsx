// @flow

// libs
import React, { Component } from 'react';

// components
import FlatTaxonsDropdown from './taxons-dropdown/flat-taxons-dropdown';
import TaxonsDropdown from './taxons-dropdown/taxons-dropdown';

type Props = {
  taxonomy: Taxonomy,
  onTaxonClick: Function
}

export default class TaxonomyDropdown extends Component {

  props: Props;

  render() {
    const { taxonomy, onTaxonClick } = this.props;

    if (taxonomy.hierarchical) {
      return null;
    }

    return (
      <FlatTaxonsDropdown
        onTaxonClick={onTaxonClick}
        taxonomy={taxonomy}
      />
    );
  }
}

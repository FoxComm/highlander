// @flow

// libs
import React, { Component } from 'react';

// components
import FlatTaxonsDropdown from './taxons-dropdown/flat-taxons-dropdown'
import TaxonsDropdown from './taxons-dropdown/taxons-dropdown'

export default class TaxonomyDropdown extends Component {

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
    )
  }
}

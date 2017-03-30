// @flow

// libs
import React, { Component } from 'react';
import Dropdown from 'components/dropdown/dropdown'
import { autobind } from 'core-decorators';
import { get } from 'lodash';

// style
import styles from './flat-taxons-dropdown.css';

// components
import { AddButton } from 'components/common/buttons';

export default class FlatTaxonsDropdown extends Component {

  state = {
    value: '',
    searchList: [],
  };

  onInputChange({ target }) {
    this.setState({
      value: target.value
    });
    this.searchTaxons(target.value)
  }

  @autobind
  searchTaxons(input) {
    const { taxonomy: {taxons} } = this.props;
    let search = input.toLowerCase();
    let searchList = [];
    taxons.map((taxon) => {
      const name = get(taxon, 'node.attributes.name.v').toLowerCase();
      if (name.includes(search)) {
        searchList.push(taxon)
      }
    });

    this.setState({ searchList: searchList })
  }

  render() {
    const { taxonomy, onTaxonClick } = this.props;

    const taxonList = this.state.value.length == 0 ? taxonomy.taxons : this.state.searchList;
    const items = taxonList.map((item) => (
      item.node.attributes.name.v, [item.node.id, item.node.attributes.name.v,]
    ));

    return (
      <div styleName="dropdown" onChange={(event) => this.onInputChange(event)}>
        <Dropdown
          editable
          placeholder=""
          noControls
          items={items}
          onChange={onTaxonClick}
        />
      </div>
    )
  }
}

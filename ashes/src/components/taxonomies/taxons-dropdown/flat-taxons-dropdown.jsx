// @flow

// libs
import React, { Component } from 'react';
import GenericDropdown from 'components/dropdown/generic-dropdown';
import { DropdownItem } from 'components/dropdown';
import { autobind } from 'core-decorators';
import { get } from 'lodash';

// style
import styles from './flat-taxons-dropdown.css';

type Props = {
  taxonomy: Taxonomy,
  onTaxonClick: Function
}

type EventTarget = {
  target: {
    value: string,
  },
};

export default class FlatTaxonsDropdown extends Component {
  props: Props;

  state = {
    value: '',
  };

  _d: GenericDropdown;

  @autobind
  onInputChange({ target }: EventTarget) {
    this.setState({
      value: target.value
    });
  }

  @autobind
  onFocus() {
    this._d.openMenu();
  }

  @autobind
  searchTaxons() {
    const { taxonomy: {taxons} } = this.props;
    let search = this.state.value.toLowerCase();


    const filtered = taxons.filter((taxon) => {
      const name = get(taxon, 'node.attributes.name.v');
      return name.toLowerCase().includes(search);
    });

    return filtered.map((taxon) => {
      const name = get(taxon, 'node.attributes.name.v');

      const highlighted = name.replace(new RegExp(
        this.state.value, 'ig'),
        x => `<span class=${styles.needle}>${x}</span>`
      );

      return (
        <DropdownItem value={taxon.node.id} key={taxon.node.id}>
          <span dangerouslySetInnerHTML={{ __html: highlighted }} />
        </DropdownItem>
      );
    });

  }

  // TODO replace any
  @autobind
  renderInput(value: string): Element<*> {
    return (
      <input
        value={value}
        onChange={(event) => this.onInputChange(event)}
        onFocus={this.onFocus}
      />
    );
  }

  @autobind
  searchResults() {
    const { taxonomy } = this.props;

    if (this.state.value.length == 0) {
      return taxonomy.taxons.map((item) => (
        <DropdownItem value={item.node.id} key={item.node.id}>
          <span>{item.node.attributes.name.v}</span>
        </DropdownItem>
      ));
    }

  }

  render() {
    const { onTaxonClick } = this.props;
    const children = (this.state.value == '') ? this.searchResults() : this.searchTaxons();

    return (
      <div styleName="dropdown">
        <GenericDropdown
          open={false}
          value={this.state.value}
          onChange={onTaxonClick}
          renderDropdownInput={this.renderInput}
          ref={d => this._d = d}
          noControls
          editable
        >
          {children}
        </GenericDropdown>
      </div>
    );
  }
}

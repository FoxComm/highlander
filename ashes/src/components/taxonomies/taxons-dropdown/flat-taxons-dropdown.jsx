// @flow

// libs
import React, { Component } from 'react';
import GenericDropdown from 'components/dropdown/generic-dropdown';
import { DropdownItem } from 'components/dropdown';
import { autobind } from 'core-decorators';
import { get, compact } from 'lodash';

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
    open: false,
  };

  _d: GenericDropdown;

  @autobind
  onInputChange({ target }: EventTarget) {
    this.setState({
      value: target.value
    });
  }

  @autobind
  onInputClick() {
    this._d.toggleMenu()
  }

  @autobind
  searchTaxons() {
    const { taxonomy: {taxons} } = this.props;
    let search = this.state.value.toLowerCase();

    return taxons.map((taxon) => {
      const name = get(taxon, 'node.attributes.name.v');

      if (name.toLowerCase().includes(search)) {
        const highlighted = name.replace(new RegExp(
          this.state.value, 'ig'),
          x => `<span class=${styles.needle}>${x}</span>`
        );

        return (
          <DropdownItem value={taxon.node.id} key={taxon.node.id}>
            <span dangerouslySetInnerHTML={{ __html: highlighted }} />
          </DropdownItem>
        )
      }
    });

  }

  @autobind
  renderInput(value: any, title: any, props: any, handleToggleClick: (e: MouseEvent) => void): Element<*> {
    const { taxonomy: { taxons }, taxon } = this.props;

    return (
      <input
        value={this.state.value}
        onChange={(event) => this.onInputChange(event)}
        onClick={this.onInputClick}
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
      ))
    }

  }

  render() {
    const { onTaxonClick } = this.props;
    const { value } = this.state;
    const renderChild = (value == "") ? this.searchResults() : this.searchTaxons();

    return (
      <div styleName="dropdown">
        <GenericDropdown
          open={this.state.open}
          noControls
          onChange={onTaxonClick}
          renderDropdownInput={this.renderInput}
          ref={d => this._d = d}
        >
          {renderChild}
        </GenericDropdown>
      </div>
    );
  }
}


// @flow

// lib
import { compact, defer, get, isEmpty, values } from 'lodash';
import { assoc, merge } from 'sprout-data';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import TextInput from 'components/forms/text-input';
import { DropdownItem } from 'components/dropdown';
import GenericDropdown from 'components/dropdown/generic-dropdown';
import PilledInput from 'components/pilled-search/pilled-input';
import { renderTree } from '../taxons/hierarchical-taxonomy-widget';

// styles
import styles from './taxons-dropdown.css';

type DDItem = {
  id: number,
  name: string,
  path: string,
};

type ReduceResult = {
  [key: string]: DDItem,
};

type Props = {
  taxonomy: Taxonomy,
  taxon: Taxon,
  onChange: (id: ?number) => any,
};

type State = {
  token: string,
  treeMode: boolean,
};

const SEP = ' > ';

const getName = (taxon: ?Taxon, dft = '') => get(taxon, 'attributes.name.v', dft);

function findNode(taxons: TaxonsTree, id: number): ?Taxon {
  const _find = (taxons: TaxonsTree, id: number) =>
    taxons.reduce((res: ?Taxon, node): ?Taxon => {
      if (res) {
        return res;
      }
      if (node.taxon.id === id) {
        return node.taxon;
      }
      if (node.children) {
        return _find(node.children, id);
      }

      return null;
    }, null);

  return _find(taxons, id);
}

const buildTaxonsDropDownItems = (taxons: TaxonsTree, prefix: string, sep: string, finale: ReduceResult = {}) =>
  taxons.reduce((res: ReduceResult, node: TaxonNode) => {
    const name = getName(node.taxon);
    const path = `${prefix}${name}`;

    res = assoc(res, node.taxon.id, { id: node.taxon.id, name, path });

    if (!isEmpty(node.children)) {
      res = merge(res, buildTaxonsDropDownItems(node.children, `${name}${sep}`, sep, res));
    }

    return res;
  }, finale);

const mapNeedle = (token: string) => (part: string, i: number, arr: Array<string>) =>
  part.replace(new RegExp(token, 'ig'), x => `<span class=${styles.needle}>${x}</span>`);

const filterItems = (toFilter: Array<DDItem>, token: string, current: Taxon): Array<DDItem> => {
  if (!token.length) {
    return toFilter;
  }

  return toFilter
    .filter(({ id }) => id !== current.id)
    .filter(({ path }) => path.toLowerCase().indexOf(token.toLowerCase()) > -1)
    .map(item => assoc(item, 'path', item.path.split(SEP).map(mapNeedle(token)).join(SEP)));
};

export default class TaxonsDropdown extends Component {
  props: Props;

  state: State = {
    token: '',
    treeMode: false,
  };

  _d: GenericDropdown;

  get parentItems(): Array<DDItem> {
    const taxons = this.props.taxonomy.taxons;

    return values(buildTaxonsDropDownItems(taxons, '', SEP));
  }

  @autobind
  handleTokenChange({ target }: { target: HTMLInputElement }) {
    this.setState({
      token: target.value,
    });
  }

  @autobind
  handleParentSelect(id: ?number) {
    this.setState({ token: '' });

    this._d.closeMenu();

    this.props.onChange(id);
  }

  @autobind
  handleInputClick(treeMode: boolean, handleToggleClick: (e: MouseEvent) => void) {
    return (e: MouseEvent) => {
      handleToggleClick(e);

      this.setState({ treeMode: treeMode });
    };
  }

  @autobind
  renderInput(value: any, title: any, props: any, handleToggleClick: (e: MouseEvent) => void): Element<*> {
    const parentId = get(this.props.taxon, ['location', 'parent']);
    const parent = findNode(this.props.taxonomy.taxons, parentId);
    const parentName = getName(parent, null);

    return (
      <PilledInput
        solid
        icon="hierarchy"
        placeholder="Parent value name"
        value={this.state.token}
        onChange={this.handleTokenChange}
        pills={compact([parentName])}
        onPillClose={() => this.handleParentSelect(null)}
        onIconClick={this.handleInputClick(true, handleToggleClick)}
        onClick={this.handleInputClick(false, handleToggleClick)}
      />
    );
  }

  get searchResults(): Array<Element<any>> {
    const items = filterItems(this.parentItems, this.state.token, this.props.taxon);

    if (this.state.treeMode) {
      return renderTree({
        taxons: this.props.taxonomy.taxons,
        onClick: this.handleParentSelect,
        getTitle: getName,
      });
    }

    return items.map(({ id, path }: DDItem) => (
      <DropdownItem value={id} key={id}>
        <span dangerouslySetInnerHTML={{ __html: path }} />
      </DropdownItem>
    ));
  }

  render() {
    return (
      <GenericDropdown
        className={styles.dropdown}
        onChange={this.handleParentSelect}
        renderDropdownInput={this.renderInput}
        emptyMessage="No values found"
        noControls
        ref={d => this._d = d}
      >
        {this.searchResults}
      </GenericDropdown>
    );
  }
}

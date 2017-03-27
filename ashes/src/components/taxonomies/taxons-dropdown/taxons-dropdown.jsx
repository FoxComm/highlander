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
  ddOpen: boolean,
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

const mapFilterResult = (token: string) => (part: string, i: number, arr: Array<string>) =>
  part.replace(new RegExp(token, 'ig'), x => `<span class=${styles.needle}>${x}</span>`);

const filterItems = (toFilter: Array<DDItem>) => (token: string): Array<DDItem> => {
  let items = [];

  if (token.length > 0) {
    items = toFilter
      .filter(({ path }) => path.toLowerCase().indexOf(token.toLowerCase()) > -1)
      .map(item => assoc(item, 'path', item.path.split(SEP).map(mapFilterResult(token)).join(SEP)));
  }

  return items;
};

export default class TaxonsDropdown extends Component {
  props: Props;

  state: State = {
    token: '',
    ddOpen: false,
    treeMode: false,
  };

  get parentItems(): Array<DDItem> {
    const taxons = this.props.taxonomy.taxons;

    return values(buildTaxonsDropDownItems(taxons, '', SEP));
  }

  @autobind
  handleTokenChange({ target }: { target: HTMLInputElement }) {
    this.setState({
      token: target.value,
      ddOpen: target.value.length > 0,
    });
  }

  @autobind
  handleParentSelect(id: ?number) {
    this.setState({ ddOpen: false, token: '' });

    this.props.onChange(id);
  }

  @autobind
  close() {
    this.setState({ ddOpen: false });
  }

  @autobind
  renderInput(): Element<*> {
    const parentId = get(this.props.taxon, ['location', 'parent']);
    const parent = findNode(this.props.taxonomy.taxons, parentId);
    const parentName = getName(parent, null);

    return (
      <PilledInput
        solid
        icon="hierarchy"
        value={this.state.token}
        onChange={this.handleTokenChange}
        pills={compact([parentName])}
        onPillClose={() => this.handleParentSelect(null)}
        onPillClick={() => this.setState({ treeMode: true })}
        onFocus={() => this.setState({ ddOpen: this.state.token.length > 0 })}
        onBlur={this.close}
      />
    );
  }

  get searchResults(): Array<Element<*>> {
    const items = filterItems(this.parentItems)(this.state.token);

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
        open={this.state.ddOpen}
        onChange={this.handleParentSelect}
        renderDropdownInput={this.renderInput}
        emptyMessage="No values found"
        noControls
      >
        {this.searchResults}
      </GenericDropdown>
    );
  }
}

// @flow

// lib
import { compact, get, isEmpty, values } from 'lodash';
import { assoc, merge } from 'sprout-data';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import { DropdownItem } from 'components/dropdown';
import GenericDropdown from 'components/dropdown/generic-dropdown';
import PilledInput from 'components/pilled-search/pilled-input';
import { renderTree } from '../taxons/hierarchical-taxonomy-widget';

// helpers
import { transitionTo } from 'browserHistory';
import { findNode } from 'paragons/tree';

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
  context: string,
  taxonomy: Taxonomy,
  taxon: Taxon,
  onChange: (id: ?number) => any,
};

type State = {
  token: string,
};

const SEP = ' > ';

const getName = (taxon: ?Taxon, dft = '') => get(taxon, 'attributes.name.v', dft);

const buildTaxonsDropDownItems = (taxons: TaxonsTree, prefix: string, sep: string, finale: ReduceResult = {}) =>
  taxons.reduce((res: ReduceResult, node: TaxonTreeNode) => {
    const name = getName(node.node);
    const path = `${prefix}${name}`;

    res = assoc(res, node.node.id, { id: node.node.id, name, path });

    if (!isEmpty(node.children)) {
      res = merge(res, buildTaxonsDropDownItems(node.children, `${path}${sep}`, sep, res));
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
  };

  _d: GenericDropdown;

  componentDidMount() {
    window.addEventListener('keydown', this.handleKeyPress, true);
  }

  componentWillUnmount() {
    window.removeEventListener('keydown', this.handleKeyPress, true);
  }

  componentWillUpdate(nextProps: Props, nextState: State) {
    if (nextState.token.length > 0) {
      this._d.openMenu();
    }
  }

  get parentItems(): Array<DDItem> {
    const taxons = this.props.taxonomy.taxons;

    return values(buildTaxonsDropDownItems(taxons, '', SEP));
  }

  @autobind
  handleKeyPress(e: KeyboardEvent) {
    switch (e.keyCode) {
      // backspace
      case 8:
        if (!this.state.token.length && get(this.props.taxon, ['location', 'parent'])) {
          this.handleParentSelect(null);

          this._d.openMenu();
        }

        break;
    }
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
  handlePillClick() {
    const { context, taxonomy, taxon } = this.props;

    const taxonId = get(taxon, ['location', 'parent']);
    if (taxonId !== this.props.taxon.id) {
      transitionTo('taxon-details', { context, taxonomyId: taxonomy.id, taxonId });
    }
  }

  @autobind
  renderInput(value: any, title: any, props: any, handleToggleClick: (e: MouseEvent) => void): Element<*> {
    const { taxonomy, taxon } = this.props;

    const parentId = get(taxon, ['location', 'parent']);
    const parent = findNode(taxonomy.taxons, parentId);
    const parentName = getName(get(parent, 'node'));

    return (
      <PilledInput
        solid
        icon="hierarchy"
        placeholder="Parent value name"
        value={this.state.token}
        onChange={this.handleTokenChange}
        pills={compact([parentName])}
        onPillClick={this.handlePillClick}
        onPillClose={() => this.handleParentSelect(null)}
        onFocus={handleToggleClick}
      />
    );
  }

  get searchResults(): Array<Element<any>> {
    const items = filterItems(this.parentItems, this.state.token, this.props.taxon);

    if (!this.state.token.length) {
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
        placeholder="- Select -"
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

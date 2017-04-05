// @flow

// libs
import { isEmpty, get } from 'lodash';
import React, { Component } from 'react';

// components
import { Dropdown } from 'components/dropdown';

// styles
import s from './taxonomy-dropdown.css';

// types
import type { DropdownItemType } from 'components/dropdown/generic-dropdown';

type ReduceResult = Array<DropdownItemType>;

type Props = {
  taxonomy: Taxonomy,
  onTaxonClick: Function
}

const SEP = ' > ';

const getName = (taxon: ?Taxon) => get(taxon, 'attributes.name.v', '');

const buildTaxonsDropDownItems = (taxons: TaxonsTree, prefix: string, sep: string = SEP, finale: ReduceResult = []) =>
  taxons.reduce((res: ReduceResult, node: TaxonTreeNode) => {
    const name = getName(node.node);
    const path = `${prefix}${name}`;

    res.push([node.node.id, path, false]);

    if (!isEmpty(node.children)) {
      buildTaxonsDropDownItems(node.children, `${name}${sep}`, sep, res);
    }

    return res;
  }, finale);

export default class TaxonomyDropdown extends Component {

  props: Props;

  render() {
    const { taxonomy, onTaxonClick } = this.props;

    const items = buildTaxonsDropDownItems(taxonomy.taxons, '');

    return (
      <Dropdown
        className={s.dropdown}
        name="taxons"
        placeholder=""
        items={items}
        onChange={onTaxonClick}
        noControls
        editable
      />
    );
  }
}

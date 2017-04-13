// @flow

// libs
import { differenceBy, isEmpty, get } from 'lodash';
import React from 'react';

// components
import { Dropdown } from 'components/dropdown';
import { AddButton } from 'components/common/buttons';

// styles
import s from './taxonomy-dropdown.css';

// types
import type { DropdownItemType } from 'components/dropdown/generic-dropdown';

type ReduceResult = Array<DropdownItemType>;

type Props = {
  taxonomy: Taxonomy,
  linkedTaxonomy: LinkedTaxonomy,
  onTaxonClick: Function,
  onNewValueClick: (taxonomy: Taxonomy) => any,
}

const SEP = ' > ';

const getName = (taxon: ?Taxon) => get(taxon, 'attributes.name.v', '');

const buildTaxonsDropDownItems = (taxons: TaxonsTree, prefix: string, sep: string = SEP, finale: ReduceResult = []) =>
  taxons.reduce((res: ReduceResult, node: TaxonTreeNode) => {
    const name = getName(node.node);
    const path = `${prefix}${name}`;

    res.push([node.node.id, path, false]);

    if (!isEmpty(node.children)) {
      buildTaxonsDropDownItems(node.children, `${path}${sep}`, sep, res);
    }

    return res;
  }, finale);

export default (props: Props) => {
  const { taxonomy, linkedTaxonomy = {}, onTaxonClick, onNewValueClick } = props;

  const taxons = differenceBy(taxonomy.taxons, linkedTaxonomy.taxons, t => get(t, 'node.id') || get(t, 'id'));

  const items = buildTaxonsDropDownItems(taxons, '');

  const renderAddButton = (dropdownToggle: Function) => {
    const handler = (e: MoseEvent) => {
      dropdownToggle(e);
      onNewValueClick(taxonomy);
    };

    return (
      <AddButton className={s.newValueButton} onClick={handler}>
        New Value
      </AddButton>
    );
  };

  return (
    <Dropdown
      className={s.dropdown}
      name="taxons"
      placeholder=""
      items={items}
      onChange={onTaxonClick}
      renderAppend={renderAddButton}
      emptyMessage="No items"
      noControls
      editable
    />
  );
};

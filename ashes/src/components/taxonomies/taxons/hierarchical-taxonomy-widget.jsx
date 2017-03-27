// @flow

// libs
import React, { Element } from 'react';

// components
import TreeNode from 'components/tree-node/tree-node';

// types
import type { Node } from 'components/tree-node/tree-node';

type Props = {
  taxons: TaxonsTree,
  activeTaxonId?: string,
  onClick: (id: number) => any,
  getTitle: (node: Taxon) => string,
}

const _reduce = (res: Array<Node<Taxon>>, node: TaxonNode) => {
  const children = node.children ? node.children.reduce(_reduce, []) : null;

  res.push({ children, node: node.taxon });

  return res;
};

const prepareTree = (taxons: TaxonsTree): Array<Node<Taxon>> => taxons.reduce(_reduce, []);

export const renderTree = ({ taxons, activeTaxonId = '', ...rest }: Props): Array<Element<any>> =>
  prepareTree(taxons).map((item: Node<Taxon>) => (
    <TreeNode
      node={item}
      visible={true}
      depth={20}
      currentObjectId={activeTaxonId}
      key={item.node.id}
      {...rest}
    />
  ));

export default (props: Props) => (
  <div>{renderTree(props)}</div>
);

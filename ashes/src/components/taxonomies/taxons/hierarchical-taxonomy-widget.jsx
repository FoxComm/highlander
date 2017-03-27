// @flow

// libs
import React from 'react';

// components
import TreeNode from 'components/tree-node/tree-node';

// types
import type { Node } from 'components/tree-node/tree-node';

type Props = {
  taxons: TaxonsTree,
  activeTaxonId: string,
  handleTaxonClick: (id: number) => any,
  getTitle: (node: Taxon) => string,
}

const _reduce = (res: Array<Node<Taxon>>, node: TaxonNode) => {
  const children = node.children ? node.children.reduce(_reduce, []) : null;

  res.push({ children, node: node.taxon });

  return res;
};

const prepareTree = (taxons: TaxonsTree): Array<Node<Taxon>> => taxons.reduce(_reduce, []);

export default ({ taxons, handleTaxonClick, activeTaxonId, getTitle }: Props) => (
  <div>
    {prepareTree(taxons).map((item: Node<Taxon>) => (
      <TreeNode
        node={item}
        visible={true}
        depth={20}
        handleClick={handleTaxonClick}
        currentObjectId={activeTaxonId}
        getTitle={getTitle}
        key={item.node.id}
      />
    ))}
  </div>
);

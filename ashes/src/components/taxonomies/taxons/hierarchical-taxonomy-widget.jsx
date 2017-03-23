// @flow

// libs
import React, { Component } from 'react';

// components
import TreeNode from './tree-node';

export default class HierarchicalTaxonomyListWidget extends Component {

  get content(): Element {
    const { taxons, handleTaxonClick, currentTaxon } = this.props;

    return taxons.map((item) => {
      return (
        <div key={item.taxon.id}>
          <TreeNode
            node={item}
            visible={true}
            depth={20}
            handleClick={handleTaxonClick}
            currentObject={currentTaxon}
          />
        </div>
      );
    });
  }

  render() {
    return (
      <div>
        {this.content}
      </div>
    );
  }

}

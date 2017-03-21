// @flow

// libs
import React, { Component } from 'react';
import classNames from 'classnames';

// style
import styles from './tree-node.css';

export default class TreeNode extends Component {

  state = {
    expanded: false
  };

  toggleExpanded(event) {
    this.setState({ expanded: !this.state.expanded });
    event.stopPropagation();
  }

  renderChildren() {
    const { node, depth, visible, handleTaxonClick, currentTaxon } = this.props;
    const active = node.taxon.id.toString() === currentTaxon;
    const className = classNames(styles['node'], { [styles.visible]: visible }, { [styles.active]: active });

    if (!node.children) { return null;}

     return node.children.map((child) => (
          <ul>
            <TreeNode
              visible={this.state.expanded && visible}
              node={child}
              depth={depth+10}
              className={className}
              key={child.taxon.id}
              handleTaxonClick={handleTaxonClick}
              currentTaxon={currentTaxon}
            />
          </ul>
        )
      )

  }

  render() {
    const { node, depth, visible, handleTaxonClick, currentTaxon } = this.props;
    const active = node.taxon.id.toString() === currentTaxon;
    const className = classNames(styles['node'], { [styles.visible]: visible }, { [styles.active]: active });

    return(
      <li
        key={node.taxon.id}
        className={className}
        onClick={(event) => handleTaxonClick(node.taxon.id, event)}
      >
        <span
          onClick={(event) => this.toggleExpanded(event)}
          styleName="item"
          style={{ marginLeft: `${depth + 20}` }}
        >
        {node.taxon.attributes.name.v}
        </span>
        {this.renderChildren()}
      </li>
    )
  }
}

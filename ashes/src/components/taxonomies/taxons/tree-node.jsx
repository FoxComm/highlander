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

  render() {
    const { node, level, visible } = this.props;
    const children = [];

    const className = classNames(styles['node'], { [styles.visible]: visible });

    if (node.children) {
      node.children.forEach((child) => {
        children.push(
          <TreeNode
            visible={this.state.expanded && visible}
            node={child}
            level={level+1}
            className={className}
            key={child.taxon.id}
          />
        )
      })
    }

    return(
      <li
        onClick={(event) => this.toggleExpanded(event)}
        key={node.taxon.id}
        className={className}
      >
        {node.taxon.attributes.name.v}
        {`   ${level}`}
        {children}
      </li>
    )
  }
}

// @flow

// libs
import React, { Component } from 'react';
import classNames from 'classnames';

// style
import styles from './tree-node.css';

export default class TreeNode extends Component {

  state = {
    expanded: false,
  };

  toggleExpanded(event) {
    if (this.props.node.children) {
      this.setState({ expanded: !this.state.expanded });
    }
    event.stopPropagation();
  }

  get renderChildren() {
    const { node, depth, visible, handleClick, currentObject } = this.props;
    const active = node.taxon.id.toString() === currentObject;
    const className = classNames(styles['node'], { [styles.visible]: visible }, { [styles.active]: active });

    if (!node.children) { return null; }

     return node.children.map((child) => (
          <div>
            <TreeNode
              visible={this.state.expanded && visible}
              node={child}
              depth={depth+20}
              className={className}
              key={child.taxon.id}
              handleClick={handleClick}
              currentObject={currentObject}
            />
          </div>
        )
      )

  }

  get renderIcon() {

    if (!this.props.node.children) {
      return (
        <i className="icon-category" />
      )
    }

    if (this.state.expanded) {
      return (
        <i className="icon-category-collapse" />
      )
    }

    if (!this.state.expanded) {
      return (
        <i className="icon-category-expand" />
      )
    }

  }

  render() {
    const { node, depth, visible, handleClick, currentObject } = this.props;
    const active = node.taxon.id.toString() === currentObject;
    const className = classNames(styles['node'], { [styles.visible]: visible }, { [styles.active]: active });

    return(
      <div
        key={node.taxon.id}
      >
        <div className={className}>
          <span
            onClick={(event) => this.toggleExpanded(event)}
            styleName="item"
            style={{ marginLeft: `${depth}px` }}
          >
            <span>
              {this.renderIcon}
              <span
                styleName="text"
                onClick={(event) => handleClick(node.taxon.id, event)}
              >
                {node.taxon.attributes.name.v}
              </span>
            </span>
          </span>
        </div>
        {this.renderChildren}
      </div>
    )
  }
}

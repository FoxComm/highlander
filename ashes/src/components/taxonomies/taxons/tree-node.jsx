// @flow

// libs
import React, { Component } from 'react';
import classNames from 'classnames';

// style
import styles from './tree-node.css';

type Props = {
  visible: boolean,
  node: Node,
  depth: number,
  handleClick: (id: number) => void,
  currentObject: string,
}

type Node = {
  taxon: Taxon,
  children?: Array<Node>
}

export default class TreeNode extends Component {

  props: Props;

  state = {
    expanded: false,
  };

  toggleExpanded(event: SyntheticEvent) {
    if (this.props.node.children) {
      this.setState({ expanded: !this.state.expanded });
    }
    event.stopPropagation();
  }

  renderChildren() {
    const { node, depth, visible, handleClick, currentObject } = this.props;
    const active = node.taxon.id.toString() === currentObject;
    const className = classNames(styles['node'],
      { [styles.visible]: visible }, { [styles.active]: active });

    if (!node.children) { return null; }

     return node.children.map((child) => (
          <div key={child.taxon.id}>
            <TreeNode
              visible={this.state.expanded && visible}
              node={child}
              depth={depth+20}
              className={className}
              handleClick={handleClick}
              currentObject={currentObject}
            />
          </div>
        )
      );

  }

  get renderIcon() {

    const { children } = this.props.node;

    const cls = classNames({
      'icon-category': !children,
      'icon-category-expand': children && !this.state.expanded,
      'icon-category-collapse': children && this.state.expanded,
    });

    return <i className={cls} />;
  }

  get renderText() {
    const { node: { taxon }, handleClick } = this.props;
    return (
      <span
        styleName="text"
        onClick={() => handleClick(taxon.id)}
      >
        {taxon.attributes.name.v}
      </span>
    );
  }

  render() {
    const { node, depth, visible, currentObject } = this.props;
    const active = node.taxon.id.toString() === currentObject;
    const className = classNames(styles['node'], {
      [styles.visible]: visible,
      [styles.active]: active,
    });

    return(
      <div>
        <div className={className}>
          <div
            styleName="item"
            onClick={(event) => this.toggleExpanded(event)}
            style={{ marginLeft: `${depth}px` }}
          >
            <div styleName="attributes">
              {this.renderIcon}
              {this.renderText}
            </div>
          </div>
        </div>
        {this.renderChildren()}
      </div>
    );
  }
}

// @flow

// libs
import React, { Component } from 'react';
import classNames from 'classnames';

// style
import styles from './tree-node.css';

export type Node<T> = {
  children: ?Array<Node<T>>,
  node: {
    id: number,
    [key: string]: any,
  },
}

type Props<T> = {
  visible: boolean,
  node: Node<T>,
  depth: number,
  handleClick: (id: number) => void,
  currentObjectId: string,
  getTitle: (node: T) => string,
}

export default class TreeNode extends Component {
  props: Props<*>;

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
    const { node, depth, visible, handleClick, currentObjectId, getTitle } = this.props;
    const active = node.node.id.toString() === currentObjectId;
    const className = classNames(styles.node,
      { [styles.visible]: visible }, { [styles.active]: active });

    if (!node.children) {
      return null;
    }

    return node.children.map((child: Node<any>) => (
        <TreeNode
          visible={this.state.expanded && visible}
          node={child}
          depth={depth + 20}
          className={className}
          handleClick={handleClick}
          currentObjectId={currentObjectId}
          getTitle={getTitle}
          key={child.node.id}
        />
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
    const { node, getTitle, handleClick } = this.props;
    return (
      <span styleName="text" onClick={() => handleClick(node.node.id)}>
        {getTitle(node.node)}
      </span>
    );
  }

  render() {
    const { node, depth, visible, currentObjectId } = this.props;
    const active = node.node.id.toString() === currentObjectId;
    const className = classNames(styles['node'], {
      [styles.visible]: visible,
      [styles.active]: active,
    });

    return (
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

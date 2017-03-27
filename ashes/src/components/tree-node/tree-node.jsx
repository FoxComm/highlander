// @flow

// libs
import noop from 'lodash/noop';
import { autobind } from 'core-decorators';
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

  static defaultProps = {
    handleClick: noop,
  };

  @autobind
  toggleExpanded(event: SyntheticEvent) {
    event.stopPropagation();

    if (this.props.node.children) {
      this.setState({ expanded: !this.state.expanded });
    }
  }

  get icon() {
    const { children } = this.props.node;

    const cls = classNames({
      'icon-category': !children,
      'icon-category-expand': children && !this.state.expanded,
      'icon-category-collapse': children && this.state.expanded,
    });

    return <i className={cls} onClick={this.toggleExpanded} />;
  }

  get label() {
    const { node, getTitle } = this.props;

    return <span styleName="text">{getTitle(node.node)}</span>;
  }

  get children() {
    const { node, depth, visible, currentObjectId, ...rest } = this.props;

    if (!node.children) {
      return null;
    }

    return node.children.map((child: Node<any>) => (
        <TreeNode
          visible={this.state.expanded && visible}
          node={child}
          depth={depth + 20}
          currentObjectId={currentObjectId}
          key={child.node.id}
          {...rest}
        />
      )
    );
  }

  get node() {
    const { node, depth, currentObjectId, handleClick } = this.props;
    const active = node.node.id.toString() === currentObjectId;

    const className = classNames(styles.node, {
      [styles.active]: active,
    });

    const style = { marginLeft: `${depth}px` };

    return (
      <div className={className}>
        <div styleName="item" onClick={() => handleClick(node.node.id)} style={style}>
          <div styleName="attributes">
            {this.icon}
            {this.label}
          </div>
        </div>
      </div>
    );
  }

  render() {
    if (!this.props.visible) {
      return null;
    }

    return (
      <div>
        {this.node}
        {this.children}
      </div>
    );
  }
}

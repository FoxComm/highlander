// @flow

// libs
import noop from 'lodash/noop';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import classNames from 'classnames';

// components
import Icon from 'components/core/icon';

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
  onClick: (id: number) => void,
  currentObjectId?: string,
  getTitle: (node: T) => string,
}

export default class TreeNode extends Component {
  props: Props<*>;

  state = {
    expanded: false,
  };

  static defaultProps = {
    onClick: noop,
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
      'category': !children,
      'category-expand': children && !this.state.expanded,
      'category-collapse': children && this.state.expanded,
    });

    return <Icon className={cls} onClick={this.toggleExpanded} />;
  }

  get label() {
    const { node, getTitle } = this.props;

    return <span styleName="text">{getTitle(node.node)}</span>;
  }

  get children(): ?Array<Element<*>> {
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

  get node(): Element<*> {
    const { node, depth, currentObjectId, onClick } = this.props;
    const active = node.node.id.toString() === currentObjectId;

    const className = classNames(styles.node, {
      [styles.active]: active,
    });

    const style = { paddingLeft: `${depth}px` };

    return (
      <div className={className}>
        <div styleName="item" onClick={() => onClick(node.node.id)} style={style}>
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

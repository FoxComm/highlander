/* @flow */

// libs
import classNames from 'classnames';
import { each, filter, find, get, isEmpty, values } from 'lodash';
import { assoc, update } from 'sprout-data';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import MultiSelectTable from 'components/table/multi-select-table';
import { Button } from 'components/common/buttons';

// styles
import styles from './tree-table.css';

type Identifier = number|string;

type Node<T> = {
  row: T,
  id: Identifier,
  parentId: ?Identifier,
  collapsed: boolean,
  children: Array<Node<T>>,
  level: number,
};

type Props<T> = {
  columns: Columns,
  data: {
    rows: Array<T>,
    total: number,
    from: number,
    size: number,
  },
  collapseField: string,
  renderRow: (row: T, index: number, columns: Columns, params: Object) => Element<*>,
  setState: (state: Object) => any,
  hasActionsColumn: boolean,
  isLoading: boolean,
  failed: boolean,
  emptyMessage: string,
  className: string,
  idField: string,
  headerControls: Array<Element<*>>,
};

type State<T> = {
  root: Tree<T>,
}

type Tree<T> = { [key: Identifier]: Node<T> };

function buildTree<T>(arr: Array<T>, idField: string) {
  const tree: Tree<T> = {};

  const acc: Tree<T> = arr.reduce((acc: Acc<T>, obj: T) => assoc(acc, [obj[idField]], {
    row: obj,
    id: obj[idField],
    parentId: obj.parentId,
    collapsed: true,
    children: [],
    level: 0,
  }), {});

  values(acc).forEach(function (obj: Node<T>) {
    if (obj.parentId) {
      acc[obj.parentId].children.push(obj);
    } else {
      tree[obj.id] = obj;
    }
  });

  return tree;
}

function updateNodes(tree: Tree<T>, updater: (node: Node<T>) => void) {
  const traverse = (nodes: Array<Node<T>>) =>
    nodes.forEach((node: Node<T>) => {
      if (node.children) traverse(node.children);

      updater(node);
    });

  traverse(values(tree));

  return tree;
}

const toggleAll = (tree: Tree<T>, collapse: boolean) => updateNodes(tree, (n: Node<T>) => n.collapsed = collapse);

class TreeTable<T> extends Component {
  props: Props;

  state: State = {
    root: {},
  };

  static defaultProps = {
    idField: 'id',
    collapseField: 'name',
    isLoading: false,
    failed: false,
    className: '',
  };

  constructor(props: Props) {
    super(props);

    this.state.root = buildTree(props.data.rows, props.idField);
  }

  componentWillReceiveProps(nextProps: Props) {
    this.setState({ root: buildTree(nextProps.data.rows, nextProps.idField) });
  }

  @autobind
  expandAll() {
    this.setState({ root: toggleAll(this.state.root, false) });
  }

  @autobind
  collapseAll() {
    this.setState({ root: toggleAll(this.state.root, true) });
  }

  get rows(): Array<Node<T>> {
    const _reduce = (level: number) => (rows: T, node: Node<T>) => {
      const children = !node.collapsed ? node.children.reduce(_reduce(level + 1), []) : [];

      rows.push(assoc(node, 'level', level), ...children);

      return rows;
    };

    return values(this.state.root).reduce(_reduce(0), []);
  }

  @autobind
  handleCollapse(node: Node<T>, event: MouseEvent) {
    event.preventDefault();
    event.stopPropagation();

    node.collapsed = !node.collapsed;

    this.forceUpdate();
  }

  @autobind
  renderRow(node: Node<T>, index: number, columns: Columns, params: Object) {
    const el = this.props.renderRow(node.row, index, columns, params);

    const processCell = (content: Element<*>, col: Column) => {
      const collapsible = node.children.length;
      const collapsed = node.collapsed;
      const level = node.level;

      let cellContents = content;

      if (col.field === this.props.collapseField) {
        const iconClassName = classNames(({
          'icon-category': !collapsible,
          'icon-category-expand': collapsible && collapsed,
          'icon-category-collapse': collapsible && !collapsed,
        }));

        cellContents = (
          <span className="fc-collapse" style={{ paddingLeft: `${level * 20}px`}}>
              <i className={iconClassName} onClick={this.handleCollapse.bind(this, node)} />
            {content}
            </span>
        );
      }

      return cellContents;
    };

    return React.cloneElement(el, { processCell });
  }

  get headerControls() {
    return [
      <Button className={styles.headerButton} onClick={this.expandAll}>Expand All</Button>,
      <Button className={styles.headerButton} onClick={this.collapseAll}>Collapse All</Button>,
      ...this.props.headerControls,
    ];
  }

  render() {
    const { data } = this.props;

    return (
      <div className={styles.tree}>
        <MultiSelectTable
          {...this.props}
          data={{
          ...data,
          rows: this.rows,
        }}
          headerControls={this.headerControls}
          renderRow={this.renderRow}
        />
      </div>
    );
  }
}

export default TreeTable;

/* @flow */

// libs
import classNames from 'classnames';
import { each, filter, find, get, isEmpty, values } from 'lodash';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import MultiSelectTable from 'components/table/multi-select-table';
import { Button } from 'components/common/buttons';

// styles
import styles from './tree-table.css';

type Identifier = number|string;

type Row = { [key: string]: string|number };

type Node = {
  row: Row,
  id: Identifier,
  parentId: ?Identifier,
  collapsed: boolean,
  children: Array<Node>,
  level: number,
};

type Tree = { [key: Identifier]: Node };

type Props = {
  columns: Columns,
  data: {
    rows: Array<Row>,
    total: number,
    from: number,
    size: number,
  },
  collapseField: string,
  renderRow: (row: any, index: number, columns: Columns, params: Object) => Element<*>,
  setState?: (state: Object) => any,
  hasActionsColumn: boolean,
  isLoading: boolean,
  failed: boolean,
  emptyMessage: string,
  className: string,
  idField: string,
  headerControls: Array<Element<*>>,
};

type State = {
  root: Tree,
}

function buildTree(arr: Array<Row>, idField: string) {
  const tree: Tree = {};

  const acc: Tree = arr.reduce((acc: Tree, row: Row) => assoc(acc, row[idField], {
    row: row,
    id: row[idField],
    parentId: row['parentId'],
    collapsed: true,
    children: [],
    level: 0,
  }), {});

  values(acc).forEach(function (node: Node) {
    if (node.parentId) {
      acc[node.parentId].children.push(node);
    } else {
      tree[node.id] = node;
    }
  });

  return tree;
}

function updateNodes(tree: Tree, updater: (node: Node) => any) {
  const traverse = (nodes: Array<Node>) =>
    nodes.forEach((node: Node) => {
      if (node.children) traverse(node.children);

      updater(node);
    });

  traverse(values(tree));

  return tree;
}

const toggleAll = (tree: Tree, collapse: boolean) => updateNodes(tree, (n: Node) => n.collapsed = collapse);

class TreeTable extends Component {
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

  get rows(): Array<Node> {
    const _reduce = (level: number) => (nodes: Array<Node>, node: Node) => {
      const children = !node.collapsed ? node.children.reduce(_reduce(level + 1), []) : [];

      nodes.push(assoc(node, 'level', level), ...children);

      return nodes;
    };

    return values(this.state.root).reduce(_reduce(0), []);
  }

  @autobind
  handleCollapse(node: Node, event: MouseEvent) {
    event.preventDefault();
    event.stopPropagation();

    node.collapsed = !node.collapsed;

    this.forceUpdate();
  }

  @autobind
  renderRow(node: Node, index: number, columns: Columns, params: Object) {
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

  get headerControls(): Array<Element<*>> {
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

/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import classNames from 'classnames';
import flatMap from '../../lib/flatMap';
import { autobind } from 'core-decorators';

import TableHead from './head';
import TableRow from './row';
import TableCell from './cell';
import WaitAnimation from '../common/wait-animation';

export function tableMessage(message: Element|string, inline: boolean = false): Element {
  const cls = classNames('fc-table-message', { 'fc-table-message__inline': inline });

  return (
    <div className={cls}>
      <div className="fc-content-box__empty-row">
        {message}
      </div>
    </div>
  );
}

type RowType = Object;
type Rows = Array<RowType>;
type Column = {
  type: string;
  field?: string;
}

type Props = {
  data: {
    rows: Rows,
    sortBy?: string,
    from?: number,
    size?: number,
    total?: number,
  };
  renderRow: (row: RowType, index: number, isNew: ?boolean) => ?Element;
  setState: Function;
  predicate: (row: RowType) => string|number;
  processRows: (rows: Array<Element>) => Element;
  detectNewRows?: boolean;
  isLoading?: boolean;
  failed?: boolean;
  emptyMessage?: string;
  errorMessage?: string;
  className?: string;
  showLoadingOnMount?: boolean;
  wrapToTbody: boolean;
  columns: Array<Column>;
}

type State = {
  newIds: Array<string|number>;
}

const ROWS_COUNT_TO_SHOW_LOADING_OVERLAY = 4;

export default class Table extends Component {
  props: Props;

  static defaultProps = {
    columns: [],
    data: {
      rows: [],
      from: 0,
      size: 0,
      total: 0
    },
    showLoadingOnMount: true,
    isLoading: false,
    failed: false,
    errorMessage: 'An error occurred. Try again later.',
    emptyMessage: 'No data found.',
    predicate: entity => entity.id,
    processRows: _.identity,
    detectNewRows: false,
    wrapToTbody: true,
  };

  state: State = {
    newIds: [],
  };

  _el: HTMLElement;
  _head: HTMLElement;
  _body: HTMLElement;

  componentDidUpdate(): void {
    const tables = this._el.getElementsByClassName('fc-table');

    [].forEach.call(tables, (table: HTMLElement) => {
      table.style.tableLayout = 'auto';
    });

    const rows = this._body.getElementsByClassName('fc-table-tr');

    if (rows.length === 0) {
      return;
    }

    const headerCells = this._head.getElementsByClassName('fc-table-th');
    const cells = this._body.getElementsByClassName('fc-table-td');

    const cellsInARow = cells.length / rows.length;

    for (let cellIndex = 0; cellIndex < cellsInARow; cellIndex++) {

      const headerCellWidth = headerCells[cellIndex].getBoundingClientRect().width;
      const columnMaxWidth = _.range(0, rows.length)
        .map((rowIndex: number) => cells[rowIndex * cellsInARow + cellIndex].getBoundingClientRect().width)
        .reduce((prevRow: number, currRow: number) => Math.max(prevRow, currRow), 0);

      const max = Math.max(columnMaxWidth, headerCellWidth);

      for (let i = cellIndex; i < cells.length; i += cellsInARow) {
        cells[i].style.width = `${max}px`;
        cells[i].style.minWidth = `${max}px`;
      }

      headerCells[cellIndex].style.width = `${max}px`;
      headerCells[cellIndex].style.minWidth = `${max}px`;
    }

    [].forEach.call(tables, (table: HTMLElement) => {
      table.style.tableLayout = 'fixed';
    });

  }

  get rows(): Rows {
    return this.props.data.rows;
  }

  @autobind
  onScroll(e: MouseEvent): void {
    e.preventDefault();

    const el = e.target == this._head ? this._body : this._head;

    el.scrollLeft = e.target.scrollLeft;
  }

  @autobind
  defaultRenderRow(row: RowType, index: number, isNew: ?boolean): Element {
    const rowKey = this.props.predicate && this.props.predicate(row) || index;
    return (
      <TableRow key={`row-${rowKey}`} isNew={isNew}>
        {this.props.columns.map((column, cellIdx) => {
          return (
            <TableCell column={column} key={`cell-${cellIdx}`}>
              {row[column.field]}
            </TableCell>
          );
        })}
      </TableRow>
    );
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.detectNewRows) {
      let newIds = [];

      if (this.props.predicate && nextProps.data.rows && nextProps.data.rows !== this.rows) {

        const oldRows = _.indexBy(this.rows, this.props.predicate);
        const newRows = _.indexBy(nextProps.data.rows, this.props.predicate);

        newIds = _.difference(_.keys(newRows), _.keys(oldRows));
      }
      this.setState({ newIds });
    }
  }

  get loadingInline(): boolean {
    return this.rows.length >= ROWS_COUNT_TO_SHOW_LOADING_OVERLAY;
  }

  get tableRows(): Element {
    const { props } = this;

    const renderRow = props.renderRow || this.defaultRenderRow;

    const rows = flatMap(this.rows, ((row, index) => {
      const isNew = props.detectNewRows &&
        props.predicate &&
        (this.state.newIds.indexOf(String(props.predicate(row))) != -1);

      return renderRow(row, index, isNew);
    }));

    return props.processRows(rows, props.columns);
  }

  get message(): ?Element {
    const { props } = this;

    const showLoading = props.showLoadingOnMount && props.isLoading === null || props.isLoading;
    const rows = this.tableRows;

    if (showLoading) {
      return tableMessage(<WaitAnimation />, this.loadingInline);
    } else if (props.failed && props.errorMessage) {
      return tableMessage(props.errorMessage);
    } else if (_.isEmpty(rows) && props.emptyMessage) {
      return tableMessage(props.emptyMessage);
    }
  }

  get body(): ?Element {
    const rowsCount = this.rows.length;
    const isLoading = this.props.isLoading;

    if (!isLoading && rowsCount || isLoading && this.loadingInline) {
      return <div className="fc-table-body">{this.tableRows}</div>;
    }
  }

  render() {
    const {data, setState, className, ...rest} = this.props;

    return (
      <div className="fc-table-wrap" ref={(el) => this._el = el}>
        <div className="fill-width inner inner-head" ref={(r) => this._head = r} onScroll={this.onScroll}>
          <div className={classNames('fc-table', className)}>
            <TableHead {...rest} sortBy={data.sortBy} setState={setState} />
          </div>
        </div>

        <div className="fill-width inner" ref={(r) => this._body = r} onScroll={this.onScroll}>
          <div className={classNames('fc-table', className)}>
            {this.body}
          </div>
        </div>
        {this.message}
      </div>
    );
  }
};

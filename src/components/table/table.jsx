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
const SELECTOR_CELLS_HEAD = '.fc-table-tr:first-child .fc-table-th';
const SELECTOR_CELLS_BODY = '.fc-table-tr:first-child .fc-table-td';

function getElements(el: HTMLElement, selector: string) {
  return [].slice.call(el.querySelectorAll(selector));
}

/**
 * Reset columns widths to initial value for further recalculation of summary width
 *
 * @param {Array<HTMLElement>} headerCells Header row
 * @param {Array<HTMLElement>} bodyCells First(any) row from body
 */
function resetColumnsWidths(headerCells: Array<HTMLElement>, bodyCells: Array<HTMLElement>) {
  _.range(0, headerCells.length).forEach((index: number) => {
    headerCells[index].style.width = headerCells[index].style.minWidth = 'initial';

    if (!_.isEmpty(bodyCells)) {
      bodyCells[index].style.width = bodyCells[index].style.minWidth = 'initial';
    }
  });
}

/**
 * Set columns widths
 *
 * @param {Array<HTMLElement>} headerCells Header row
 * @param {Array<HTMLElement>} bodyCells First(any) row from body
 * @param {Array<Number>} columnWidths Array of widths of columns
 * @param {Boolean} stretchRequired If table content needs to be stretch to fit container
 */
function setColumnsWidths(headerCells: Array<HTMLElement>,
                          bodyCells: Array<HTMLElement>,
                          columnWidths: Array<number>,
                          stretchRequired: boolean) {
  _.range(1, headerCells.length).forEach((index: number) => {
    const isLast = index === headerCells.length - 1;
    const headerCellWidth = !isLast ? `${columnWidths[index]}px` : '100%';

    headerCells[index].style.width = headerCells[index].style.minWidth = headerCellWidth;

    if (!_.isEmpty(bodyCells)) {
      const bodyCellWidth = !stretchRequired || !isLast ? `${columnWidths[index]}px` : '100%';

      bodyCells[index].style.width = bodyCells[index].style.minWidth = bodyCellWidth;
    }
  });
}

/**
 * Calculate adjusted columns width for table header and body.
 * We need max width of header and body cells from one column
 * (i.e. max(headerRow[colIndex].width, bodyRow[colIndex].width))
 *
 * @param {Array<HTMLElement>} headerCells Header row
 * @param {Array<HTMLElement>} bodyCells First(any) row from body
 *
 * @return {Array<Number>} Array of adjusted columns widths
 */
function getAdjustedColumnsWidths(headerCells: Array<HTMLElement>, bodyCells: Array<HTMLElement>) {
  /* add 1px for each column to prevent 1px difference in case of wrong elements dimensions recalculation */
  const headerWidths = headerCells.map((cell: HTMLElement) => cell.clientWidth + 1);
  const bodyWidths = bodyCells.map((cell: HTMLElement) => cell.clientWidth + 1);

  return _.range(0, headerWidths.length).map((index: number) => {
    return Math.max(headerWidths[index], _.get(bodyWidths, index, 0));
  });
}

/**
 * Look if we need to stretch table to fit host container width
 *
 * @param {Array<Number>} columnWidths
 * @param {Number} hostWidth
 *
 * @return Number
 */
function isStretchRequired(columnWidths: Array<number>, hostWidth: number) {
  return hostWidth > columnWidths.reduce((a, b) => a + b, 0);
}

/**
 * If the sum of all adjusted columns widths less than host element width we need to stretch table columns
 *
 * @param {Array<HTMLElement>} headerCells Header row
 * @param {Array<Number>} columnWidths
 * @param {Number} hostWidth
 *
 * @return Number
 */
function getStretchedColumnsWidths(headerCells: Array<HTMLElement>, columnWidths: Array<number>, hostWidth: number) {
  const widthLack = hostWidth - columnWidths.reduce((a, b) => a + b, 0);
  const extra = Math.floor(widthLack / (headerCells.length - 2));

  return columnWidths.map((width: number, index: number) => index < columnWidths.length - 1 ? width + extra : width);
}

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

  componentDidMount(): void {
    window.addEventListener('resize', this.resize);
  }

  componentWillUnmount(): void {
    window.removeEventListener('resize', this.resize);
  }

  componentDidUpdate(prevProps: Props): void {
    if (prevProps.data.rows !== this.props.data.rows) {
      this.resize();
    }
  }

  @autobind
  resize(): void {
    const headerCells = getElements(this._head, SELECTOR_CELLS_HEAD);
    const bodyCells = getElements(this._body, SELECTOR_CELLS_BODY);

    resetColumnsWidths(headerCells, bodyCells);

    const hostWidth = this._body.clientWidth;
    const widths = getAdjustedColumnsWidths(headerCells, bodyCells, hostWidth);
    const stretchRequired = isStretchRequired(widths, hostWidth);
    const adjustedWidths = !stretchRequired ? widths : getStretchedColumnsWidths(headerCells, widths, hostWidth);

    setColumnsWidths(headerCells, bodyCells, adjustedWidths, stretchRequired);

    this._body.scrollLeft = this._head.scrollLeft = 0;
  }

  get rows(): Rows {
    return this.props.data.rows;
  }


  @autobind
  onScroll(e: any /* e.target.scrollLeft throws an type error in flow with (e: Event) declaration :( */): void {
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
    const { data, setState, className, ...rest } = this.props;

    return (
      <div className="fc-table-wrap" ref={(el) => this._el = el}>
        <div className="inner inner-head" ref={(r) => this._head = r} onScroll={this.onScroll}>
          <div className={classNames('fc-table', className)}>
            <TableHead {...rest} sortBy={data.sortBy} setState={setState} />
          </div>
        </div>

        <div className="inner" ref={(r) => this._body = r} onScroll={this.onScroll}>
          <div className={classNames('fc-table', className)}>
            {this.body}
          </div>
        </div>
        {this.message}
      </div>
    );
  }
};

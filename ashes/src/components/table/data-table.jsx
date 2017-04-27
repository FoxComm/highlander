import _ from 'lodash';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React from 'react';

import Table from './table';
import TableHead from './head';

import type Props from './table';

const SELECTOR_CELLS_HEAD = '.fc-table-tr:first-child .fc-table-th';
const SELECTOR_CELLS_BODY = '.fc-table-tr:first-child .fc-table-td';

function getElements(el: HTMLElement<*>, selector: string) {
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
  _.range(0, headerCells.length).forEach((index: number) => {
    const isLast = index === headerCells.length - 1;
    const cellWidth = !isLast ? `${columnWidths[index]}px` : '100%';
    const cellMinWidth = `${columnWidths[index]}px`;

    headerCells[index].style.width = cellWidth;
    headerCells[index].style.minWidth = cellMinWidth;

    if (!_.isEmpty(bodyCells)) {
      bodyCells[index].style.width = cellWidth;
      bodyCells[index].style.minWidth = cellMinWidth;
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
 * If the sum of all adjusted columns widths less than host Element<*>width we need to stretch table columns
 *
 * @param {Array<HTMLElement>} headerCells Header row
 * @param {Array<Number>} columnWidths
 * @param {Number} hostWidth
 *
 * @return Number
 */
function getStretchedColumnsWidths(headerCells: Array<HTMLElement>, columnWidths: Array<number>, hostWidth: number) {
  const widthLack = hostWidth - columnWidths.reduce((a, b) => a + b, 0);
  const extra = Math.floor(widthLack / (headerCells.length - 1));

  return columnWidths.map((width: number, index: number) => index < columnWidths.length - 1 ? width + extra : width);
}

class DataTable extends Table {

  _el: HTMLElement;
  _head: HTMLElement;
  _body: HTMLElement;

  scrollCache: Number = 0;

  componentDidMount(): void {
    window.addEventListener('resize', this.resize);

    this._body.addEventListener('scroll', this.handleBodyScroll);
    this._head.addEventListener('scroll', this.handleHeadScroll);
    this.resize();
  }

  componentWillUnmount(): void {
    window.removeEventListener('resize', this.resize);

    this._body.removeEventListener('scroll', this.handleBodyScroll);
    this._head.removeEventListener('scroll', this.handleHeadScroll);
  }

  componentDidUpdate(prevProps: Props): void {
    super.componentDidUpdate(prevProps);
    const dataChanged = prevProps.data.rows !== this.props.data.rows;
    const columnsSetChanged = prevProps.columns.length !== this.props.columns.length;

    if (dataChanged || columnsSetChanged) {
      this.resize();
    }
  }

  @autobind
  handleHeadScroll({ target }: SyntheticEvent): void {
    if (target.scrollLeft !== this.scrollCache) {
      this._body.scrollLeft = this.scrollCache = target.scrollLeft;
    }
  }

  @autobind
  handleBodyScroll({ target }: SyntheticEvent): void {
    if (target.scrollLeft !== this.scrollCache) {
      this._head.scrollLeft = this.scrollCache = target.scrollLeft;
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

  render() {
    const { data, setState, className, ...rest } = this.props;

    return (
      <div className={classNames('fc-data-table', className)}>
        <div className="fc-table-wrap" ref={(el) => this._el = el}>
          <div className="inner inner-head" ref={(r) => this._head = r}>
            <table className="fc-table">
              <TableHead {...rest} ref="tableHead" sortBy={data.sortBy} setState={setState} />
            </table>
          </div>

          <div className="inner" ref={(r) => this._body = r}>
            <table className="fc-table">
              {this.body}
            </table>
          </div>
          {this.message(_.isEmpty(this.tableRows))}
        </div>
      </div>
    );
  }
}

export default DataTable;

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

export type Props = {
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

  get rows(): Rows {
    return this.props.data.rows;
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
      return this.wrapBody(this.tableRows);
    }
  }

  wrapBody(body: Element): Element {
    const firstRow = React.Children.toArray(body)[0];
    if (firstRow && (firstRow.type === 'tbody' || !this.props.wrapToTbody)) {
      return body;
    }

    return <tbody className="fc-table-body">{body}</tbody>;
  }

  render() {
    const { data, setState, className, ...rest } = this.props;

    return (
      <div>
        <table className={classNames('fc-table', className)}>
          <TableHead {...rest} sortBy={data.sortBy} setState={setState} />
          {this.body}
        </table>
        {this.message}
      </div>
    );
  }
};

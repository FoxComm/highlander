'use strict';

import React from 'react';

export default class TablePaginator extends React.Component {
  onPrevPageClick() {
    this.props.setStart(Math.max(0, this.props.start - this.props.limit));
  }

  onNextPageClick() {
    this.props.setStart(Math.min(this.props.total - this.props.limit, this.props.start + this.props.limit));
  }

  render() {
    let total = this.props.total;
    let start = this.props.start + 1;
    let end = Math.min(total, this.props.start + this.props.limit);
    return (
      <div className="fc-table-paginator">
        <span>
          {start}&thinsp;-&thinsp;{end} of {total}
        </span>
        &nbsp;
        <button onClick={this.onPrevPageClick.bind(this)}><i className="fa fa-arrow-left"/></button>
        &nbsp;
        <button onClick={this.onNextPageClick.bind(this)}><i className="fa fa-arrow-right"/></button>
      </div>
    );
  }
}

TablePaginator.propTypes = {
  columns: React.PropTypes.array,
  rows: React.PropTypes.array,
  start: React.PropTypes.number,
  limit: React.PropTypes.number,
  total: React.PropTypes.number,
  setStart: React.PropTypes.func
};

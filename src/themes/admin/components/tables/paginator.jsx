'use strict';

import React from 'react';

export default class TableView extends React.Component {
  onPrevPageClick() {
    this.props.setStart(Math.max(0, this.props.start - this.props.limit));
  }

  onNextPageClick() {
    this.props.setStart(Math.min(this.props.total - this.props.limit, this.props.start + this.props.limit));
  }

  render() {
    return (
      <div className="fc-table-paginator">
        <span>
          {this.props.start + 1}&thinsp;-&thinsp;{this.props.start + this.props.limit} of {this.props.total}
        </span>
        &nbsp;
        <button onClick={this.onPrevPageClick.bind(this)}><i className="fa-arrow-left"/></button>
        &nbsp;
        <button onClick={this.onNextPageClick.bind(this)}><i className="fa-arrow-right"/></button>
      </div>
    );
  }
}

TableView.propTypes = {
  columns: React.PropTypes.array,
  rows: React.PropTypes.array,
  start: React.PropTypes.number,
  limit: React.PropTypes.number,
  total: React.PropTypes.number,
  setStart: React.PropTypes.func
};

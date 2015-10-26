'use strict';

import React from 'react';

export default class TablePaginator extends React.Component {
  static propTypes = {
  };

  onPrevPageClick() {
    this.props.data.setStart(this.props.data.start - this.props.data.limit);
  }

  onNextPageClick() {
    this.props.data.setStart(this.props.data.start + this.props.data.limit);
  }

  render() {
    let total = this.props.data.models.length;
    let start = this.props.data.start + 1;
    let end = Math.min(total, this.props.data.start + this.props.data.limit);
    return (
      <div className="fc-table-paginator">
        <span>
          {start}&thinsp;-&thinsp;{end} of {total}
        </span>
        &nbsp;
        <button onClick={this.onPrevPageClick.bind(this)}><i className="icon-chevron-left"/></button>
        &nbsp;
        <button onClick={this.onNextPageClick.bind(this)}><i className="icon-chevron-right"/></button>
      </div>
    );
  }
}

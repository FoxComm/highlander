'use strict';

import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

export default class TablePaginator extends React.Component {
  static propTypes = {
    total: PropTypes.number.isRequired,
    from: PropTypes.number.isRequired,
    size: PropTypes.number.isRequired,
    setFrom: PropTypes.func.isRequired
  };

  @autobind
  onPrevPageClick() {
    this.props.setFrom(this.props.from - this.props.size);
  }

  @autobind
  onNextPageClick() {
    this.props.setFrom(this.props.from + this.props.size);
  }

  render() {
    let total = this.props.total;
    let from = this.props.from + 1;
    let end = Math.min(total, this.props.from + this.props.size);
    return (
      <div className="fc-table-paginator">
        <span>
          {from}&thinsp;-&thinsp;{end} of {total}
        </span>
        &nbsp;
        <button className="fc-btn" onClick={this.onPrevPageClick}><i className="icon-chevron-left"/></button>
        &nbsp;
        <button className="fc-btn" onClick={this.onNextPageClick}><i className="icon-chevron-right"/></button>
      </div>
    );
  }
}

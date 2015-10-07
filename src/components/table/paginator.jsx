'use strict';

import React from 'react';
import TableStore from '../../lib/table-store';

export default class TablePaginator extends React.Component {
  onPrevPageClick() {
    this.props.store.setStart(this.props.store.start - this.props.store.limit);
  }

  onNextPageClick() {
    this.props.store.setStart(this.props.store.start + this.props.store.limit);
  }

  render() {
    let total = this.props.store.models.length;
    let start = this.props.store.start + 1;
    let end = Math.min(total, this.props.store.start + this.props.store.limit);
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

TablePaginator.propTypes = {
  store: React.PropTypes.instanceOf(TableStore)
};

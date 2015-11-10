import React, { PropTypes } from 'react';

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
        <button onClick={this.onPrevPageClick.bind(this)}><i className="icon-chevron-left"/></button>
        &nbsp;
        <button onClick={this.onNextPageClick.bind(this)}><i className="icon-chevron-right"/></button>
      </div>
    );
  }
}

TablePaginator.propTypes = {
  columns: PropTypes.array,
  rows: PropTypes.array,
  start: PropTypes.number,
  limit: PropTypes.number,
  total: PropTypes.number,
  setStart: PropTypes.func
};

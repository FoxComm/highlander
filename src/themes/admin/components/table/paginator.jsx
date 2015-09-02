'use strict';

import React from 'react';

export default class TableView extends React.Component {
  render() {
    return (
      <div className="fc-table-header">
        <span>
          {this.props.start} - {this.props.end} of {this.props.total}
        </span>
        &nbsp;
        <button><i className="fa-arrow-left"/></button>
        &nbsp;
        <button><i className="fa-arrow-right"/></button>
      </div>
    );
  }
}

TableView.propTypes = {
  columns: React.PropTypes.array,
  rows: React.PropTypes.array
};

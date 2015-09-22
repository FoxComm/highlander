'use strict';

import React from 'react';
import TableView from '../tables/tableview';

export default class LineItems extends React.Component {
  render() {
    return (
      <section className="line-items fc-content-box">
        <header className="header">
          <span>Items</span>
        </header>
        <TableView
          columns={this.props.tableColumns}
          rows={this.props.entity.lineItems}
          model={this.props.model}
          />
      </section>
    );
  }
}

LineItems.propTypes = {
  entity: React.PropTypes.object,
  tableColumns: React.PropTypes.array,
  model: React.PropTypes.string
};

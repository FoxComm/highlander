'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import LineItemCounter from './line-item-counter';
import DeleteLineItem from './line-item-delete';
import SkuStore from '../orders/sku-store';
import SkuResult from '../orders/sku-result';
import Typeahead from '../typeahead/typeahead';


const orderDefaultColumns = [
  {field: 'imagePath', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'sku', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'quantity', text: 'Qty'},
  {field: 'total', text: 'Total', type: 'currency'}
];

const orderEditColumns = [
  {field: 'imagePath', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'sku', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'lineItem', text: 'Qty', component: 'LineItemCounter'},
  {field: 'total', text: 'Total', type: 'currency'},
  {field: 'delete', text: 'Delete', component: 'DeleteLineItem'}
];

export default class LineItems extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      isEditing: false
    };
  }

  toggleEdit() {
    this.setState({
      isEditing: !this.state.isEditing
    });
  }

  itemSelected(sku) {
    if (this.props.onChange) {
      this.props.onChange([{'sku': sku.sku, 'quantity': 1}]);
    }
  }

  render() {
    let actions = null;
    let columns = this.props.tableColumns;
    let rows = this.props.entity.lineItems;
    let body = (
      <TableView
        columns={columns}
        rows={rows}
        model={this.props.model}
        />
    );
    let header = (
      <header>
        <span>Items</span>
      </header>
    );

    if (this.props.model === 'order') {
      if (this.state.isEditing) {
        columns = orderEditColumns;
        body = (
          <TableView columns={columns} rows={rows} model="lineItem">
            <LineItemCounter onChange={this.props.onChange} />
            <DeleteLineItem onDelete={this.props.onChange} />
          </TableView>
        );
        actions = (
          <footer>
            <div>
              <strong>Add Item</strong>
              <Typeahead callback={this.itemSelected.bind(this)} component={SkuResult} store={SkuStore} />
            </div>
            <button className="fc-btn fc-btn-primary" onClick={this.toggleEdit.bind(this)}>Done</button>
          </footer>
        );
      } else {
        columns = orderDefaultColumns;
        header = (
          <header>
            <div className='fc-grid'>
              <div className="fc-col-2-3">Items</div>
              <div className="fc-col-1-3 fc-align-right">
                <button className="fc-btn" onClick={this.toggleEdit.bind(this)}>
                  <i className="icon-edit"></i>
                </button>
              </div>
            </div>
          </header>
        );
        body = (
          <TableView
            columns={columns}
            rows={rows}
            model={this.props.model}
            />
        );
      }
    }

    return (
      <section className="fc-line-items fc-content-box">
        {header}
        {body}
        {actions}
      </section>
    );
  }
}

LineItems.propTypes = {
  entity: React.PropTypes.object,
  tableColumns: React.PropTypes.array,
  model: React.PropTypes.string,
  onChange: React.PropTypes.func
};

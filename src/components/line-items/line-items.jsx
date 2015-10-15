'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import LineItemCounter from './line-item-counter';
import LineItemActions from '../../actions/line-items';
import DeleteLineItem from './line-item-delete';
import SkuStore from '../../stores/skus';
import SkuResult from '../orders/sku-result';
import Typeahead from '../typeahead/typeahead';

export default class LineItems extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      isEditing: false
    };
  }

  get orderDefaultColumns () {
    return [
      {field: 'imagePath', text: 'Image', type: 'image'},
      {field: 'name', text: 'Name'},
      {field: 'sku', text: 'SKU'},
      {field: 'price', text: 'Price', type: 'currency'},
      {field: 'quantity', text: 'Qty'},
      {field: 'total', text: 'Total', type: 'currency'}
    ];
  }

  get orderEditColumns () {
    return [
      {field: 'imagePath', text: 'Image', type: 'image'},
      {field: 'name', text: 'Name'},
      {field: 'sku', text: 'SKU'},
      {field: 'price', text: 'Price', type: 'currency'},
      {field: 'lineItem', text: 'Qty', component: 'LineItemCounter'},
      {field: 'total', text: 'Total', type: 'currency'},
      {field: 'delete', text: 'Delete', component: 'DeleteLineItem'}
    ];
  }

  toggleEdit() {
    this.setState({
      isEditing: !this.state.isEditing
    });
  }

  itemSelected(sku) {
    LineItemActions.editLineItems(
      this.props.model,
      this.props.entity.referenceNumber,
      [{'sku': sku.sku, 'quantity': 1}]
    );
  }

  render() {
    let actions = null;
    let columns = this.props.tableColumns;
    let rows = this.props.entity.lineItems.skus;
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
        columns = this.orderEditColumns;
        header = (
          <header>
            <div className='fc-grid'>
              <div className="fc-col-md-2-3">Items</div>
              <div className="fc-col-md-1-3 fc-align-right">
                <button className="fc-right fc-btn fc-btn-plain icon-chevron-up" onClick={this.toggleEdit.bind(this)}>
                </button>
                <div className="fc-line-items-count fc-right">
                  5 items
                </div>
              </div>
            </div>
          </header>
        );
        body = (
          <TableView columns={columns} rows={rows} model="lineItem">
            <LineItemCounter entityName={this.props.model} entity={this.props.entity} />
            <DeleteLineItem entityName={this.props.model} entity={this.props.entity} />
          </TableView>
        );
        actions = (
          <footer className="fc-order-line-items-footer">
            <div>
              <strong>Add Item</strong>
              <Typeahead callback={this.itemSelected.bind(this)} component={SkuResult} store={SkuStore} />
            </div>
            {/*<button className="fc-btn fc-btn-primary" onClick={this.toggleEdit.bind(this)}>Done</button>*/}
          </footer>
        );
      } else {
        columns = this.orderDefaultColumns;
        header = (
          <header>
            <div className='fc-grid'>
              <div className="fc-col-md-2-3">Items</div>
              <div className="fc-col-md-1-3 fc-align-right">
                <button className="fc-btn fc-btn-plain fc-right icon-chevron-down" onClick={this.toggleEdit.bind(this)}>
                </button>
                <div className="fc-line-items-count fc-right">
                  5 items
                </div>
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
  model: React.PropTypes.string
};

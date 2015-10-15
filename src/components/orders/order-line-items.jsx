'use strict';

import React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as actionCreators from '../../actions/order-line-items';

import TableView from '../tables/tableview';
import TableHead from '../tables/head';
import OrderLineItem from './order-line-item';
import SkuStore from '../../stores/skus';
import SkuResult from './sku-result';
import Typeahead from '../typeahead/typeahead';

function mapStateToProps(state) {
  return {
    lineItems: state.orderLineItems || {}
  };
}

function mapDispatchToProps(dispatch) {
  return { actions: bindActionCreators(actionCreators, dispatch) };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class OrderLineItems extends React.Component {
  constructor(props, context) {
    super(props, context);
  }

  static defaultProps = {
    viewColumns: [
      {field: 'imagePath', text: 'Image', type: 'image'},
      {field: 'name', text: 'Name'},
      {field: 'sku', text: 'SKU'},
      {field: 'price', text: 'Price', type: 'currency'},
      {field: 'quantity', text: 'Qty'},
      {field: 'totalPrice', text: 'Total', type: 'currency'}
    ],
    editColumns: [
      {field: 'imagePath', text: 'Image', type: 'image'},
      {field: 'name', text: 'Name'},
      {field: 'sku', text: 'SKU'},
      {field: 'price', text: 'Price', type: 'currency'},
      {field: 'lineItem', text: 'Qty', component: 'LineItemCounter'},
      {field: 'totalPrice', text: 'Total', type: 'currency'},
      {field: 'delete', text: 'Delete', component: 'DeleteLineItem'}
    ]
  }

  get confirmModal() {
    if (this.props.lineItems.isDeleting) {
      return <h2>PLACEHOLDER FOR SHOWING DELETE MODAL</h2>;
    } else {
      return <div></div>;
    }
  }

  itemSelected(sku) {
    console.log('Item selected');
  }

  editLineItems() {
    this.props.actions.orderLineItemsEdit(this.props.entity);
  }

  cancelEditLineItems() {
    this.props.actions.orderLineItemsCancelEdit();
  }

  render() {
    let orderLineItems = this.props.entity.lineItems.skus.map((lineItem, idx) => {
      return (<OrderLineItem item={lineItem} />);
    });

    if (this.props.lineItems.isEditing) {
      return (
        <section className='fc-line-items fc-content-box'>
          <table className='fc-table'>
            <TableHead columns={this.props.editColumns} />
            <tbody>
              {orderLineItems}
            </tbody>
          </table>
          <footer>
            <div>
              <strong>Add Item</strong>
              <Typeahead callback={this.itemSelected.bind(this)} component={SkuResult} store={SkuStore} />
            </div>
            <button className='fc-btn fc-btn-primary' onClick={this.cancelEditLineItems.bind(this)}>Done</button>
          </footer>
          {this.confirmModal}
        </section>
      );
    } else {
      return (
        <section className='fc-line-items fc-content-box'>
          <header>
            <div className='fc-grid'>
              <div className='fc-col-md-2-3'>Items</div>
              <div className='fc-col-md-1-3 fc-align-right'>
                <button className='fc-btn' onClick={this.editLineItems.bind(this)}>
                  <i className='icon-edit'></i>
                </button>
              </div>
            </div>
          </header>
          <TableView 
            columns={this.props.viewColumns} 
            rows={this.props.entity.lineItems.skus}
            model='lineItem' />
        </section>
      );
    }
  }
}
'use strict';

import React from 'react';
import LineItems from '../line-items';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as bindActionCreators from '../../actions/order-line-items';

function mapStateToProps(state) {
  return {
    lineItems: state.orderLineItems || {}
  };
}

function mapDispatchToProps(dispatch) {
  return { actions: bindActionCreators(actionCreators, dispatch) };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class OrderLineItems extends LineItems {
  constructor(props, context) {
    super(props, context);
  }

  getDefaultProps() {
    return {
      isEditing: false,
      viewColumns: [
        {field: 'imagePath', text: 'Image', type: 'image'},
        {field: 'name', text: 'Name'},
        {field: 'sku', text: 'SKU'},
        {field: 'price', text: 'Price', type: 'currency'},
        {field: 'quantity', text: 'Qty'},
        {field: 'total', text: 'Total', type: 'currency'}
      ],
      editColumns: [
        {field: 'imagePath', text: 'Image', type: 'image'},
        {field: 'name', text: 'Name'},
        {field: 'sku', text: 'SKU'},
        {field: 'price', text: 'Price', type: 'currency'},
        {field: 'lineItem', text: 'Qty', component: 'LineItemCounter'},
        {field: 'total', text: 'Total', type: 'currency'},
        {field: 'delete', text: 'Delete', component: 'DeleteLineItem'}
      ]
    };
  }
}
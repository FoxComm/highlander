'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import Api from '../../lib/api';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import ShippingMethodItem from './shipping-method-item';
import ShippingMethods from '../../stores/shipping-methods';
import Panel from '../panel/panel';

export default class OrderShippingMethod extends React.Component {

  static defaultProps = {
    tableColumns: [
      {field: null, text: 'Method', component: 'ShippingMethodItem'},
      {field: 'defaultPrice', text: 'Price', type: 'currency'}
    ]
  }

  static propTypes = {
    order: PropTypes.object,
    tableColumns: PropTypes.array
  }

  constructor(props, context) {
    super(props, context);
    this.state = {
      methods: [
        {isActive: true, id: 1, storefrontDisplayName: 'By pigeons', defaultPrice: 2},
        {isActive: false, id: 2, storefrontDisplayName: 'By mail', defaultPrice: 5},
        {isActive: false, id: 3, storefrontDisplayName: 'By air mail', defaultPrice: 15}
      ],
      isEditing: false
    };
  }

  componentDidMount() {
    ShippingMethods.listenToEvent('change', this);
    ShippingMethods.fetch(this.props.order.referenceNumber);
  }

  componentWillUnmount() {
    ShippingMethods.stopListeningToEvent('change', this);
  }

  onChangeShippingMethods(methods) {
    this.setState({methods});
  }

  toggleEdit() {
    this.setState({
      isEditing: !this.state.isEditing
    });
  }

  render() {
    let methods = this.state.isEditing ? this.state.methods : _.filter(this.state.methods, {isActive: true});
    let actions = null;
    let footer = null;

    if (this.state.isEditing) {
      footer = (
        <footer className="fc-line-items-footer">
          <div>
            <button className="fc-btn fc-btn-primary"
                    onClick={ this.toggleEdit.bind(this) } >Done</button>
          </div>
        </footer>
      );
    } else {
      actions = (
        <div>
          <button className="fc-btn icon-edit fc-right" onClick={this.toggleEdit.bind(this)}>
          </button>
        </div>
      );
    }

    return (
      <Panel className="fc-order-shipping-method"
             title="Shipping Method"
             controls={ actions }>
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns} />
          <TableBody columns={this.props.tableColumns} rows={methods} model='shipping-method'>
            <ShippingMethodItem isEditing={this.state.isEditing} />
          </TableBody>
        </table>
        { footer }
      </Panel>
    );
  }
}

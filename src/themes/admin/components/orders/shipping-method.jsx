'use strict';

import _ from 'lodash';
import React from 'react';
import Api from '../../lib/api';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import ShippingMethodItem from './shipping-method-item';

export default class OrderShippingMethod extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      methods: []
    };
  }

  componentDidMount() {
    Api.get('/shipping-methods')
       .then((methods) => {
         this.setState({
           methods: methods
         });
       })
       .catch((err) => { console.error(err); });
  }

  render() {
    let methods = this.props.isEditing ? this.state.methods : _.filter(this.state.methods, {isActive: true});

    return (
      <section className="fc-contentBox" id="order-shipping-method">
        <header>Shipping Method</header>
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns} />
          <TableBody columns={this.props.tableColumns} rows={methods} model='shipping-method'>
            <ShippingMethodItem isEditing={this.props.isEditing} />
          </TableBody>
        </table>
      </section>
    );
  }
}

OrderShippingMethod.propTypes = {
  order: React.PropTypes.object,
  isEditing: React.PropTypes.bool,
  tableColumns: React.PropTypes.array
};

OrderShippingMethod.defaultProps = {
  tableColumns: [
    {field: null, text: 'Method', component: 'ShippingMethodItem'},
    {field: 'price', text: 'Price', type: 'currency'}
  ]
};

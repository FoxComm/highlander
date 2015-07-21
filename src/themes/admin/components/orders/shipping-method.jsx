'use strict';

import React from 'react';
import Api from '../../lib/api';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import ShippingMethodActive from './shipping-method-active';

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
       .catch((err) => { console.log(err); });
  }

  render() {
    return (
      <section id="order-shipping-method">
        <header>Shipping Method</header>
        <table className="inline">
          <TableHead columns={this.props.tableColumns} />
          <TableBody columns={this.props.tableColumns} rows={this.state.methods} model='shipping-method'>
            <ShippingMethodActive />
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
    {field: 'methodActive', text: 'Active', component: 'ShippingMethodActive'},
    {field: 'name', text: 'Name'},
    {field: 'price', text: 'Price', type: 'currency'}
  ]
};

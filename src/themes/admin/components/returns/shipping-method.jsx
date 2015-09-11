'use strict';

import _ from 'lodash';
import React from 'react';
import Api from '../../lib/api';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import ShippingMethodItem from './shipping-method-item';

export default class ReturnShippingMethod extends React.Component {
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
      <section className="fc-content-box" id="return-shipping-method">
        <header className="header">Shipping Method</header>
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

ReturnShippingMethod.propTypes = {
  return: React.PropTypes.object,
  isEditing: React.PropTypes.bool,
  tableColumns: React.PropTypes.array
};

ReturnShippingMethod.defaultProps = {
  tableColumns: [
    {field: null, text: 'Method', component: 'ShippingMethodItem'},
    {field: 'price', text: 'Price', type: 'currency'}
  ]
};

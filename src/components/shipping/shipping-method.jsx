'use strict';

import _ from 'lodash';
import React from 'react';
import Api from '../../lib/api';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import ShippingMethodItem from './shipping-method-item';
import ContentBox from '../content-box/content-box';

export default class ShippingMethod extends React.Component {
  constructor(props, context) {
    super(props, context);
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
      <ContentBox title="Shipping Method">
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns} />
          <TableBody columns={this.props.tableColumns} rows={methods} model='shipping-method'>
            <ShippingMethodItem isEditing={this.props.isEditing} />
          </TableBody>
        </table>
      </ContentBox>
    );
  }
}

ShippingMethod.propTypes = {
  isEditing: React.PropTypes.bool,
  tableColumns: React.PropTypes.array
};

ShippingMethod.defaultProps = {
  tableColumns: [
    {field: null, text: 'Method', component: 'ShippingMethodItem'},
    {field: 'price', text: 'Price', type: 'currency'}
  ]
};

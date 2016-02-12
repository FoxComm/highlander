import React, { Component, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import * as AddressActions from '../../../modules/customers/addresses';

import CreditCardForm from '../../credit-cards/card-form';

function mapStateToProps(state, props) {
  return { addresses: state.customers.addresses[props.customerId] };
}

function mapDispatchToProps(dispatch, props) {
  return _.transform(AddressActions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(props.customerId, ...args));
    };
  });
}

@connect(mapStateToProps, mapDispatchToProps)
export default class OrderCreditCardForm extends Component {
  static propTypes = {
    card: PropTypes.object.isRequired,
    customerId: PropTypes.number.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  componentDidMount() {
    this.props.fetchAddresses();
  }

  render() {
    const props = this.props;

    return (
      <div className="fc-order-credit-card-form">
        <CreditCardForm card={props.card}
                        customerId={props.customerId}
                        addresses={props.addresses.addresses}
                        form={props.card}
                        isDefaultEnabled={false}
                        showFormControls={false}
                        isNew={true}
                        onChange={props.onChange} />
      </div>
    );
  }
}


import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';

import CreditCardBox from '../../credit-cards/card-box';
import TileSelector from '../../tile-selector/tile-selector';
import TableCell from '../../table/cell';
import TableRow from '../../table/row';

import * as CreditCardActions from '../../../modules/customers/credit-cards';

function mapStateToProps(state, props) {
  return { creditCards: state.customers.creditCards[props.customerId] };
}

function mapDispatchToProps(dispatch, props) {
  return _.transform(CreditCardActions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(props.customerId, ...args));
    };
  });
}

@connect(mapStateToProps, mapDispatchToProps)
class NewPayment extends Component {
  static propTypes = {
    creditCards: PropTypes.object,
    customerId: PropTypes.number.isRequired,
    fetchCreditCards: PropTypes.func.isRequired,
  };

  static defaultProps = {
    creditCards: {},
  };

  componentDidMount() {
    this.props.fetchCreditCards();
  }

  get creditCards() {
    return _.map(_.get(this.props, 'creditCards.cards', []), card => {
      return (
        <CreditCardBox
          card={card}
          customerId={this.props.customerId}
          onChooseClick={() => console.log('choose')} />
      );
    });
  }

  render() {
    return (
      <TableRow>
        <TableCell colspan={3}>
          <TileSelector items={this.creditCards} />
        </TableCell>
      </TableRow>
    );
  }
};

export default NewPayment;

import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

import ContentBox from '../content-box/content-box';
import { Button } from 'components/core/button';

import Api from 'lib/api';
import Alert from '../alerts/alert';
import ErrorAlerts from '../alerts/error-alerts';

function requestSuggester(customerId, phoneNumber) {
  return Api.post(
    `/public/suggest/customer?channel=1`,
    { customerId, phoneNumber }
  );
}

// TODO: We need to actually handle country code instead of assuming USA.
function prependCountryCode(phoneNumber) {
  return phoneNumber && phoneNumber.length == 10 ? '1'+phoneNumber : phoneNumber;
}

@connect((state, props) => ({
  addresses: _.get(state.customers.addresses, [props.customer.id, 'addresses'], []),
  cards: _.get(state.customers.creditCards, [props.customer.id, 'cards'], []),
}))
export default class CustomerSuggestProducts extends React.Component {

  static propTypes = {
    customer: PropTypes.object
  };

  state = {
    msgSent: false,
    error: null
  };

  onSend = () => {
    let { id, phoneNumber } = this.props.customer;
    requestSuggester(id.toString(), prependCountryCode(phoneNumber))
      .then((resp) => this.setState({ msgSent: true }))
      .catch((err) => this.setState({ error: err.response.text }));
  }

  isEnabled() {
    const { addresses, cards } = this.props;
    const isDefaultAddress = _.find(addresses, address => address.isDefault);
    const isDefaultCard = _.find(cards, card => card.isDefault);
    return isDefaultAddress && isDefaultCard;
  }

  buttonOrNot() {
    if(this.state.msgSent) {
      return (
        <Alert type='success'>
          Success! Your message has been sent.
        </Alert>
      );
    }
    if(this.state.error) {
      return <ErrorAlerts error={this.state.error} />;
    }
    return (
      <Button id="customer-suggest-products-btn" onClick={this.onSend} disabled={!this.isEnabled()}>
        Send Suggestion
      </Button>
    );
  }

  render() {
    return (
      <ContentBox title="Suggest Products" className="fc-suggest-products">
        {this.buttonOrNot()}
      </ContentBox>
    );
  }
}

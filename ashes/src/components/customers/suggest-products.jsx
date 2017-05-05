import React from 'react';
import PropTypes from 'prop-types';

import ContentBox from '../content-box/content-box';
import { Button } from 'components/core/button';

import Api from 'lib/api';

function requestSuggester(customerId, customerPhoneNumber) {
  return Api.get(`/public/suggest/customer/${customerId}?channel=1&phone=+1${customerPhoneNumber}`);
}

export default class CustomerSuggestProducts extends React.Component {

  static propTypes = {
    customer: PropTypes.object
  };

  state = {
    msgSent: false,
    error: null
  };

  onSend = () => {
    requestSuggester(this.props.customer.id, this.props.customer.phoneNumber)
      .then((resp) => this.setState({ msgSent: true }))
      .catch((err) => this.setState({ error: err.response.text }));
  };

  buttonOrNot() {
    if (this.state.msgSent) {
      return <p>Message Sent!</p>;
    }
    if (this.state.error) {
      return <p>{this.state.error}</p>;
    }
    return (
      <Button id="customer-suggest-products-btn" onClick={this.onSend}>Send Suggestion</Button>
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

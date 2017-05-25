import React from 'react';
import PropTypes from 'prop-types';

import ContentBox from '../content-box/content-box';
import { Button } from 'components/core/button';

import Api from 'lib/api';
import Alert from 'components/core/alert';
import { ApiErrors } from 'components/utils/errors';

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
      .then(resp => this.setState({ msgSent: true }))
      .catch(error => this.setState({ error }));
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
      return <ApiErrors response={this.state.error} />;
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

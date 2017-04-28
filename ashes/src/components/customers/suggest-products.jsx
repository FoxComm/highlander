import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
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
  if(phoneNumber) {
    return phoneNumber.length == 10 ? '1'+phoneNumber : phoneNumber;
  }
  return phoneNumber;
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
      .then((resp) => this.setState({msgSent: true}))
      .catch((err) => this.setState({error: err.response.text}));
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
        <button
          id="customer-suggest-products-btn"
          className="fc-btn fc-btn-suggest-products"
          onClick={this.onSend}
        >Send Suggestion</button>
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

import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import Api from 'lib/api';

function requestSuggester(customerId, phoneNumber) {
  return Api.post(
    `/public/suggest/customer?channel=1`,
    { customerId, phoneNumber }
  );
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
      .then((resp) => this.setState({msgSent: true}))
      .catch((err) => this.setState({error: err.response.text}));
  }

  buttonOrNot() {
    if(this.state.msgSent) {
      return <p>Message Sent!</p>;
    }
    if(this.state.error) {
      return <p>{this.state.error}</p>;
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

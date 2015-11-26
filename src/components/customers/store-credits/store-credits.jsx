import React, { PropTypes } from 'react';
import Summary from './summary';
import { connect } from 'react-redux';
import * as StoreCreditsActions from '../../../modules/customers/store-credits';

@connect((state, props) => ({
  ...state.customers.storeCredits[props.params.customerId]
}), StoreCreditsActions)
export default class StoreCredits extends React.Component {

  componentDidMount() {
    const customerId = this.props.params.customerId;
    this.props.fetchStoreCredits(customerId);
  }

  render() {
    const props = this.props;
    return (
      <div className="fc-store-credits fc-list-page">
        <Summary {...props} />
        <div className="fc-grid fc-list-page-content">
          <div className="fc-col-md-1-1">

          </div>
        </div>
      </div>
    );
  }
}

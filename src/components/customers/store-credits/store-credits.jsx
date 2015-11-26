import React, { PropTypes } from 'react';
import Summary from './summary';

export default class StoreCredits extends React.Component {

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

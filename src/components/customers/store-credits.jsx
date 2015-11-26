import React, { PropTypes } from 'react';
import SectionTitle from '../section-title/section-title';

export default class StoreCredits extends React.Component {

  onAddCustomerClick() {
    console.log("onAddClick");
  }

  render() {
    return (
      <div className="fc-store-credits">
        <SectionTitle title="Store Credit"
                      addTitle="Store Credit"
                      onAddClick={ this.onAddCustomerClick }
                      isPrimary={false}/>
      </div>
    );
  }
}

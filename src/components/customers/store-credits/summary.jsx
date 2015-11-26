import React, { PropTypes } from 'react';
import SectionTitle from '../../section-title/section-title';
import TabListView from '../../tabs/tabs';
import TabView from '../../tabs/tab';
import { Link } from '../../link';

const Summary = props => {
  return (
    <div className="fc-list-page-header">
      <SectionTitle title="Store Credit"
                    addTitle="Store Credit"
                    onAddClick={ () => console.log("onAddClick") }
                    isPrimary={false} />

      <div className="fc-store-credits-summary">
        Total Points here
      </div>
      <TabListView>
        <TabView draggable={false}>
          <Link to="customer-storecredits" params={props.params}>Store Credits</Link>
        </TabView>
        <TabView draggable={false}>
          <Link to="customer-storecredit-transactions" params={props.params}>Transaction</Link>
        </TabView>
      </TabListView>
    </div>
  );
};

export default Summary;

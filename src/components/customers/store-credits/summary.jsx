import React, { PropTypes } from 'react';
import SectionTitle from '../../section-title/section-title';
import TabListView from '../../tabs/tabs';
import TabView from '../../tabs/tab';
import Currency from '../../common/currency';
import { PanelList, PanelListItem } from '../../panel/panel-list';
import { Link } from '../../link';

const Summary = props => {
  return (
    <div className="fc-list-page-header">
      <SectionTitle title="Store Credit"
                    addTitle="Store Credit"
                    onAddClick={ () => console.log("onAddClick") }
                    isPrimary={false} />

      <div className="fc-grid fc-grid-gutter fc-store-credits-summary">
        <div className="fc-col-md-1-4">
          <PanelList>
            <PanelListItem title="Total Availabale Balance">
              <div className="fc-store-credits-summary-balance">
                <Currency value="106" /> {/* ToDo: Replace with real total */}
              </div>
            </PanelListItem>
          </PanelList>
        </div>
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

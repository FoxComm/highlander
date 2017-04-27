// @flow

// libs
import _ from 'lodash';
import React, { Element } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { SectionTitle } from '../../section-title';
import TabListView from '../../tabs/tabs';
import TabView from '../../tabs/tab';
import Currency from '../../common/currency';
import { PanelList, PanelListItem } from '../../panel/panel-list';
import { Link, IndexLink } from '../../link';

type Props = {
  totals: Object,
  params: Object,
  transactionsSelected: ?boolean,
  children?: Element<*>,
}

const Summary = (props: Props) => {
  const params = {
    ...props,
    customerId: props.params.customerId
  };
  const availableBalance = _.get(props.totals, 'availableBalance', 0);

  return (
    <div className="fc-list-page-header">
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          <SectionTitle title="Store Credit"
                        addTitle="Store Credit"
                        onAddClick={ () => transitionTo('customer-storecredits-new', params) } />
        </div>
      </div>
      <div className="fc-grid fc-store-credits-summary">
        <div className="fc-col-md-1-4">
          <PanelList>
            <PanelListItem title="Total Available Balance">
              <div className="fc-store-credits-summary-balance">
                <Currency value={availableBalance} />
              </div>
            </PanelListItem>
          </PanelList>
        </div>
      </div>
      {props.children}
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          <TabListView>
            <TabView draggable={false} selected={!props.transactionsSelected}>
              <IndexLink to="customer-storecredits"
                         params={props.params}
                         className="fc-store-credit-summary__tab-link">
                Store Credits
              </IndexLink>
            </TabView>
            <TabView draggable={false} selected={props.transactionsSelected}>
              <Link to="customer-storecredit-transactions"
                    params={props.params}
                    className="fc-store-credit-summary__tab-link">
                Transaction
              </Link>
            </TabView>
          </TabListView>
        </div>
      </div>
    </div>
  );
};

Summary.defaultProps = {
  transactionsSelected: false
};

export default Summary;

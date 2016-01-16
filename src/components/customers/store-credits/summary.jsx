
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { transitionTo } from '../../../route-helpers';

// components
import SectionTitle from '../../section-title/section-title';
import TabListView from '../../tabs/tabs';
import TabView from '../../tabs/tab';
import Currency from '../../common/currency';
import { PanelList, PanelListItem } from '../../panel/panel-list';
import { Link } from '../../link';

const Summary = props => {
  const params = {
    ...props,
    customerId: props.params.customerId
  };
  const availableBalance = _.get(props.totals, 'availableBalance');

  console.log('Props in Summary');
  console.log(props);

  return (
    <div className="fc-list-page-header">
      <SectionTitle title="Store Credit"
                    addTitle="Store Credit"
                    onAddClick={ () => transitionTo(props.history, 'customer-storecredits-new', params) }
                    isPrimary={false} />

      <div className="fc-grid fc-grid-gutter fc-store-credits-summary">
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
      <TabListView>
        <TabView draggable={false} selected={!props.transactionsSelected} >
          <Link to="customer-storecredits"
                params={props.params}
                className="fc-store-credit-summary__tab-link">
            Store Credits
          </Link>
        </TabView>
        <TabView draggable={false} selected={props.transactionsSelected} >
          <Link to="customer-storecredit-transactions"
                params={props.params}
                className="fc-store-credit-summary__tab-link">
            Transaction
          </Link>
        </TabView>
      </TabListView>
    </div>
  );
};

Summary.propTypes = {
  totals: PropTypes.object.isRequired,
  params: PropTypes.object.isRequired,
  history: PropTypes.object.isRequired,
  transactionsSelected: PropTypes.bool
};

Summary.defaultProps = {
  transactionsSelected: false
};

export default Summary;

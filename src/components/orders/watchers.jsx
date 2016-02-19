// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// data
import { actions } from '../../modules/orders/watchers';
import { groups, entityForms } from '../../paragons/watcher';

// components
import Watchers from '../watchers/watchers';

//helpers
import { getSingularForm } from '../../lib/text-utils';


function getGroupData(group, watchers, order) {
  return {
    entries: order[group].map(user => user[getSingularForm(entityForms[group])]),
    listModalDisplayed: _.get(watchers, [order.referenceNumber, group, 'listModalDisplayed'], false),
  };
}

function mapStateToProps({orders: {watchers}}, {order}) {
  return {
    data: {
      assignees: getGroupData(groups.assignees, watchers, order),
      watchers: getGroupData(groups.watchers, watchers, order),
    }
  };
}

function OrderWatchers({data, order}) {
  return (
    <Watchers entity={{
                  entityType: 'orders',
                  entityId: order.referenceNumber,
                }}
              data={data} />
  );
};

OrderWatchers.propTypes = {
  order: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
};

export default connect(mapStateToProps)(OrderWatchers);

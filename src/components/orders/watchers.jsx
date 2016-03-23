// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { singularize } from 'fleck';

// data
import { actions } from '../../modules/orders/watchers';
import { groups } from '../../paragons/watcher';

// components
import Watchers from '../watchers/watchers';


const getGroupData = (group, watchers, order) => {
  const entityForm = singularize(group);
  const orderGroup = _.get(order, 'group', []);

  return {
    entries: orderGroup.map(user => user[entityForm]),
    listModalDisplayed: _.get(watchers, [order.referenceNumber, group, 'listModalDisplayed'], false),
  };
};

const mapStateToProps = ({orders: {watchers}}, {order}) => {
  return {
    data: {
      assignees: getGroupData(groups.assignees, watchers, order),
      watchers: getGroupData(groups.watchers, watchers, order),
    }
  };
};

const OrderWatchers = ({data, order}) => {
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

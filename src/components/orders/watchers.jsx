// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { singularize } from 'fleck';

// data
import { groups } from '../../paragons/watcher';

// components
import Watchers from '../watchers/watchers';

const getGroupData = (group, watchers, order) => ({
  entries: _.get(watchers, [order.referenceNumber, group, 'entries'], []),
  listModalDisplayed: _.get(watchers, [order.referenceNumber, group, 'listModalDisplayed'], false),
});

const mapStateToProps = ({ orders: { watchers } }, { order }) => ({
  data: {
    assignees: getGroupData(groups.assignees, watchers, order),
    watchers: getGroupData(groups.watchers, watchers, order),
  }
});

const OrderWatchers = ({ data, order }) => {
  const entity = {
    entityType: 'orders',
    entityId: order.referenceNumber,
  };

  return (
    <Watchers entity={entity} data={data} />
  );
};

OrderWatchers.propTypes = {
  order: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
};

export default connect(mapStateToProps)(OrderWatchers);


import _ from 'lodash';
import { get, post } from '../lib/search';
import moment from 'moment';
import * as dsl from './dsl';

const sortBy = [
  dsl.sortByField('createdAt', 'desc'),
  dsl.sortByField('activity.createdAt', 'desc'),
];

function buildRequest({fromId = null, untilDate = null, query = null, dimension = 'admin', objectId = null} = {}) {
  const filter = [
    dsl.termFilter('dimension', dimension)
  ];
  if (objectId != null) {
    filter.push(dsl.termFilter('objectId', objectId));
  }
  if (fromId != null) {
    filter.push(dsl.rangeFilter('id', {lt: fromId}));
  }
  if (untilDate) {
    filter.push(dsl.rangeFilter('createdAt', {gt: untilDate}));
  }

  return dsl.query({
    bool: {filter}
  });
}

export function fetch(queryParams, forCount = false) {
  let q = buildRequest(queryParams);
  if (!forCount) {
    q.sort = sortBy;
  } else {
    q.size = 0;
  }

  return post(`activity_connections/_search`, q);
}

export default function searchActivities(fromActivity = null, trailParams, days = 2, query = null) {

  function queryFirstActivity() {
    return {
      ...buildRequest(trailParams),
      sort: sortBy,
      size: 1,
    };
  }

  let promise,
    hasMore;

  if (fromActivity == null) {
    const now = moment.utc();

    promise = post('activity_connections/_search', queryFirstActivity())
      .then(response => {
        const result = response.result;
        if (result.length) {
          const firstActivityDate = moment.utc(result[0].createdAt);

          // if we have activities in last 2 days - fetch activities for last 2 days
          // if not - fetch activities for last 2 days from latest activity

          const markerDate = now.diff(firstActivityDate, 'days', true) > days ? firstActivityDate : now;
          const untilDate = markerDate.endOf('day').subtract(days, 'days');

          return fetch({...trailParams, untilDate, query});
        }

        return response;
      });
  } else {
    const untilDate = moment.utc(fromActivity.createdAt).startOf('day').subtract(days, 'days');
    promise = fetch({...trailParams, fromId: fromActivity.id, untilDate, query});
  }

  let response;

  return promise
    .then(_response => {
      response = _response;
      const result = response.result;

      if (result.length == 0) {
        hasMore = false;
      } else {
        const fromId = _.get(_.last(result), 'id');
        return fetch({...trailParams, fromId, query}, '_count')
          .then(response => hasMore = response.count > 0);
      }
    })
    .then(() => {
      response.hasMore = hasMore;
      return response;
    });
}

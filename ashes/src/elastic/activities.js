import _ from 'lodash';
import Agni from 'lib/agni';
import { post } from 'lib/search';
import moment from 'moment';
import * as dsl from './dsl';

const sortBy = [
  dsl.sortByField('createdAt', 'desc'),
  dsl.sortByField('activity.createdAt', 'desc'),
];

function buildRequest({ fromDate = null, untilDate = null, query = null, dimension = 'admin', objectId = null } = {}) {
  const filter = [
    dsl.termFilter('dimension', dimension)
  ];
  if (objectId != null) {
    filter.push(dsl.termFilter('objectId', objectId));
  }
  if (fromDate) {
    filter.push(dsl.rangeFilter('createdAt', { gt: fromDate }));
  }
  if (untilDate) {
    filter.push(dsl.rangeFilter('createdAt', { lt: untilDate }));
  }

  return dsl.query({
    bool: { filter }
  });
}

export function fetch(queryParams, forCount = false) {
  let query = buildRequest(queryParams);
  let verb = '_search';

  if (!forCount) {
    query.sort = sortBy;
  } else {
    verb = '_count';
  }

  return post(`scoped_activity_trails/${verb}`, query);
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

    const q = queryFirstActivity();

    promise = Agni.post('scoped_activity_trails', q)
      .then(response => {
        const result = _.isEmpty(response.result) ? [] : response.result;
        _.set(response, 'result', result);

        if (result.length) {
          const firstActivityDate = moment.utc(result[0].createdAt);

          // if we have activities in last 2 days - fetch activities for last 2 days
          // if not - fetch activities for last 2 days from latest activity

          const markerDate = now.diff(firstActivityDate, 'days', true) > days ? firstActivityDate : now;
          const fromDate = markerDate.endOf('day').subtract(days, 'days');

          return fetch({ ...trailParams, fromDate, query });
        }

        return response;
      });
  } else {
    const untilDate = moment.utc(fromActivity.createdAt).startOf('day').subtract(days, 'days');
    promise = fetch({ ...trailParams, fromDate: fromActivity.createdAt, untilDate, query });
  }

  let response;

  return promise
    .then(_response => {
      response = _response;
      const result = response.result;

      if (result.length == 0) {
        hasMore = false;
      } else {
        const untilDate = _.get(_.last(result), 'createdAt');
        return fetch({ ...trailParams, untilDate, query }, true)
          .then(response => hasMore = response.count > 0);
      }
    })
    .then(() => {
      response.hasMore = hasMore;
      return response;
    });
}

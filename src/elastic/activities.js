
import _ from 'lodash';
import { get, post } from '../lib/search';
import ejs from 'elastic.js';
import moment from 'moment';

const sortBy = [
  ejs.Sort('createdAt').desc(),
  ejs.Sort('activity.createdAt').desc()
];

export function fetch({fromId = null, untilDate = null, query = null} = {}, type = '_search') {

  function buildFilter(filter) {
    filter = filter.must(ejs.TermQuery('dimension', 'admin'));
    if (fromId != null) {
      filter = filter.must(ejs.RangeQuery('id').lt(fromId));
    }
    if (untilDate) {
      filter = filter.must(ejs.RangeQuery('createdAt').gt(untilDate));
    }
    return filter;
  }

  const filter = buildFilter(ejs.BoolQuery());

  let q = ejs.Request().query(filter);
  if (type == '_search') {
    q = q.sort(sortBy);
  }

  return post(`activity_connections/${type}`, q);
}

export default function searchActivities(fromActivity = null, days = 2, query = null) {

  function queryFirstActivity() {
    return ejs.Request()
      .query(
        ejs.TermQuery('dimension', 'admin')
      )
      .sort(sortBy)
      .size(1);
  }

  let promise,
    hasMore;

  if (fromActivity == null) {
    const now = moment();

    promise = post('activity_connections/_search', queryFirstActivity())
      .then(response => {
        const result = response.result;
        if (result.length) {
          const firstActivityDate = moment(result[0].createdAt);

          if (now.diff(firstActivityDate, 'days', true) > days) {
            const untilDate = firstActivityDate.endOf('day').subtract(days, 'days');

            return fetch({untilDate});
          }
        }

        return response;
      });
  } else {
    const untilDate = moment(fromActivity.createdAt).startOf('day').subtract(days, 'days');
    promise = fetch({fromId: fromActivity.id, untilDate, query});
  }

  let response;

  return promise
    .then(_response => {
      response = _response;
      const result = response.result;

      if (result.length == 0) {
        hasMore = false;
      } else {
        return fetch({fromId: _.last(result).id, query}, '_count')
          .then(response => hasMore = response.count > 0);
      }
    })
    .then(() => {
      response.hasMore = hasMore;
      return response;
    });
}

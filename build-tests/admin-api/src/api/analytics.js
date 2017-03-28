// @class Analytics
// Accessible via [analytics](#foxapi-analytics) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';
import querystring from 'querystring';

function randomSalt(maxRand = 100000) {
  return Math.floor(Math.random() * maxRand);
}

// Add a count argument
function sphexTrackerUrl(tracker, saltFunction = randomSalt) {
  const { channel, subject, verb, obj, objId, count = 1 } = tracker;

  const queryParams = {
    ch: channel,
    sub: subject,
    v: verb,
    ob: obj,
    id: objId,
    c: count,
    slt: saltFunction(),
  };
  const query = querystring.stringify(queryParams);

  return `${endpoints.hal}?${query}`;
}

export default class Analytics {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method trackEvent(tracker: Object<SphexTracker>, saltFunction?: Function): Promise
   * Enables the tracking of UI specific events. The optional salt function must
   * be a random number generator.
   */
  trackEvent(tracker, saltFunction = randomSalt) {
    return this.api.get(sphexTrackerUrl(tracker, saltFunction));
  }
}

// @miniclass SphexTracker (Analytics)
// @field channel: Number
// Unique Channel ID number
//
// @field subject: Number
// Unique Subject ID number
//
// @field obj: String
// Name of the object type you are tracking the event for
//
// @field verb: String
// A single word that describes the event
//
// @field objId: Number
// Unique number that can trace the object being tracked
//
// @field count: Number
// Quantity
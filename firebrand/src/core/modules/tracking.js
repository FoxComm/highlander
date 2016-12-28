/* @flow weak */

// libraries
import querystring from 'querystring';

// types
export type SphexTracker = {
  url: string;
  channel: number;
  subject: number;
  obj: string;
  verb: string;
  objId: number;
}

// helpers
export function randomSalt(maxRand: number = 100000): number {
  return Math.floor(Math.random() * maxRand);
}

export function trackRequest(tracker: SphexTracker, saltFunction: Function = randomSalt): ?Promise {
  return fetch(sphexTrackerUrl(tracker, saltFunction));
}

export function sphexTrackerUrl(tracker: SphexTracker, saltFunction: Function = randomSalt): string {
  const { url, channel, subject, obj, verb, objId } = tracker;
  const queryParams = {
    ch: channel,
    sub: subject,
    v: verb,
    ob: obj,
    id: objId,
    salt: saltFunction(),
  };
  const query = querystring.stringify(queryParams);

  return `${url}?${query}`;
}

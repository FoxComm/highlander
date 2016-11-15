
export function trackPageView(page, fieldsObject) {
  ga('send', 'pageview', page, fieldsObject);
}

/**
 * See: https://developers.google.com/analytics/devguides/collection/analyticsjs/events
 * @method trackEvent(eventCategory, eventAction, eventLabel, ...)
 */
export function trackEvent(...args) {
  ga('send', 'event', ...args);
}

export function setUserId(userId) {
  if (userId != null) {
    try {
      ga('set', 'userId', userId);
    } catch (ex) {
      // ignore errors here
    }
  }
}

export function initTracker(userId) {
  ga('require', 'ec');
  setUserId(userId);
}

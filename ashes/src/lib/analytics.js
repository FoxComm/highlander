
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

export function initTracker() {
  const userInfoStr = localStorage.getItem('user');
  if (userInfoStr) {
    try {
      const userData = JSON.parse(userInfoStr);
      ga('set', 'userId', userData.id);
    } catch (ex) {
      // pass
    }
  }
}


export function trackPageView(page, fieldsObject) {
  ga('send', 'pageview', page, fieldsObject);
}

export function trackEvent(eventCategory, eventAction, eventLabel, eventValue, fieldsObject) {
  ga('send', 'event', eventCategory, eventAction, eventLabel, eventValue, fieldsObject);
}

export function initTracker() {
  const userInfoStr = localStorage.getItem('user');
  if (userInfoStr) {
    try {
      const userData = JSON.parse(userInfoStr);
      ga('set', 'userId', userData.id);
    } catch (ex) {}
  }
}

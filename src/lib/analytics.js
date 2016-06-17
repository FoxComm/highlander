
export function trackPageView(page, fieldsObject) {
  ga('send', 'pageview', page, fieldsObject);
}

export function trackEvent(eventCategory, eventAction, eventLabel, eventValue, fieldsObject) {
  ga('send', 'event', eventCategory, eventAction, eventLabel, eventValue, fieldsObject);
}

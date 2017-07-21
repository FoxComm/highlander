/**
 * Detects element is viewport or not.
 * Use this method if you need to write some logic related to scrolling, for example.
 * @param el
 * @returns {boolean}
 */
export function isElementInViewport(el) {
  const rect = el.getBoundingClientRect();

  return (
    rect.top >= 0 &&
    rect.left >= 0 &&
    rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
    rect.right <= (window.innerWidth || document.documentElement.clientWidth)
  );
}

/**
 * Detects element is overlapped by over elements or not.
 * @param element
 * @param ancestors
 * @returns {boolean}
 */
export function isElementVisible(element, ...ancestors) {
  const elements = [element, ...ancestors];
  const {left, right, top, bottom} = element.getBoundingClientRect();

  const corners = [
    document.elementFromPoint(left + 1, top + 1),
    document.elementFromPoint(right - 1, top + 1),
    document.elementFromPoint(right - 1, bottom - 1),
    document.elementFromPoint(left + 1, bottom - 1),
  ];

  return corners.every(corner => elements.includes(corner));
}

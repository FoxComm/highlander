// @flow

export function setPageTitle(title: string): void {
  if (typeof document == 'undefined') return;
  document.title = `FoxCommerce - ${title}`;
}

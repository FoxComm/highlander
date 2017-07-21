/* @flow */

export default function format(str: string, ...values: Array<any>): string {
  return str.replace(/%(\d+)/g, function (allMatch, indexMatch) {
    return indexMatch in values ? values[indexMatch] : allMatch;
  });
}

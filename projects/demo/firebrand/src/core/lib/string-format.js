/* @flow */

export default function format(str: string, ...values: Array<any>): string {
  return str.replace(/%(\d+)/g, function (match) {
    const position = Number(match);

    return position && position in values ? values[position] : match;
  });
}

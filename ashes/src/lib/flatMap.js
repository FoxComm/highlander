export default function flatMap(arr, map) {
  return [].concat.apply([], arr.map(map));
}

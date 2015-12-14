export function formatCode(code) {
  return code.match(/.{1,4}/g).join(' ');
}

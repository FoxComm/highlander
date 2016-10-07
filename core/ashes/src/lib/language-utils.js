import _ from 'lodash';

function codeToName(code) {
  const parts = code.split(/(?=[A-Z])/);
  const name = parts.map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(' ');
  return name;
}

export { codeToName };

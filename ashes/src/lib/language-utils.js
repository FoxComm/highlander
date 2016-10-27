import _ from 'lodash';

function codeToName(code) {
  const parts = code.split(/(?=[A-Z])/);
  return parts.map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(' ');
}

export { codeToName };

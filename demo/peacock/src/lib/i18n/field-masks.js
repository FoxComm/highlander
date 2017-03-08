import _ from 'lodash';

// Create masks related to react-text-mask component
// https://github.com/text-mask/text-mask/blob/master/componentDocumentation.md#mask

export function createNumberMask(stringPattern) {
  return _.map(stringPattern, c => c.match(/\d/) ? /\d/ : c);
}

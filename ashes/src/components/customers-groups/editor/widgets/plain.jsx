//components
import { Input, getDefault, isValid } from '../inputs/plain';
import { Label } from '../labels/plain';

export default function (type) {
  return {
    Input: Input(type),
    getDefault: getDefault(type),
    isValid: isValid(type),
    Label: Label(type),
  };
}

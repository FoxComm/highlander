//components
import { Input, getDefault, isValid } from '../inputs/one-of';
import { Label } from '../labels/one-of';

export default function(Widget) {
  return {
    Input: Input(Widget),
    getDefault: getDefault(Widget),
    isValid: isValid(Widget),
    Label: Label(Widget),
  };
}
